package com.ten.tenapigateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ten.common.model.vo.InvokeUserVO;
import com.ten.common.security.RequestSignature;
import com.ten.common.service.InnerUserInterfaceInfoService;
import com.ten.common.service.InnerUserService;
import com.ten.common.service.InnerInterfaceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Verify the exact request before consuming a nonce or quota. */
@Slf4j
@Component
public class ApiAuthInvokeFilter implements GlobalFilter, Ordered {
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerUserService innerUserService;
    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;
    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerInterfaceInfoService innerInterfaceInfoService;

    private final Cache<String, InvokeUserVO> invokeUserCache = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(2, TimeUnit.MINUTES).build();

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;
    @Autowired
    private GatewayInvokeProperties properties;
    @Value("${gateway.internal-secret:}")
    private String internalSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Client-supplied identity headers must never be trusted, even on bypass routes.
        ServerHttpRequest clean = exchange.getRequest().mutate().headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Gateway-Signature");
            headers.remove("body");
        }).build();
        ServerWebExchange cleanExchange = exchange.mutate().request(clean).build();
        String path = clean.getPath().value();
        if (!needSignatureCheck(path)) return chain.filter(cleanExchange);
        // A whitelist may bypass session routes, never an open API.
        if (properties.getWhiteList() != null) {
            for (String white : properties.getWhiteList()) {
                if (MATCHER.match(white, path)) {
                    log.warn("Ignoring open-API authentication whitelist entry");
                }
            }
        }
        if (!RequestSignature.validInternalSecret(internalSecret)) {
            return error(cleanExchange, HttpStatus.SERVICE_UNAVAILABLE, "网关内部鉴权尚未配置");
        }

        String ak = clean.getHeaders().getFirst("accessKey");
        String nonce = clean.getHeaders().getFirst("nonce");
        String timestamp = clean.getHeaders().getFirst("timestamp");
        String sign = clean.getHeaders().getFirst("sign");
        if (!RequestSignature.VERSION.equals(clean.getHeaders().getFirst("signatureVersion"))
                || !StringUtils.hasText(ak) || ak.length() > 128
                || !RequestSignature.validNonce(nonce)
                || !RequestSignature.validTimestamp(timestamp, System.currentTimeMillis() / 1000)
                || sign == null || !sign.matches("[0-9a-f]{64}")) {
            return error(cleanExchange, HttpStatus.FORBIDDEN, "鉴权参数无效，请使用 v2 签名");
        }

        Mono<byte[]> body = DataBufferUtils.join(clean.getBody(), RequestSignature.MAX_BODY_BYTES)
                .map(buffer -> {
                    try {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                }).defaultIfEmpty(new byte[0]);

        return body.onErrorResume(DataBufferLimitException.class, e ->
                        error(cleanExchange, HttpStatus.PAYLOAD_TOO_LARGE, "请求体超过 1 MiB").then(Mono.empty()))
                .flatMap(bytes -> authenticate(clean, ak, nonce, timestamp, sign, bytes)
                        // Only authentication errors are converted here; downstream failures keep their status.
                        .onErrorResume(InvokeException.class, e ->
                                error(cleanExchange, e.status, e.getMessage()).then(Mono.empty()))
                        .onErrorResume(e -> {
                            log.warn("API authentication dependency failed: {}", e.getClass().getSimpleName());
                            return error(cleanExchange, HttpStatus.SERVICE_UNAVAILABLE, "鉴权服务暂不可用")
                                    .then(Mono.empty());
                        })
                        .flatMap(user -> {
                            String userId = String.valueOf(user.getId());
                            String proof = RequestSignature.sign(clean.getMethodValue(),
                                    RequestSignature.target(clean.getURI()), userId, timestamp, nonce, bytes, internalSecret);
                            ServerHttpRequest forwarded = clean.mutate().headers(headers -> {
                                headers.set("X-User-Id", userId);
                                headers.set("X-Gateway-Signature", proof);
                            }).build();
                            ServerHttpRequestDecorator cached = new ServerHttpRequestDecorator(forwarded) {
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                                }
                            };
                            return chain.filter(cleanExchange.mutate().request(cached).build());
                        }));
    }

    private Mono<InvokeUserVO> authenticate(ServerHttpRequest request, String ak, String nonce,
                                           String timestamp, String suppliedSign, byte[] bytes) {
        return Mono.fromCallable(() -> invokeUserCache.get(ak, innerUserService::getInvokeUser))
                .subscribeOn(Schedulers.boundedElastic()).timeout(Duration.ofSeconds(2))
                .switchIfEmpty(Mono.error(new InvokeException(HttpStatus.FORBIDDEN, "鉴权失败")))
                .flatMap(user -> {
                    String expected = RequestSignature.sign(request.getMethodValue(),
                            RequestSignature.target(request.getURI()), ak, timestamp, nonce, bytes, user.getSecretKey());
                    if (!RequestSignature.matches(expected, suppliedSign)) {
                        return Mono.error(new InvokeException(HttpStatus.FORBIDDEN, "签名错误"));
                    }
                    return Mono.fromCallable(() -> innerInterfaceInfoService.resolveOnlineInterfaceId(
                                    request.getMethodValue(), request.getURI().getRawPath()))
                            .subscribeOn(Schedulers.boundedElastic()).timeout(Duration.ofSeconds(2))
                            .switchIfEmpty(Mono.error(new InvokeException(HttpStatus.NOT_FOUND, "接口不存在或已下线")))
                            .flatMap(interfaceId -> redisTemplate.opsForValue()
                            .setIfAbsent("gateway:nonce:" + ak + ":" + nonce, timestamp,
                                    Duration.ofSeconds(RequestSignature.NONCE_TTL_SECONDS))
                            .defaultIfEmpty(false)
                            .flatMap(created -> {
                                if (!created) return Mono.error(new InvokeException(HttpStatus.FORBIDDEN, "重复请求"));
                                return Mono.fromCallable(() ->
                                                innerUserInterfaceInfoService.invokeCount(user.getId(), interfaceId))
                                        .subscribeOn(Schedulers.boundedElastic()).timeout(Duration.ofSeconds(2))
                                        .flatMap(ok -> Boolean.TRUE.equals(ok) ? Mono.just(user)
                                                : Mono.error(new InvokeException(HttpStatus.FORBIDDEN, "调用额度不足")));
                            }));
                });
    }

    private boolean needSignatureCheck(String path) {
        if (path.startsWith("/api/user/") || path.startsWith("/api/interfaceInfo/")
                || path.startsWith("/api/userInterfaceInfo/") || path.startsWith("/api/post/")) return false;
        return path.startsWith("/api/");
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Messages are fixed server strings; never expose RPC/Redis exception text or request data.
        byte[] json = ("{\"code\":" + (status == HttpStatus.FORBIDDEN ? 40300 : status.value() * 100)
                + ",\"message\":\"" + message + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(json)));
    }

    private static class InvokeException extends RuntimeException {
        private final HttpStatus status;
        InvokeException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
