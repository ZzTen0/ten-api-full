package com.ten.tenapigateway.filter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权白名单配置
 */
@Component
@ConfigurationProperties(prefix = "gateway.invoke")
@Data
public class GatewayInvokeProperties {

    /**
     * 不需要签名校验的白名单路径（管理后台登录注册等）
     */
    private List<String> whiteList = new ArrayList<>();

    /**
     * 对外开放API（需要签名校验）的路径前缀或关键词
     * 约定：URL 中包含 /name/ 或其他接口模块的前缀走签名校验；
     * 若留空则除白名单外的 POST/PUT 请求全部尝试签名校验
     */
    private List<String> apiPrefix = new ArrayList<>();
}
