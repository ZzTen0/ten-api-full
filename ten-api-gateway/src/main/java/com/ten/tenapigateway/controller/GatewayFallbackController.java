package com.ten.tenapigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 下游接口服务不可用时的统一响应。
 */
@RestController
public class GatewayFallbackController {

    @RequestMapping("/fallback/api")
    public Mono<ResponseEntity<Map<String, Object>>> apiFallback() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 50300);
        body.put("data", null);
        body.put("message", "接口服务暂时不可用");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
