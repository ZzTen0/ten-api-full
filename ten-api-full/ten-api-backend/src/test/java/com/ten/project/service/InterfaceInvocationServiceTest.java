package com.ten.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ten.apiclientsdk.client.ApiClient;
import com.ten.apiclientsdk.client.ApiClientException;
import com.ten.project.exception.BusinessException;
import com.ten.project.model.entity.InterfaceInfo;
import com.ten.project.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 在线试调用服务测试。
 *
 * <p>覆盖所选接口路由、GET/POST 参数编码、网关地址继承和上游错误传播。</p>
 */
class InterfaceInvocationServiceTest {
    private ApiClient client;
    private InterfaceInvocationService service;
    private User user;
    private InterfaceInfo info;

    @BeforeEach
    void setUp() {
        ApiClient template = mock(ApiClient.class);
        client = mock(ApiClient.class);
        when(template.withCredentials("ak", "sk")).thenReturn(client);
        service = new InterfaceInvocationService(template, new ObjectMapper());
        user = new User(); user.setAccessKey("ak"); user.setSecretKey("sk");
        info = new InterfaceInfo();
    }

    @Test
    void getQueryUsesSelectedEndpointAndEncodesParameters() {
        info.setUrl("/api/tools/random"); info.setMethod("GET");
        when(client.request("GET", "/api/tools/random?min=1&max=5&unique=true", ""))
                .thenReturn("{\"code\":0,\"data\":{\"numbers\":[1]}}");
        Object response = service.invoke(info, "{\"min\":1,\"max\":5,\"unique\":true}", user);
        assertEquals(1, ((JsonNode) response).get("data").get("numbers").get(0).asInt());
        verify(client).request("GET", "/api/tools/random?min=1&max=5&unique=true", "");
    }

    @Test
    void postUsesActualJsonAndSelectedEndpoint() {
        info.setUrl("/api/tools/base64"); info.setMethod("POST");
        when(client.request("POST", "/api/tools/base64", "{\"text\":\"你好\"}")).thenReturn("{\"code\":0}");
        service.invoke(info, "{\"text\":\"你好\"}", user);
        verify(client).request("POST", "/api/tools/base64", "{\"text\":\"你好\"}");
    }

    @Test
    void legacyPathParametersRemainCallableAndAreEncoded() {
        info.setUrl("/api/name/path/{name}"); info.setMethod("POST");
        when(client.request("POST", "/api/name/path/a%20b", "{}")).thenReturn("POST 你的名字是a b");
        assertEquals("POST 你的名字是a b", service.invoke(info, "{\"name\":\"a b\"}", user));
    }

    @Test
    void onlyGatewayPathsAreAllowedAndMalformedParametersAreRejected() {
        info.setUrl("http://127.0.0.1:3306"); info.setMethod("POST");
        assertThrows(BusinessException.class, () -> service.invoke(info, "{}", user));
        info.setUrl("/api/tools/../user/delete");
        assertThrows(BusinessException.class, () -> service.invoke(info, "{}", user));
        info.setUrl("/api/tools/base64");
        assertThrows(BusinessException.class, () -> service.invoke(info, "[]", user));
        assertThrows(BusinessException.class, () -> service.invoke(info, "invalid-json", user));
        info.setMethod("GET");
        assertThrows(BusinessException.class, () -> service.invoke(info, "{\"text\":{}}", user));
        verifyNoInteractions(client);
    }

    @Test
    void upstreamValidationFailureDoesNotBecomeASuccessResult() {
        info.setUrl("/api/tools/password"); info.setMethod("POST");
        when(client.request("POST", "/api/tools/password", "{\"length\":999}"))
                .thenThrow(new ApiClientException(400, "length 参数无效"));
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.invoke(info, "{\"length\":999}", user));
        assertEquals("length 参数无效", error.getMessage());
    }

    @Test
    void sdkCredentialCopyPreservesConfiguredGateway() {
        ApiClient template = new ApiClient();
        template.setGatewayHost("http://ten-api-gateway:8090");
        ApiClient scoped = template.withCredentials("ak", "sk");
        assertEquals("http://ten-api-gateway:8090", scoped.getGatewayHost());
        assertEquals("ak", scoped.getAccessKey());
        assertNull(template.getAccessKey());
    }
}
