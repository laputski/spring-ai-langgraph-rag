package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Rewrites the user query to improve retrieval recall.
 * <p>
 * Called when {@code gradeDocuments} finds no relevant documents. Asks the LLM
 * to reformulate the query to be more specific or use different vocabulary.
 * Always increments {@code retryCount} — the conditional edge in {@link io.callicode.rag.agent.SelfRagGraph}
 * checks {@code retryCount >= 2} to break the retrieval cycle.
 * </p>
 */
@Slf4j
@Component
public class RewriteQueryNode {

    private static final String REWRITE_PROMPT = """
            The following query did not return relevant documents from the knowledge base.
            Rewrite it to be more specific, use different technical vocabulary, or decompose it.
            Return only the rewritten query — no explanation.

            Original query: %s
            """;

    private final ChatClient.Builder builder;

    public RewriteQueryNode(ChatClient.Builder builder) {
        this.builder = builder;
    }

    public Map<String, Object> process(SelfRagState state) {
        String original = state.effectiveQuery();
        log.info("Rewriting query (attempt {}): {}", state.retryCount() + 1, original);

        String rewritten = builder.build().prompt(REWRITE_PROMPT.formatted(original)).call().content();
        if (rewritten != null) rewritten = rewritten.trim();

        log.info("Rewritten query: {}", rewritten);

        return Map.of(
                SelfRagState.REWRITTEN_QUERY, rewritten != null ? rewritten : original,
                SelfRagState.RETRY_COUNT, state.retryCount() + 1
        );
    }
}
