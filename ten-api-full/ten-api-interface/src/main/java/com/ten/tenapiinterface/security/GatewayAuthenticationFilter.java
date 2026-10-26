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

/** All interface endpoints require a short-lived, single-use gateway proof. */
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final String internalSecret;

    public GatewayAuthenticationFilter(StringRedisTemplate redis,
            @Value("${gateway.internal-secret:}") String internalSecret) {
        this.redis = redis;
        this.internalSecret = internalSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && (request.getContextPath() + "/actuator/health").equals(request.getRequestURI());
    }

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

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + status * 100 + ",\"message\":\"" + message + "\",\"data\":null}");
    }

    private static class CachedRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] bytes, int off, int len) { return input.read(bytes, off, len); }
                @Override public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Synchronous request body");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
