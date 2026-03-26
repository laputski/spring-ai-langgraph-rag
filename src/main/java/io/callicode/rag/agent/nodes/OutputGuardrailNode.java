package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.guardrails.GuardrailResult;
import io.callicode.rag.guardrails.GuardrailsAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Validates the generated answer against Guardrails AI output validators.
 * <p>
 * Runs after {@link GenerateNode}. Validators include toxic language detection
 * and secrets presence checks. If validation fails, the state is marked blocked
 * and the graph exits without caching the result.
 * </p>
 */
@Slf4j
@Component
public class OutputGuardrailNode {

    private final GuardrailsAiClient guardrailsAiClient;

    public OutputGuardrailNode(GuardrailsAiClient guardrailsAiClient) {
        this.guardrailsAiClient = guardrailsAiClient;
    }

    public Map<String, Object> process(SelfRagState state) {
        String answer = state.answer();
        log.info("Output guardrail check on generated answer");

        GuardrailResult result = guardrailsAiClient.validateOutput(answer);

        if (result.isBlocked()) {
            log.warn("Answer blocked by Guardrails AI: {}", result.reason());
            return Map.of(
                    SelfRagState.BLOCKED, true,
                    SelfRagState.BLOCK_REASON, result.reason() != null ? result.reason() : "output-validation-failed",
                    SelfRagState.ANSWER, ""
            );
        }

        return Map.of(SelfRagState.BLOCKED, false);
    }
}
