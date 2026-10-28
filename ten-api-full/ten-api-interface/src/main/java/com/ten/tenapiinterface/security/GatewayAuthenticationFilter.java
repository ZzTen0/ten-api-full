package com.ten.tenapiinterface.security;

import com.ten.common.security.RequestSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 接口服务内部网关身份过滤器。
 *
 * <p>所有业务入口均须验证网关签名及单次 nonce，防止伪造 X-User-Id 绕过鉴权；
 * 健康检查仅放行精确的 GET 路径。</p>
 */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    /**
     * 用于消费内部 nonce 的 Redis 模板，与网关的外部 nonce 记录相互独立。
     */
    private final StringRedisTemplate redis;

    /**
     * 与网关共享的独立内部鉴权密钥。
     */
    private final String internalSecret;

    /**
     * 创建内部网关鉴权过滤器。
     *
     * @param redis Redis 操作模板
     * @param internalSecret 网关内部密钥，未满足配置要求时拒绝业务调用
     */
    public GatewayAuthenticationFilter(StringRedisTemplate redis,
            @Value("${gateway.internal-secret:}") String internalSecret) {
        this.redis = redis;
        this.internalSecret = internalSecret;
    }

    /**
     * 判断是否为可免内部鉴权的健康检查。
     *
     * @param request 当前 HTTP 请求
     * @return 仅精确的 GET 健康检查路径返回 true
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && (request.getContextPath() + "/actuator/health").equals(request.getRequestURI());
    }

    /**
     * 验证网关身份、真实请求体签名及内部防重放记录。
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param chain 后续过滤器链
     * @throws ServletException 下游 Servlet 处理失败
     * @throws IOException 读取请求体或写入响应失败
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!RequestSignature.validInternalSecret(internalSecret)) {
            reject(response, 503, "接口内部鉴权尚未配置");
            return;
        }
        String userId = request.getHeader("X-User-Id");
        String proof = request.getHeader("X-Gateway-Signature");
        String timestamp = request.getHeader("timestamp");
        String nonce = request.getHeader("nonce");
        if (userId == null || !userId.matches("[1-9][0-9]{0,18}")
                || !RequestSignature.validNonce(nonce)
                || !RequestSignature.validTimestamp(timestamp, System.currentTimeMillis() / 1000)) {
            reject(response, 403, "需要有效的网关身份");
            return;
        }
        if (request.getContentLengthLong() > RequestSignature.MAX_BODY_BYTES) {
            reject(response, 413, "请求体超过 1 MiB");
            return;
        }
        // 除 Content-Length 预检查外，读取过程中也限制大小，覆盖分块传输及未知长度请求。
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = request.getInputStream().read(buffer)) != -1) {
            if (output.size() + count > RequestSignature.MAX_BODY_BYTES) {
                reject(response, 413, "请求体超过 1 MiB");
                return;
            }
            output.write(buffer, 0, count);
        }
        byte[] body = output.toByteArray();
        String target = request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String expected = RequestSignature.sign(request.getMethod(), target, userId, timestamp, nonce, body, internalSecret);
        if (!RequestSignature.matches(expected, proof)) {
            reject(response, 403, "网关签名无效");
            return;
        }
        // 先验证签名，再原子登记内部 nonce；Redis 不可用时不允许业务请求继续执行。
        final Boolean created;
        try {
            created = redis.opsForValue().setIfAbsent("interface:nonce:" + userId + ":" + nonce, timestamp,
                    Duration.ofSeconds(RequestSignature.NONCE_TTL_SECONDS));
        } catch (RuntimeException e) {
            reject(response, 503, "鉴权服务暂不可用");
            return;
        }
        if (!Boolean.TRUE.equals(created)) {
            reject(response, 403, "重复请求");
            return;
        }
        chain.doFilter(new CachedRequest(request, body), response);
    }

    /**
     * 输出受控的内部鉴权失败响应。
     *
     * @param response 当前 HTTP 响应
     * @param status HTTP 状态码
     * @param message 服务端固定错误说明
     * @throws IOException 写入响应失败
     */
    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + status * 100 + ",\"message\":\"" + message + "\",\"data\":null}");
    }

    /**
     * 缓存已完成鉴权的请求体，为控制器提供可再次读取的请求包装器。
     */
    private static class CachedRequest extends HttpServletRequestWrapper {
        /**
         * 已验证的原始请求体字节。
         */
        private final byte[] body;
        /**
         * 创建缓存请求包装器。
         *
         * @param request 原始 HTTP 请求
         * @param body 已验证的请求体字节
         */
        CachedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        /**
         * 重新打开已验证请求体的同步输入流。
         *
         * @return 从缓存字节起点开始读取的新输入流
         */
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isReady() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int read() {
                    return input.read();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int read(byte[] bytes, int off, int len) {
                    return input.read(bytes, off, len);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Synchronous request body");
                }
            };
        }

        /**
         * 按 UTF-8 重新打开已验证请求体的文本读取器。
         *
         * @return 基于缓存输入流的 UTF-8 读取器
         */
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
