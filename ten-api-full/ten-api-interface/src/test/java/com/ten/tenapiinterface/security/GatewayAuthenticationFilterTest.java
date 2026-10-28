package com.ten.tenapiinterface.security;

import com.ten.common.security.RequestSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 接口服务内部鉴权过滤器测试。
 *
 * <p>覆盖真实请求体保留、身份及内容篡改、重放拒绝和依赖故障时拒绝放行。</p>
 */
class GatewayAuthenticationFilterTest {
    private static final String SECRET = "internal-secret-for-tests-0123456789";
    private static final String NONCE = "0123456789abcdef";
    private static final byte[] BODY = "{\"username\":\"测试\"}".getBytes(StandardCharsets.UTF_8);
    private final String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private GatewayAuthenticationFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        filter = new GatewayAuthenticationFilter(redis, SECRET);
    }

    private MockHttpServletRequest request(String method, String path, byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setContextPath("/api");
        request.setContent(body);
        request.addHeader("X-User-Id", "42");
        request.addHeader("timestamp", timestamp);
        request.addHeader("nonce", NONCE);
        request.addHeader("X-Gateway-Signature", RequestSignature.sign(method, path, "42",
                timestamp, NONCE, body, SECRET));
        return request;
    }

    @Test
    void validProofPreservesBodyForController() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.doFilter(request("POST", "/api/name/object", BODY), response, (req, res) -> {
            assertArrayEquals(BODY, StreamUtils.copyToByteArray(req.getInputStream()));
            forwarded.set(true);
        });
        assertTrue(forwarded.get());
        verify(values).setIfAbsent(eq("interface:nonce:42:" + NONCE), eq(timestamp), eq(Duration.ofSeconds(601)));
    }

    @Test
    void userIdHeaderAloneCannotBypassAuthentication() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/name/object", BODY);
        request.removeHeader("X-Gateway-Signature");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Spoofed identity accepted"));
        assertEquals(403, response.getStatus());
        verifyNoInteractions(values);
    }

    @Test
    void rejectsTamperedBody() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/name/object", BODY);
        request.setContent("tampered".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Tampered body accepted"));
        assertEquals(403, response.getStatus());
        verifyNoInteractions(values);
    }

    @Test
    void rejectsTamperedIdentity() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/name/object", BODY);
        request.removeHeader("X-User-Id");
        request.addHeader("X-User-Id", "99");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Tampered identity accepted"));
        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsReplay() throws Exception {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/name/object", BODY), response, (req, res) -> fail("Replay accepted"));
        assertEquals(403, response.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/name/ga", "/api/name/path/test"})
    void protectsGetAndPathEndpointsAsWell(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setContextPath("/api");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Unsigned endpoint accepted"));
        assertEquals(403, response.getStatus());
    }

    @Test
    void healthCheckRemainsAvailableWithoutCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/actuator/health");
        request.setContextPath("/api");
        AtomicBoolean forwarded = new AtomicBoolean();
        new GatewayAuthenticationFilter(redis, "").doFilter(request, new MockHttpServletResponse(),
                (req, res) -> forwarded.set(true));
        assertTrue(forwarded.get());
        verifyNoInteractions(values);
    }

    @Test
    void redisFailureFailsClosedWithoutLeakingDetails() throws Exception {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("redis-password"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("POST", "/api/name/object", BODY), response, (req, res) -> fail("Redis error ignored"));
        assertEquals(503, response.getStatus());
        assertFalse(response.getContentAsString().contains("redis-password"));
    }

    @Test
    void missingInternalSecretFailsClosed() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new GatewayAuthenticationFilter(redis, "").doFilter(request("POST", "/api/name/object", BODY), response,
                (req, res) -> fail("Missing secret ignored"));
        assertEquals(503, response.getStatus());
    }
}
