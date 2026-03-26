package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.guardrails.GuardrailResult;
import io.callicode.rag.guardrails.NemoGuardrailsClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * First node in the graph — checks the user query against NeMo Guardrails input rails.
 * <p>
 * If the query is blocked (off-topic, jailbreak attempt, etc.) the state is marked
 * with {@code blocked=true} and the graph exits immediately via a conditional edge.
 * </p>
 */
@Slf4j
@Component
public class InputGuardrailNode {

    private final NemoGuardrailsClient nemoGuardrailsClient;

    public InputGuardrailNode(NemoGuardrailsClient nemoGuardrailsClient) {
        this.nemoGuardrailsClient = nemoGuardrailsClient;
    }

    public Map<String, Object> process(SelfRagState state) {
        String query = state.query();
        log.info("Input guardrail check for query: {}", query);

        GuardrailResult result = nemoGuardrailsClient.checkInput(query);

        if (result.isBlocked()) {
            log.warn("Query blocked by NeMo Guardrails: {}", result.reason());
            return Map.of(
                    SelfRagState.BLOCKED, true,
                    SelfRagState.BLOCK_REASON, result.reason() != null ? result.reason() : "blocked-by-guardrail"
            );
        }

        return Map.of(SelfRagState.BLOCKED, false);
    }
}
