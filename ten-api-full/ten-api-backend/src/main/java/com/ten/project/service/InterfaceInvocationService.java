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

/**
 * 接口在线试调用服务。
 *
 * <p>仅通过配置的网关调用本平台工具与示例接口，使用当前登录用户的 AK/SK，
 * 并依据接口元数据组装路径变量、查询参数和 JSON 请求体。</p>
 */
@Service
public class InterfaceInvocationService {
    /**
     * 接口元数据中路径变量的匹配规则，例如 {name}。
     */
    private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_]*)\\}");

    /**
     * 持有网关地址配置的 SDK 客户端模板。
     */
    private final ApiClient template;

    /**
     * 在线试调用的 JSON 参数及响应解析器。
     */
    private final ObjectMapper json;

    /**
     * 创建在线试调用服务。
     *
     * @param template 持有网关地址配置的 SDK 客户端
     * @param json JSON 解析器
     */
    public InterfaceInvocationService(ApiClient template, ObjectMapper json) {
        this.template = template;
        this.json = json;
    }

    /**
     * 调用所选接口，并保留该接口返回的结构化结果或文本。
     *
     * @param info 已完成存在性及上线状态校验的接口元数据
     * @param raw 用户填写的 JSON 对象字符串，空值按空对象处理
     * @param user 当前登录用户，提供本次调用的 AK/SK
     * @return JSON 响应树，或非 JSON 接口返回的原始文本
     * @throws BusinessException 接口地址不支持、参数格式无效或上游返回失败状态
     */
    public Object invoke(InterfaceInfo info, String raw, User user) {
        String path = info.getUrl();
        String method = info.getMethod() == null ? "" : info.getMethod().toUpperCase(Locale.ROOT);
        // 固定使用客户端模板的网关地址，禁止元数据将请求引向任意主机或管理端路径。
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
        // 从参数对象中消费路径变量，避免同一字段再次混入查询参数或请求体。
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

    /**
     * 编码单个路径变量或查询参数，将空格统一表示为百分号编码。
     *
     * @param value 原始参数值
     * @return UTF-8 百分号编码后的参数值
     */
    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException e) { throw new IllegalStateException(e); }
    }

    /**
     * 构造在线试调用参数错误。
     *
     * @param message 面向用户的错误说明
     * @return 参数错误业务异常
     */
    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.PARAMS_ERROR, message);
    }
}
