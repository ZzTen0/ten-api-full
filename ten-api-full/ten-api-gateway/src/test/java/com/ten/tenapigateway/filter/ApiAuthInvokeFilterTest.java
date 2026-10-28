package com.ten.tenapigateway.filter;

import com.ten.common.model.vo.InvokeUserVO;
import com.ten.common.security.RequestSignature;
import com.ten.common.service.InnerUserInterfaceInfoService;
import com.ten.common.service.InnerUserService;
import com.ten.common.service.InnerInterfaceInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 网关开放接口鉴权及额度路由测试。
 *
 * <p>覆盖请求篡改、真实接口 ID、下线拦截、重放拒绝和下游错误隔离，RPC 与 Redis 使用 mock。</p>
 */
class ApiAuthInvokeFilterTest {
    private static final String SECRET = "client-secret";
    private static final String INTERNAL = "internal-secret-for-tests-0123456789";
    private static final String NONCE = "0123456789abcdef0123456789abcdef";
    private static final String PATH = "/api/name/object?lang=zh";
    private static final String BODY = "{\"username\":\"测试\"}";
    private final String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    private ApiAuthInvokeFilter filter;
    private InnerUserService users;
    private InnerInterfaceInfoService interfaces;
    private InnerUserInterfaceInfoService quota;
    private ReactiveStringRedisTemplate redis;
    private ReactiveValueOperations<String, String> values;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        filter = new ApiAuthInvokeFilter();
        users = mock(InnerUserService.class);
        interfaces = mock(InnerInterfaceInfoService.class);
        quota = mock(InnerUserInterfaceInfoService.class);
        redis = mock(ReactiveStringRedisTemplate.class);
        values = mock(ReactiveValueOperations.class);
        InvokeUserVO user = new InvokeUserVO();
        user.setId(42L);
        user.setSecretKey(SECRET);
        when(users.getInvokeUser("ak")).thenReturn(user);
        when(interfaces.resolveOnlineInterfaceId(anyString(), anyString())).thenReturn(1L);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        when(quota.invokeCount(42L, 1L)).thenReturn(true);
        ReflectionTestUtils.setField(filter, "innerUserService", users);
        ReflectionTestUtils.setField(filter, "innerInterfaceInfoService", interfaces);
        ReflectionTestUtils.setField(filter, "innerUserInterfaceInfoService", quota);
        ReflectionTestUtils.setField(filter, "redisTemplate", redis);
        ReflectionTestUtils.setField(filter, "properties", new GatewayInvokeProperties());
        ReflectionTestUtils.setField(filter, "internalSecret", INTERNAL);
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String body, String sign) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, URI.create(path))
                .header("accessKey", "ak").header("timestamp", timestamp).header("nonce", NONCE)
                .header("signatureVersion", "2").header("sign", sign)
                .header("body", "untrusted old header")
                .header("X-User-Id", "999").header("X-Gateway-Signature", "forged")
                .body(body));
    }

    private String signature() {
        return RequestSignature.sign("POST", PATH, "ak", timestamp, NONCE,
                BODY.getBytes(StandardCharsets.UTF_8), SECRET);
    }

    private void rejected(MockServerWebExchange exchange) {
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.filter(exchange, next -> { forwarded.set(true); return Mono.empty(); }).block();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(forwarded.get());
        verifyNoInteractions(quota, values);
    }

    @Test
    void authenticatesActualBodyAndForwardsItWithFreshIdentityProof() {
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        AtomicReference<String> forwardedBody = new AtomicReference<>();
        filter.filter(exchange, next -> {
            assertEquals("42", next.getRequest().getHeaders().getFirst("X-User-Id"));
            assertNull(next.getRequest().getHeaders().getFirst("body"));
            assertEquals(RequestSignature.sign("POST", PATH, "42", timestamp, NONCE,
                    BODY.getBytes(StandardCharsets.UTF_8), INTERNAL),
                    next.getRequest().getHeaders().getFirst("X-Gateway-Signature"));
            return DataBufferUtils.join(next.getRequest().getBody()).doOnNext(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                forwardedBody.set(new String(bytes, StandardCharsets.UTF_8));
            }).then();
        }).block();
        assertEquals(BODY, forwardedBody.get());
        verify(values).setIfAbsent(eq("gateway:nonce:ak:" + NONCE), eq(timestamp), eq(Duration.ofSeconds(601)));
        verify(quota).invokeCount(42L, 1L);
    }

    @Test
    void rejectsChangedBodyEvenWithAnUnchangedBodyHeader() {
        rejected(exchange(HttpMethod.POST, PATH, "{\"username\":\"attacker\"}", signature()));
    }

    @Test
    void chargesTheResolvedInterfaceInsteadOfAFixedId() {
        when(interfaces.resolveOnlineInterfaceId("POST", "/api/name/object")).thenReturn(77L);
        when(quota.invokeCount(42L, 77L)).thenReturn(true);
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        filter.filter(exchange, next -> Mono.empty()).block();
        verify(quota).invokeCount(42L, 77L);
        verify(quota, never()).invokeCount(42L, 1L);
    }

    @Test
    void offlineOrUnknownInterfacesAreNotCharged() {
        when(interfaces.resolveOnlineInterfaceId(anyString(), anyString())).thenReturn(null);
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        filter.filter(exchange, next -> fail("Offline interface was forwarded")).block();
        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
        verifyNoInteractions(quota, values);
    }

    @Test
    void rejectsChangedQuery() {
        rejected(exchange(HttpMethod.POST, "/api/name/object?lang=en", BODY, signature()));
    }

    @Test
    void rejectsChangedMethod() {
        rejected(exchange(HttpMethod.PUT, PATH, BODY, signature()));
    }

    @Test
    void rejectsChangedPath() {
        rejected(exchange(HttpMethod.POST, "/api/name/path/other", BODY, signature()));
    }

    @Test
    void rejectsLegacySignatures() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post(PATH)
                .header("accessKey", "ak").header("timestamp", timestamp).header("nonce", NONCE)
                .header("sign", signature()).body(BODY));
        rejected(exchange);
        verifyNoInteractions(users);
    }

    @Test
    void rejectsReplayWithoutCharging() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(false));
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        filter.filter(exchange, next -> fail("Replay was forwarded")).block();
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verifyNoInteractions(quota);
    }

    @Test
    void failsClosedWhenInternalSecretIsMissing() {
        ReflectionTestUtils.setField(filter, "internalSecret", "");
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        filter.filter(exchange, next -> fail("Request was forwarded")).block();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        verifyNoInteractions(users, quota, values);
    }

    @Test
    void rejectsOversizedBodiesBeforeAuthentication() {
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH,
                String.join("", Collections.nCopies(RequestSignature.MAX_BODY_BYTES + 1, "a")), signature());
        filter.filter(exchange, next -> fail("Oversized request was forwarded")).block();
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exchange.getResponse().getStatusCode());
        verifyNoInteractions(users, quota, values);
    }

    @Test
    void stripsSpoofedIdentityOnSessionRoutes() {
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/api/user/login", "{}", signature());
        filter.filter(exchange, next -> {
            assertNull(next.getRequest().getHeaders().getFirst("X-User-Id"));
            assertNull(next.getRequest().getHeaders().getFirst("X-Gateway-Signature"));
            return Mono.empty();
        }).block();
        verifyNoInteractions(users, quota, values);
    }

    @Test
    void whitelistCannotDisableOpenApiAuthentication() {
        GatewayInvokeProperties properties = new GatewayInvokeProperties();
        properties.setWhiteList(Collections.singletonList("/api/**"));
        ReflectionTestUtils.setField(filter, "properties", properties);
        rejected(exchange(HttpMethod.POST, PATH, "tampered", signature()));
    }

    @Test
    void downstreamErrorsAreNotMisreportedAsAuthenticationFailures() {
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        assertThrows(IllegalStateException.class, () -> filter.filter(exchange,
                next -> Mono.error(new IllegalStateException("downstream failure"))).block());
        assertNotEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void dependencyFailuresDoNotExposeExceptionDetails() {
        when(users.getInvokeUser("ak")).thenThrow(new RuntimeException("private-password-in-RPC-error"));
        MockServerWebExchange exchange = exchange(HttpMethod.POST, PATH, BODY, signature());
        filter.filter(exchange, next -> fail("Request was forwarded")).block();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        assertFalse(exchange.getResponse().getBodyAsString().block().contains("private-password"));
    }
}
