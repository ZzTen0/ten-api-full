package com.ten.apiclientsdk.security;

import com.ten.common.security.RequestSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 公共 v2 签名协议测试。
 *
 * <p>通过独立 HMAC 测试向量、签名字段篡改及时间窗口边界验证协议约束。</p>
 */
class RequestSignatureTest {
    private static final byte[] BODY = "{\"username\":\"ten\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void matchesIndependentHmacVector() {
        assertEquals("00d3d0b5e05713cbc1c95bec56a3c6f71b9d126b791bed15dadf3e0735a8d1f6",
                RequestSignature.sign("POST", "/api/name/object?lang=zh", "ak", "1700000000",
                        "0123456789abcdef", BODY, "test-secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"method", "path", "query", "identity", "timestamp", "nonce", "body"})
    void everyRequestFieldIsAuthenticated(String changed) {
        String original = RequestSignature.sign("POST", "/api/name/object?x=1", "ak", "1700000000",
                "0123456789abcdef", BODY, "secret");
        String modified = RequestSignature.sign(changed.equals("method") ? "PUT" : "POST",
                changed.equals("path") ? "/api/other?x=1" : changed.equals("query") ? "/api/name/object?x=2" : "/api/name/object?x=1",
                changed.equals("identity") ? "other-ak" : "ak", changed.equals("timestamp") ? "1700000001" : "1700000000",
                changed.equals("nonce") ? "0123456789abcdeg" : "0123456789abcdef",
                changed.equals("body") ? new byte[0] : BODY, "secret");
        assertFalse(RequestSignature.matches(original, modified));
    }

    @Test
    void preservesEncodedPathAndQueryOrder() {
        assertEquals("/api/name/path/a%20b?x=%2F&b=2",
                RequestSignature.target(URI.create("http://localhost/api/name/path/a%20b?x=%2F&b=2")));
    }

    @Test
    void rejectsExpiredFutureAndOverflowTimestamps() {
        long now = 1700000000;
        assertTrue(RequestSignature.validTimestamp("1700000300", now));
        assertTrue(RequestSignature.validTimestamp("1699999700", now));
        assertFalse(RequestSignature.validTimestamp("1700000301", now));
        assertFalse(RequestSignature.validTimestamp("1699999699", now));
        assertFalse(RequestSignature.validTimestamp("-9223372036854775808", now));
        assertFalse(RequestSignature.validTimestamp("9223372036854775807", now));
        assertFalse(RequestSignature.validTimestamp(null, now));
        assertTrue(RequestSignature.NONCE_TTL_SECONDS > 2 * RequestSignature.CLOCK_SKEW_SECONDS);
    }

    @Test
    void rejectsMalformedSignaturesAndFields() {
        assertFalse(RequestSignature.matches("00", null));
        assertFalse(RequestSignature.matches("00", "not-hex"));
        assertFalse(RequestSignature.validNonce("short"));
        assertFalse(RequestSignature.validNonce("0123456789abcde\n"));
        assertThrows(IllegalArgumentException.class, () -> RequestSignature.sign("POST",
                "/path\ninjected", "ak", "1700000000", "0123456789abcdef", BODY, "secret"));
    }
}
