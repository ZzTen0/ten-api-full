package com.ten.apiclientsdk.client;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.ten.apiclientsdk.model.Username;
import lombok.Data;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.ten.apiclientsdk.utils.SignUtils.getSign;

/**
 * 调用第三方接口的客户端
 */
@Data
public class ApiClient {

    private String accessKey;
    private String secretKey;
    /**
     * 网关地址，可配置
     */
    private String gatewayHost = "http://localhost:8090";

    private RedisTemplate<String, Object> redisTemplate;

    public ApiClient() {
    }

    public ApiClient(String accessKey, String secretKey, RedisTemplate<String, Object> redisTemplate) {
        this.secretKey = secretKey;
        this.accessKey = accessKey;
        this.redisTemplate = redisTemplate;
    }

    public ApiClient(String accessKey, String secretKey) {
        this.secretKey = secretKey;
        this.accessKey = accessKey;
    }

    public String getNameByGET(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        return HttpUtil.get(gatewayHost + "/api/name/ga", paramMap);
    }

    public String getNameByPOSTPath(String name) {
        String url = gatewayHost + "/api/name/path/" + name;
        return HttpUtil.post(url, "");
    }

    private Map<String, String> getHeaderMap(String body) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        String nonce = RandomUtil.randomString(16);
        hashMap.put("nonce", nonce);
        String nonceKey = "nonce:" + accessKey + ":" + nonce;
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(nonceKey, nonce, 5, TimeUnit.MINUTES);
        }
        hashMap.put("body", body);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", getSign(body, secretKey));
        return hashMap;
    }

    public String getNameByPOSTJson(Username username) {
        String json = JSONUtil.toJsonStr(username);
        HttpResponse httpResponse = HttpRequest.post(gatewayHost + "/api/name/object")
                .charset(StandardCharsets.UTF_8)
                .addHeaders(getHeaderMap(json))
                .body(json)
                .execute();
        return httpResponse.body();
    }
}
