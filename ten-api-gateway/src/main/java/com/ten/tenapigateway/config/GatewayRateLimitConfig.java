package com.ten.tenapigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 对外 API 的限流维度：优先按 accessKey，缺失时按客户端 IP。
 */
@Configuration
public class GatewayRateLimitConfig {

    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String accessKey = exchange.getRequest().getHeaders().getFirst("accessKey");
            if (StringUtils.hasText(accessKey)) {
                return Mono.just("ak:" + accessKey);
            }
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String host = remoteAddress == null ? "unknown" : remoteAddress.getHostString();
            return Mono.just("ip:" + host);
        };
    }
}
