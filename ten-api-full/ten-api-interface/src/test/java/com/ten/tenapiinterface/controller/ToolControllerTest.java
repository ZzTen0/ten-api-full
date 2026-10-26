package com.ten.tenapiinterface.controller;

import com.ten.common.security.RequestSignature;
import com.ten.tenapiinterface.security.GatewayAuthenticationFilter;
import com.ten.tenapiinterface.service.ToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ToolControllerTest {
    private static final String SECRET = "tool-controller-internal-secret-0123456789";
    private MockMvc mvc;

    @BeforeEach @SuppressWarnings("unchecked") void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ToolController(new ToolService()))
                .setControllerAdvice(new ToolExceptionHandler()).setValidator(validator)
                .addFilters(new GatewayAuthenticationFilter(redis, SECRET)).build();
    }

    private ResultActions call(String method, String target, String body) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String proof = RequestSignature.sign(method, target, "42", timestamp, nonce,
                body.getBytes(StandardCharsets.UTF_8), SECRET);
        return mvc.perform(request(HttpMethod.valueOf(method), URI.create(target)).contextPath("/api")
                .header("X-User-Id", "42").header("X-Gateway-Signature", proof)
                .header("timestamp", timestamp).header("nonce", nonce)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test void allElevenEndpointsAreCallableThroughAuthenticationFilter() throws Exception {
        call("POST", "/api/tools/password", "{}").andExpect(status().isOk()).andExpect(jsonPath("$.data.length").value(16))
                .andExpect(header().string("Cache-Control", "no-store"));
        call("GET", "/api/tools/uuid?count=2&hyphens=false", "").andExpect(status().isOk()).andExpect(jsonPath("$.data.uuids.length()").value(2));
        call("POST", "/api/tools/timestamp", "{\"dateTime\":\"1970-01-01T00:00:00\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timestamp").value(0));
        call("POST", "/api/tools/text/stats", "{\"text\":\"hello\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.characters").value(5));
        call("POST", "/api/tools/base64", "{\"text\":\"hello\"}").andExpect(status().isOk()).andExpect(jsonPath("$.data.result").value("aGVsbG8="));
        call("POST", "/api/tools/json", "{\"text\":\"{\\\"x\\\":1}\",\"mode\":\"compress\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("{\"x\":1}"));
        call("POST", "/api/tools/digest", "{\"text\":\"abc\"}").andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        call("GET", "/api/tools/random?min=7&max=7", "").andExpect(status().isOk()).andExpect(jsonPath("$.data.numbers[0]").value(7));
        call("POST", "/api/tools/date", "{\"operation\":\"add\",\"date\":\"2024-02-28\",\"days\":1}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("2024-02-29"));
        call("POST", "/api/tools/units", "{\"category\":\"length\",\"from\":\"m\",\"to\":\"cm\",\"value\":1}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value(100));
        call("GET", "/api/tools/http-status?code=404", "").andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("NOT_FOUND"));
    }

    @Test void unsignedRequestsCannotReachAnyTool() throws Exception {
        mvc.perform(get("/api/tools/uuid").contextPath("/api")).andExpect(status().isForbidden());
    }

    @ParameterizedTest @CsvSource({"/api/tools/uuid?count=101", "/api/tools/random?count=1001",
            "/api/tools/random?min=10&max=1", "/api/tools/http-status?code=599",
            "/api/tools/http-status?code=invalid", "/api/tools/http-status"})
    void invalidGetParametersReturnConsistentErrors(String target) throws Exception {
        call("GET", target, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40000));
    }

    @Test void jsonValidationAndInvalidPayloadsReturn400WithoutEchoingInput() throws Exception {
        call("POST", "/api/tools/password", "{\"length\":129}").andExpect(status().isBadRequest());
        call("POST", "/api/tools/text/stats", "{}").andExpect(status().isBadRequest());
        call("POST", "/api/tools/units", "{\"category\":\"length\",\"from\":\"m\",\"to\":\"cm\"}").andExpect(status().isBadRequest());
        call("POST", "/api/tools/timestamp", "{\"dateTime\":\"not-a-date\"}").andExpect(status().isBadRequest());
        call("POST", "/api/tools/base64", "{\"operation\":\"decode\",\"text\":\"%secret-input%\"}").andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("%secret-input%"))));
        call("POST", "/api/tools/json", "invalid-json").andExpect(status().isBadRequest());
    }
}
