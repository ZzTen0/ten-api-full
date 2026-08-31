package com.ten.apiclientsdk;

import com.ten.apiclientsdk.client.ApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("api.client")
@Data
@ComponentScan
public class ApiClientConfig {

    private String accessKey;

    private String secretKey;

    /**
     * 网关地址，可配置
     */
    private String gatewayHost = "http://localhost:8090";

    @Bean
    public ApiClient apiClient() {
        ApiClient apiClient = new ApiClient(accessKey, secretKey);
        apiClient.setGatewayHost(gatewayHost);
        return apiClient;
    }
}
