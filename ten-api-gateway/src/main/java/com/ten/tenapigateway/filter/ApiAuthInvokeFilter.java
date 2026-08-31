package com.ten.tenapigateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ten.common.model.vo.InvokeUserVO;
import com.ten.common.service.InnerUserInterfaceInfoService;
import com.ten.common.service.InnerUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 接口签名鉴权 + 调用次数扣减 全局过滤器
 */
@Slf4j
@Component
public class ApiAuthInvokeFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final int MIN_NONCE_LENGTH = 16;
    private static final int MAX_NONCE_LENGTH = 64;
    private static final long RPC_TIMEOUT_MILLIS = 2000;
    private static final Pattern NONCE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    /**
     * 默认接口 id（当前测试 NameController 接口）
     * 实际生产可通过 URL -> interface_info 表匹配获取
     */
    private static final long DEFAULT_INTERFACE_ID = 1L;

    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerUserService innerUserService;

    @DubboReference(timeout = 2000, retries = 0, check = false, lazy = true)
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    private final Cache<String, InvokeUserVO> invokeUserCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private GatewayInvokeProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1) 白名单直接放行
        if (properties.getWhiteList() != null) {
            for (String white : properties.getWhiteList()) {
                if (MATCHER.match(white, path)) {
                    log.debug("白名单放行: {}", path);
                    return chain.filter(exchange);
                }
            }
        }

        // 2) 管理后台接口（登录态session鉴权）由后端自己校验，网关直接放行
        //    仅对 对外接口（/api/name/** 这类非管理路径）做 AK/SK 签名校验
        boolean needSignCheck = needSignatureCheck(path);
        if (!needSignCheck) {
            return chain.filter(exchange);
        }

        // 3) 取 header
        String accessKey = request.getHeaders().getFirst("accessKey");
        String nonce = request.getHeaders().getFirst("nonce");
        String timestamp = request.getHeaders().getFirst("timestamp");
        String sign = request.getHeaders().getFirst("sign");
        String body = request.getHeaders().getFirst("body");

        if (!StringUtils.hasText(accessKey)
                || !StringUtils.hasText(nonce)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(sign)) {
            return unauthorized(exchange, "缺少鉴权参数(accessKey/nonce/timestamp/sign)");
        }

        // 4) nonce 长度 + 时间戳超时校验
        if (accessKey.length() > 128) {
            return unauthorized(exchange, "accessKey格式错误");
        }
        if (nonce.length() < MIN_NONCE_LENGTH
                || nonce.length() > MAX_NONCE_LENGTH
                || !NONCE_PATTERN.matcher(nonce).matches()) {
            return unauthorized(exchange, "随机数长度错误");
        }
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis() / 1000;
            if (Math.abs(now - ts) > 300) {
                return unauthorized(exchange, "请求已超时");
            }
        } catch (NumberFormatException e) {
            return unauthorized(exchange, "时间戳格式错误");
        }

        // 5) nonce 防重放（Redis SETNX，5分钟）
        ReactiveValueOperations<String, String> ops = redisTemplate.opsForValue();
        String nonceKey = "gateway:nonce:" + accessKey + ":" + nonce;
        return ops.setIfAbsent(nonceKey, timestamp, Duration.ofMinutes(5))
                .defaultIfEmpty(false)
                .flatMap(absent -> {
                    if (Boolean.FALSE.equals(absent)) {
                        return Mono.error(new RuntimeException("nonce已使用或重复请求"));
                    }
                    // 6) 短时缓存调用用户，未命中时再通过 Dubbo 查询
                    return Mono.fromCallable(() -> invokeUserCache.get(
                                    accessKey, innerUserService::getInvokeUser))
                            .subscribeOn(Schedulers.boundedElastic())
                            .timeout(Duration.ofMillis(RPC_TIMEOUT_MILLIS))
                            .switchIfEmpty(Mono.error(new RuntimeException("accessKey无效")));
                })
                .cast(InvokeUserVO.class)
                .flatMap(user -> {
                    // 7) 签名校验
                    String secretKey = user.getSecretKey();
                    String serverSign = GatewaySignUtils.getSign(body == null ? "" : body, secretKey);
                    if (!serverSign.equals(sign)) {
                        return Mono.error(new RuntimeException("签名错误"));
                    }
                    // 8) 后端使用原子 SQL 完成额度判断、初始化和扣减
                    long userId = user.getId();
                    return Mono.fromCallable(() ->
                                    innerUserInterfaceInfoService.invokeCount(userId, DEFAULT_INTERFACE_ID))
                            .subscribeOn(Schedulers.boundedElastic())
                            .timeout(Duration.ofMillis(RPC_TIMEOUT_MILLIS))
                            .flatMap(ok -> {
                                if (Boolean.FALSE.equals(ok)) {
                                    return Mono.error(new RuntimeException("调用次数扣减失败/次数不足"));
                                }
                                // 9) 放行，把 userId 传到下游
                                ServerHttpRequest newReq = request.mutate()
                                        .header("X-User-Id", String.valueOf(userId))
                                        .build();
                                return chain.filter(exchange.mutate().request(newReq).build());
                            });
                })
                .onErrorResume(throwable -> {
                    log.warn("接口鉴权失败 path={}, msg={}", path, throwable.getMessage());
                    return unauthorized(exchange, throwable.getMessage());
                });
    }

    /**
     * 判断路径是否需要AK/SK签名校验
     * 策略：非管理路径（user/interfaceInfo/userInterfaceInfo/post）的 /api/** 均需校验
     */
    private boolean needSignatureCheck(String path) {
        if (path.startsWith("/api/user/")
                || path.startsWith("/api/interfaceInfo/")
                || path.startsWith("/api/userInterfaceInfo/")
                || path.startsWith("/api/post/")) {
            return false;
        }
        return path.startsWith("/api/");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"code\":40300,\"message\":\"" + escapeJson(msg) + "\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    @Override
    public int getOrder() {
        // 让路由级限流先执行，再进入签名鉴权和 Dubbo 调用
        return 10;
    }
}
