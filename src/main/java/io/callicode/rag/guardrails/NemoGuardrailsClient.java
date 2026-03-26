package io.callicode.rag.guardrails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client for NVIDIA NeMo Guardrails sidecar service.
 * <p>
 * NeMo Guardrails exposes an OpenAI-compatible REST endpoint. When a Colang
 * rail is triggered the response contains {@code "blocked": true} at the top
 * level. If the sidecar is unreachable the client fails open (returns allowed)
 * so the application stays available.
 * </p>
 */
@Slf4j
@Component
public class NemoGuardrailsClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final boolean failOpen;

    public NemoGuardrailsClient(WebClient.Builder builder, GuardrailsProperties props) {
        this.webClient = builder
                .baseUrl(props.getNemo().getBaseUrl())
                .build();
        this.failOpen = props.getNemo().isFailOpen();
    }

    /**
     * Checks whether the user query passes NeMo input rails.
     *
     * @param query the raw user query
     * @return {@link GuardrailResult#allowed()} when rails pass,
     *         {@link GuardrailResult#blocked(String)} when a rail is triggered
     */
    public GuardrailResult checkInput(String query) {
        try {
            NemoResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(Map.of(
                            "model", "self-rag-guardrails",
                            "messages", List.of(Map.of("role", "user", "content", query))
                    ))
                    .retrieve()
                    .bodyToMono(NemoResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            return toGuardrailResult(response);
        } catch (WebClientResponseException e) {
            log.warn("NeMo Guardrails returned HTTP {}, failing open", e.getStatusCode());
            return failOpenResult();
        } catch (Exception e) {
            log.warn("NeMo Guardrails unreachable ({}), failing open", e.getMessage());
            return failOpenResult();
        }
    }

    private GuardrailResult toGuardrailResult(NemoResponse response) {
        if (response == null) {
            return failOpenResult();
        }
        if (Boolean.TRUE.equals(response.blocked())) {
            String reason = response.reason() != null ? response.reason() : "guardrail-triggered";
            return GuardrailResult.blocked(reason);
        }
        return GuardrailResult.allowed();
    }

    private GuardrailResult failOpenResult() {
        if (failOpen) {
            return GuardrailResult.allowed();
        }
        return GuardrailResult.blocked("guardrail-service-unavailable");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NemoResponse(Boolean blocked, String reason) {}
}
