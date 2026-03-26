package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.agent.SerializableDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grades each retrieved document for relevance to the query.
 * <p>
 * Sends a YES/NO prompt to the LLM for each document. Documents are annotated
 * with {@code metadata.relevant = true/false}. Relevant documents propagate to
 * the {@code generate} node; irrelevant sets trigger the {@code rewriteQuery} path.
 * </p>
 */
@Slf4j
@Component
public class GradeDocumentsNode {

    private static final String GRADE_PROMPT = """
            Is the following document relevant to answering the question?
            Answer with only YES or NO — no explanation.

            Question: %s

            Document: %s
            """;

    private final ChatClient.Builder builder;

    public GradeDocumentsNode(ChatClient.Builder builder) {
        this.builder = builder;
    }

    public Map<String, Object> process(SelfRagState state) {
        List<SerializableDocument> docs = state.documents();
        String query = state.effectiveQuery();
        log.info("Grading {} document(s) for query: {}", docs.size(), query);

        List<SerializableDocument> graded = docs.stream()
                .map(doc -> grade(query, doc))
                .toList();

        long relevant = graded.stream()
                .filter(d -> Boolean.TRUE.equals(d.getMetadata().get("relevant")))
                .count();
        log.info("{}/{} documents are relevant", relevant, graded.size());

        return Map.of(SelfRagState.DOCUMENTS, graded);
    }

    private SerializableDocument grade(String query, SerializableDocument doc) {
        String prompt = GRADE_PROMPT.formatted(query, doc.getText());
        String response = builder.build().prompt(prompt).call().content();
        boolean relevant = response != null && response.trim().toUpperCase().startsWith("YES");

        HashMap<String, Object> meta = new HashMap<>(doc.getMetadata());
        meta.put("relevant", relevant);
        return new SerializableDocument(doc.getId(), doc.getText(), meta);
    }
}
