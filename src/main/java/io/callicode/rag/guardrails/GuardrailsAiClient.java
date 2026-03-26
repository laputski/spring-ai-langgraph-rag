package io.callicode.rag.guardrails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Client for Guardrails AI sidecar service.
 * <p>
 * Guardrails AI validates LLM output against configured validators (toxic language,
 * secrets detection, etc.). The guard ID is fixed to {@code output-safety-guard}
 * matching the Docker sidecar configuration.
 * </p>
 */
@Slf4j
@Component
public class GuardrailsAiClient {

    private static final String GUARD_ID = "output-safety-guard";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final boolean failOpen;

    public GuardrailsAiClient(WebClient.Builder builder, GuardrailsProperties props) {
        this.webClient = builder
                .baseUrl(props.getGuardrailsAi().getBaseUrl())
                .build();
        this.failOpen = props.getGuardrailsAi().isFailOpen();
    }

    /**
     * Validates the LLM-generated output against Guardrails AI validators.
     *
     * @param output the generated text to validate
     * @return {@link GuardrailResult#allowed()} when validation passes,
     *         {@link GuardrailResult#blocked(String)} when a validator fires
     */
    public GuardrailResult validateOutput(String output) {
        try {
            GuardrailsAiResponse response = webClient.post()
                    .uri("/guards/{id}/validate", GUARD_ID)
                    .bodyValue(Map.of("llmOutput", output))
                    .retrieve()
                    .bodyToMono(GuardrailsAiResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            return toGuardrailResult(response);
        } catch (WebClientResponseException e) {
            log.warn("Guardrails AI returned HTTP {}, failing open", e.getStatusCode());
            return failOpenResult();
        } catch (Exception e) {
            log.warn("Guardrails AI unreachable ({}), failing open", e.getMessage());
            return failOpenResult();
        }
    }

    private GuardrailResult toGuardrailResult(GuardrailsAiResponse response) {
        if (response == null) {
            return failOpenResult();
        }
        if (!Boolean.TRUE.equals(response.validationPassed())) {
            String reason = response.error() != null ? response.error() : "validation-failed";
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
    record GuardrailsAiResponse(Boolean validationPassed, String error, String validatedOutput) {}
}
