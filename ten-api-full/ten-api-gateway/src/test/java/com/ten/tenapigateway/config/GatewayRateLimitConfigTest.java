package com.ten.tenapigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayRateLimitConfigTest {

    private final KeyResolver keyResolver = new GatewayRateLimitConfig().apiKeyResolver();

    @Test
    void shouldPreferAccessKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/name/ga")
                        .header("accessKey", "test-ak")
                        .build());

        assertEquals("ak:test-ak", keyResolver.resolve(exchange).block());
    }

    @Test
    void shouldFallbackToClientIp() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/name/ga")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                        .build());

        assertEquals("ip:127.0.0.1", keyResolver.resolve(exchange).block());
    }
}
