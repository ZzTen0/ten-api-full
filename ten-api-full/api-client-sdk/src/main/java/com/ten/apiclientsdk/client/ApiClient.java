package com.ten.apiclientsdk.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.ten.apiclientsdk.model.Username;
import com.ten.common.security.RequestSignature;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 开放接口调用客户端。
 *
 * <p>统一通过配置的网关发送 v2 签名请求，按实际传输字节计算请求体摘要，
 * 无需连接服务端 Redis。</p>
 */
@Data
public class ApiClient {
    /**
     * 调用方访问密钥 AK。
     */
    private String accessKey;

    /**
     * 调用方签名密钥 SK，不参与客户端对象的字符串输出。
     */
    @ToString.Exclude
    private String secretKey;

    /**
     * 网关基础地址，默认使用本地 8090 端口。
     */
    private String gatewayHost = "http://localhost:8090";

    /**
     * 创建待配置凭据的客户端。
     */
    public ApiClient() {
    }

    /**
     * 创建具备调用凭据的客户端。
     *
     * @param accessKey 调用方访问密钥
     * @param secretKey 调用方签名密钥
     */
    public ApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * 创建使用指定凭据的独立客户端，并继承当前网关地址。
     *
     * @param accessKey 本次调用方访问密钥
     * @param secretKey 本次调用方签名密钥
     * @return 使用指定凭据的新客户端，不修改当前客户端
     */
    public ApiClient withCredentials(String accessKey, String secretKey) {
        ApiClient client = new ApiClient(accessKey, secretKey);
        client.setGatewayHost(gatewayHost);
        return client;
    }

    /**
     * 兼容旧版带 Redis 参数的构造方式。
     *
     * @param accessKey 调用方访问密钥
     * @param secretKey 调用方签名密钥
     * @param ignored 仅为源码兼容保留，不再参与 nonce 或鉴权处理
     * @deprecated v2 协议无需客户端访问服务端 Redis，请使用双参数构造方法。
     */
    @Deprecated
    public ApiClient(String accessKey, String secretKey, RedisTemplate<String, Object> ignored) {
        this(accessKey, secretKey);
    }

    /**
     * 调用查询参数形式的名字示例接口。
     *
     * @param name 待查询的名字，自动执行 URL 编码
     * @return 接口返回的文本
     */
    public String getNameByGET(String name) {
        return request("GET", "/api/name/ga?name=" + encode(name), "");
    }

    /**
     * 调用路径变量形式的名字示例接口。
     *
     * @param name 路径参数中的名字，自动执行 URL 编码
     * @return 接口返回的文本
     */
    public String getNameByPOSTPath(String name) {
        return request("POST", "/api/name/path/" + encode(name), "");
    }

    /**
     * 调用 JSON 请求体形式的名字示例接口。
     *
     * @param username 用户名请求对象
     * @return 接口返回的文本
     */
    public String getNameByPOSTJson(Username username) {
        return request("POST", "/api/name/object", JSONUtil.toJsonStr(username));
    }

    /**
     * 发送带 v2 签名的通用接口请求。
     *
     * @param method HTTP 方法
     * @param path 以单个斜线开头的相对接口路径，可包含已编码的查询串，不允许 URL 片段
     * @param body 原始 JSON 字符串或待序列化对象；无请求体时使用空字符串或 null
     * @return 接口响应文本
     * @throws IllegalArgumentException 路径格式无效、方法不支持或请求体超过 1 MiB
     * @throws IllegalStateException 调用凭据未配置
     * @throws ApiClientException 接口返回非成功 HTTP 状态
     */
    public String request(String method, String path, Object body) {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("API credentials are required");
        }
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("#")) {
            throw new IllegalArgumentException("Expected a relative API path without a fragment");
        }
        // 请求体只序列化一次，签名和实际发送共用同一份 UTF-8 字节，避免内容不一致。
        byte[] bytes = (body == null ? "" : body instanceof String ? (String) body : JSONUtil.toJsonStr(body))
                .getBytes(StandardCharsets.UTF_8);
        if (bytes.length > RequestSignature.MAX_BODY_BYTES) {
            throw new IllegalArgumentException("API request body exceeds 1 MiB");
        }
        String url = gatewayHost.replaceAll("/+$", "") + path;
        URI uri = URI.create(url);
        // 每个请求生成独立 nonce 与当前秒级时间戳，不复用客户端鉴权头。
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, String> headers = new HashMap<>();
        headers.put("accessKey", accessKey);
        headers.put("nonce", nonce);
        headers.put("timestamp", timestamp);
        headers.put("signatureVersion", RequestSignature.VERSION);
        headers.put("sign", RequestSignature.sign(method, RequestSignature.target(uri),
                accessKey, timestamp, nonce, bytes, secretKey));
        HttpRequest request = HttpRequest.of(url).method(Method.valueOf(method.toUpperCase(java.util.Locale.ROOT)))
                .charset(StandardCharsets.UTF_8).addHeaders(headers).timeout(10000);
        if (bytes.length > 0) {
            request.header("Content-Type", "application/json; charset=UTF-8").body(bytes);
        }
        try (HttpResponse response = request.execute()) {
            if (!response.isOk()) {
                String message = "API request failed (HTTP " + response.getStatus() + ")";
                try {
                    String detail = JSONUtil.parseObj(response.body()).getStr("message");
                    if (detail != null && detail.length() <= 256) message = detail;
                } catch (RuntimeException ignored) { /* 非 JSON 上游错误保留通用提示，避免将响应原文写入异常。 */ }
                throw new ApiClientException(response.getStatus(), message);
            }
            return response.body();
        }
    }

    /**
     * 将参数编码为 UTF-8 百分号形式，空格使用 %20。
     *
     * @param value 原始参数值
     * @return 编码后的参数值
     */
    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
