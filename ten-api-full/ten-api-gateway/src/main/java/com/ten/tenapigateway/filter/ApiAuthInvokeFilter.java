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

/**
 * 开放接口签名鉴权、接口状态校验及调用额度过滤器。
 *
 * <p>校验真实请求内容后登记 nonce，按已上线接口的实际 ID 扣减额度，
 * 并为下游接口生成绑定请求内容及用户身份的内部签名。</p>
 */
@Slf4j
@Component
public class ApiAuthInvokeFilter implements GlobalFilter, Ordered {
    /**
     * 用于检测错误配置到开放接口路径的鉴权白名单。
     */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * 通过 AK 查询调用用户的内部 RPC 服务。
     */
    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerUserService innerUserService;

    /**
     * 按用户和实际接口原子扣减额度的内部 RPC 服务。
     */
    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    /**
     * 按请求方法及路径解析已上线接口的内部 RPC 服务。
     */
    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerInterfaceInfoService innerInterfaceInfoService;

    /**
     * 用户凭据的短期本地缓存，最多 10000 项，写入后两分钟失效。
     */
    private final Cache<String, InvokeUserVO> invokeUserCache = Caffeine.newBuilder()
            .maximumSize(10000).expireAfterWrite(2, TimeUnit.MINUTES).build();

    /**
     * 响应式 Redis 操作模板，用于原子登记防重放记录。
     */
    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    /**
     * 网关鉴权配置，开放接口不会因白名单配置而绕过验签。
     */
    @Autowired
    private GatewayInvokeProperties properties;

    /**
     * 网关与接口服务共享的独立内部鉴权密钥。
     */
    @Value("${gateway.internal-secret:}")
    private String internalSecret;

    /**
     * 校验开放接口请求，并向下游透传可验证的用户身份。
     *
     * @param exchange 当前响应式请求上下文
     * @param chain 后续网关过滤器链
     * @return 鉴权失败响应或下游过滤器链的异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 所有路由先清理客户端伪造的内部身份头，管理端放行路径也不例外。
        ServerHttpRequest clean = exchange.getRequest().mutate().headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Gateway-Signature");
            headers.remove("body");
        }).build();
        ServerWebExchange cleanExchange = exchange.mutate().request(clean).build();
        String path = clean.getPath().value();
        if (!needSignatureCheck(path)) return chain.filter(cleanExchange);
        // 开放接口必须验签，白名单仅用于管理端路径，不能扩展成开放接口的绕过入口。
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

        // 读取受限请求体后释放原始缓冲区，后续用同一字节数组完成验签与转发。
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
                        // 仅在鉴权阶段转换异常，下游接口错误保留原来的响应语义。
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
                            // 请求体已被验签消费，必须为下游提供可重新读取的字节流。
                            ServerHttpRequestDecorator cached = new ServerHttpRequestDecorator(forwarded) {
                                /**
                                 * 重放已验证的请求体字节。
                                 *
                                 * @return 每次订阅独立包装的请求体缓冲区
                                 */
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                                }
                            };
                            return chain.filter(cleanExchange.mutate().request(cached).build());
                        }));
    }

    /**
     * 验证签名、接口上线状态和 nonce，并按实际接口扣减额度。
     *
     * @param request 已清理客户端伪造内部身份头的原始请求
     * @param ak 调用方访问密钥
     * @param nonce 本次请求随机串
     * @param timestamp UNIX 秒级时间戳字符串
     * @param suppliedSign 客户端提供的签名
     * @param bytes 实际请求体字节
     * @return 完成鉴权及额度扣减的调用用户；失败以响应式异常信号返回
     */
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

    /**
     * 判断路径是否属于需要 AK/SK 签名的开放接口。
     *
     * @param path 当前请求路径
     * @return 非管理路径的开放接口返回 true，管理端交由后端 Session 鉴权
     */
    private boolean needSignatureCheck(String path) {
        if (path.startsWith("/api/user/") || path.startsWith("/api/interfaceInfo/")
                || path.startsWith("/api/userInterfaceInfo/") || path.startsWith("/api/post/")) return false;
        return path.startsWith("/api/");
    }

    /**
     * 输出统一网关错误响应。
     *
     * @param exchange 当前请求上下文
     * @param status HTTP 状态码
     * @param message 服务端固定错误说明，不传入原始异常或请求文本
     * @return 写入错误响应的异步结果
     */
    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // 只输出服务端固定说明，不暴露 RPC、Redis 异常原文或用户请求数据。
        byte[] json = ("{\"code\":" + (status == HttpStatus.FORBIDDEN ? 40300 : status.value() * 100)
                + ",\"message\":\"" + message + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(json)));
    }

    /**
     * 可向调用方返回受控说明的鉴权或额度异常。
     */
    private static class InvokeException extends RuntimeException {
        /**
         * 应返回的 HTTP 状态码。
         */
        private final HttpStatus status;
        /**
         * 创建受控调用异常。
         *
         * @param status HTTP 状态码
         * @param message 面向调用方的错误说明
         */
        InvokeException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    /**
     * 指定过滤器执行顺序，使路由级限流先于签名校验和 RPC 扣减。
     *
     * @return 过滤器排序值 10
     */
    @Override
    public int getOrder() {
        return 10;
    }
}
