package io.callicode.rag.guardrails;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailsAiClientTest {

    private MockWebServer mockWebServer;
    private GuardrailsAiClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        GuardrailsProperties props = new GuardrailsProperties();
        props.getGuardrailsAi().setBaseUrl(mockWebServer.url("/").toString());
        props.getGuardrailsAi().setFailOpen(true);

        client = new GuardrailsAiClient(WebClient.builder(), props);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void validateOutput_allowed_whenValidationPasses() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"validationPassed": true, "validatedOutput": "Spring Boot simplifies development."}
                        """));

        GuardrailResult result = client.validateOutput("Spring Boot simplifies development.");

        assertThat(result.isAllowed()).isTrue();
    }

    @Test
    void validateOutput_blocked_whenToxicLanguageDetected() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"validationPassed": false, "error": "toxic_language"}
                        """));

        GuardrailResult result = client.validateOutput("some toxic content here");

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).isEqualTo("toxic_language");
    }

    @Test
    void validateOutput_blocked_whenSecretsDetected() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"validationPassed": false, "error": "secrets_present"}
                        """));

        GuardrailResult result = client.validateOutput("API key: sk-1234567890abcdef");

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.reason()).isEqualTo("secrets_present");
    }

    @Test
    void validateOutput_failOpen_whenSidecarUnreachable() {
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        GuardrailResult result = client.validateOutput("A perfectly valid answer.");

        assertThat(result.isAllowed()).isTrue();
    }
}
