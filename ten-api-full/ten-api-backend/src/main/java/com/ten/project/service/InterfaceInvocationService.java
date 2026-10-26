package com.ten.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ten.apiclientsdk.client.ApiClient;
import com.ten.apiclientsdk.client.ApiClientException;
import com.ten.project.common.ErrorCode;
import com.ten.project.exception.BusinessException;
import com.ten.project.model.entity.InterfaceInfo;
import com.ten.project.model.entity.User;
import org.springframework.stereotype.Service;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Online debugging uses only the configured gateway, with the logged-in user's credentials. */
@Service
public class InterfaceInvocationService {
    private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)\\}");
    private final ApiClient template;
    private final ObjectMapper json;
    public InterfaceInvocationService(ApiClient template, ObjectMapper json) {
        this.template = template;
        this.json = json;
    }

    public Object invoke(InterfaceInfo info, String raw, User user) {
        String path = info.getUrl();
        String method = info.getMethod() == null ? "" : info.getMethod().toUpperCase(Locale.ROOT);
        if (path == null || !(path.startsWith("/api/tools/") || path.startsWith("/api/name/"))
                || path.contains("..") || path.contains("\\") || path.contains("?") || path.contains("#")
                || path.contains("//") || !("GET".equals(method) || "POST".equals(method))) {
            throw invalid("仅支持当前网关内的 GET/POST 工具或示例接口，请检查接口地址");
        }
        if (raw != null && raw.length() > 65536) throw invalid("调试参数过长");
        JsonNode parameters;
        try {
            parameters = json.readTree(raw == null || raw.trim().isEmpty() ? "{}" : raw);
            if (parameters == null || !parameters.isObject()) throw invalid("参数必须为 JSON 对象");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw invalid("参数必须为合法 JSON 对象");
        }
        com.fasterxml.jackson.databind.node.ObjectNode body = (com.fasterxml.jackson.databind.node.ObjectNode) parameters.deepCopy();
        Matcher matcher = VARIABLE.matcher(path);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            JsonNode value = body.remove(key);
            if (value == null || !value.isValueNode() || value.isNull()) throw invalid("缺少路径参数 " + key);
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(encode(value.asText())));
        }
        matcher.appendTail(resolved);
        path = resolved.toString();
        if (path.contains("{") || path.contains("}")) throw invalid("接口路径模板无效");
        String payload = "";
        if ("GET".equals(method)) {
            StringBuilder query = new StringBuilder();
            Iterator<Map.Entry<String, JsonNode>> fields = body.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue().isNull()) continue;
                if (!entry.getValue().isValueNode()) throw invalid("GET 参数必须为字符串、数字或布尔值");
                query.append(query.length() == 0 ? '?' : '&').append(encode(entry.getKey()))
                        .append('=').append(encode(entry.getValue().asText()));
            }
            path += query;
            if (path.length() > 8192) throw invalid("GET 查询参数过长");
        } else {
            payload = body.toString();
        }
        final String response;
        try {
            response = template.withCredentials(user.getAccessKey(), user.getSecretKey()).request(method, path, payload);
        } catch (ApiClientException e) {
            throw new BusinessException(e.getStatus() == 400 ? ErrorCode.PARAMS_ERROR : ErrorCode.OPERATION_ERROR,
                    e.getMessage());
        }
        try { return json.readTree(response); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { return response; }
    }

    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException e) { throw new IllegalStateException(e); }
    }
    private static BusinessException invalid(String message) { return new BusinessException(ErrorCode.PARAMS_ERROR, message); }
}
