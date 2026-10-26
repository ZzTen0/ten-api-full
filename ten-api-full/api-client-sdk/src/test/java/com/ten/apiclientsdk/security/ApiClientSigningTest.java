package com.ten.apiclientsdk.security;

import com.sun.net.httpserver.HttpServer;
import com.ten.apiclientsdk.client.ApiClient;
import com.ten.apiclientsdk.model.Username;
import com.ten.common.security.RequestSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ApiClientSigningTest {
    private HttpServer server;
    private ApiClient client;
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicReference<String> target = new AtomicReference<>();

    @BeforeEach void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                byte[] body = StreamUtils.copyToByteArray(exchange.getRequestBody());
                target.set(RequestSignature.target(exchange.getRequestURI()));
                String expected = RequestSignature.sign(exchange.getRequestMethod(), target.get(), "ak",
                        exchange.getRequestHeaders().getFirst("timestamp"),
                        exchange.getRequestHeaders().getFirst("nonce"), body, "secret");
                assertEquals("2", exchange.getRequestHeaders().getFirst("signatureVersion"));
                assertNull(exchange.getRequestHeaders().getFirst("body"));
                assertEquals(expected, exchange.getRequestHeaders().getFirst("sign"));
                byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (Throwable failure) {
                error.set(failure);
                exchange.sendResponseHeaders(500, -1);
            } finally {
                exchange.close();
            }
        });
        server.start();
        client = new ApiClient("ak", "secret");
        client.setGatewayHost("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach void tearDown() {
        server.stop(0);
        assertNull(error.get(), () -> String.valueOf(error.get()));
    }

    @Test void signsGetQueryExactlyAsSent() {
        assertEquals("ok", client.getNameByGET("张 三&x=1"));
        assertTrue(target.get().contains("%26x%3D1"));
    }

    @Test void signsEncodedPathExactlyAsSent() {
        assertEquals("ok", client.getNameByPOSTPath("a b"));
        assertEquals("/api/name/path/a%20b", target.get());
    }

    @Test void signsActualUtf8JsonBytes() {
        Username username = new Username();
        username.setUsername("测试名字");
        assertEquals("ok", client.getNameByPOSTJson(username));
    }

    @Test void rejectsHttpErrors() {
        server.removeContext("/");
        server.createContext("/", exchange -> { exchange.sendResponseHeaders(403, -1); exchange.close(); });
        assertThrows(IllegalStateException.class, () -> client.getNameByGET("test"));
    }

    @Test void rejectsAbsoluteUrlAndMissingCredentials() {
        assertThrows(IllegalArgumentException.class, () -> client.request("POST", "https://example.com", "{}"));
        client.setSecretKey("");
        assertThrows(IllegalStateException.class, () -> client.getNameByGET("test"));
    }
}
