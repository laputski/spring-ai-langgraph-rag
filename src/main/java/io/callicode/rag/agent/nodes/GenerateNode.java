package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.agent.SerializableDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates the final answer using the LLM with retrieved context (RAG).
 * <p>
 * Only relevant documents (as graded by {@link GradeDocumentsNode}) are included
 * in the context. If no relevant documents were found (max retries exceeded),
 * all available documents are used as fallback.
 * </p>
 */
@Slf4j
@Component
public class GenerateNode {

    private static final String GENERATE_PROMPT = """
            You are a helpful technical documentation assistant.
            Answer the question based ONLY on the provided context documents.
            If the context does not contain enough information, say so clearly.

            Context:
            %s

            Question: %s

            Answer:
            """;

    private final ChatClient.Builder builder;

    public GenerateNode(ChatClient.Builder builder) {
        this.builder = builder;
    }

    public Map<String, Object> process(SelfRagState state) {
        List<SerializableDocument> allDocs = state.documents();

        // Use only relevant docs; fall back to all if none are marked relevant
        List<SerializableDocument> contextDocs = allDocs.stream()
                .filter(d -> Boolean.TRUE.equals(d.getMetadata().get("relevant")))
                .toList();
        if (contextDocs.isEmpty()) {
            contextDocs = allDocs;
        }

        String context = contextDocs.stream()
                .map(SerializableDocument::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Always answer the original user question — effectiveQuery() may contain a
        // rewritten retrieval query that should never be shown back to the user.
        String query = state.query();
        log.info("Generating answer from {} document(s) for: {}", contextDocs.size(), query);

        String answer = builder.build().prompt(GENERATE_PROMPT.formatted(context, query)).call().content();

        return Map.of(SelfRagState.ANSWER, answer != null ? answer.trim() : "");
    }
}
