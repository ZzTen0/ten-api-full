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

/** API calls always pass through the gateway using the v2 signature protocol. */
@Data
public class ApiClient {
    private String accessKey;
    @ToString.Exclude
    private String secretKey;
    private String gatewayHost = "http://localhost:8090";

    public ApiClient() { }

    public ApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public ApiClient withCredentials(String accessKey, String secretKey) {
        ApiClient client = new ApiClient(accessKey, secretKey);
        client.setGatewayHost(gatewayHost);
        return client;
    }

    /** Retained for source compatibility; clients no longer need access to server Redis. */
    @Deprecated
    public ApiClient(String accessKey, String secretKey, RedisTemplate<String, Object> ignored) {
        this(accessKey, secretKey);
    }

    public String getNameByGET(String name) {
        return request("GET", "/api/name/ga?name=" + encode(name), "");
    }

    public String getNameByPOSTPath(String name) {
        return request("POST", "/api/name/path/" + encode(name), "");
    }

    public String getNameByPOSTJson(Username username) {
        return request("POST", "/api/name/object", JSONUtil.toJsonStr(username));
    }

    /** Body is a JSON string, or an object serialized exactly once before signing and sending. */
    public String request(String method, String path, Object body) {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException("API credentials are required");
        }
        if (path == null || !path.startsWith("/") || path.startsWith("//") || path.contains("#")) {
            throw new IllegalArgumentException("Expected a relative API path without a fragment");
        }
        byte[] bytes = (body == null ? "" : body instanceof String ? (String) body : JSONUtil.toJsonStr(body))
                .getBytes(StandardCharsets.UTF_8);
        if (bytes.length > RequestSignature.MAX_BODY_BYTES) {
            throw new IllegalArgumentException("API request body exceeds 1 MiB");
        }
        String url = gatewayHost.replaceAll("/+$", "") + path;
        URI uri = URI.create(url);
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
                } catch (RuntimeException ignored) { /* Non-JSON upstream error: keep the generic message. */ }
                throw new ApiClientException(response.getStatus(), message);
            }
            return response.body();
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
