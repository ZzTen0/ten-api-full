package com.ten.tenapigateway;

import com.ten.tenapigateway.config.GatewayRateLimitConfig;
import com.ten.tenapigateway.controller.GatewayFallbackController;
import com.ten.tenapigateway.filter.RequestLogFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
        classes = TenApiGatewayApplicationTests.TestApplication.class,
        properties = "dubbo.enabled=false")
class TenApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({
            GatewayRateLimitConfig.class,
            GatewayFallbackController.class,
            RequestLogFilter.class
    })
    static class TestApplication {
    }
}
