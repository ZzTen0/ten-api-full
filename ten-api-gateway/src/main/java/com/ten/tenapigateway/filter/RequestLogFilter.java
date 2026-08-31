package com.ten.tenapigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 全局请求日志过滤器
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final long SLOW_REQUEST_THRESHOLD_MILLIS = 1000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        String method = exchange.getRequest().getMethodValue();
        String path = exchange.getRequest().getPath().value();
        return chain.filter(exchange).doFinally(signalType -> {
            long cost = System.currentTimeMillis() - start;
            Integer status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : -1;
            if (cost >= SLOW_REQUEST_THRESHOLD_MILLIS || status >= 500) {
                log.warn("[GATEWAY] {} {} status={} cost={}ms signal={}",
                        method, path, status, cost, signalType);
            } else {
                log.debug("[GATEWAY] {} {} status={} cost={}ms",
                        method, path, status, cost);
            }
        });
    }

    @Override
    public int getOrder() {
        // 最先执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
