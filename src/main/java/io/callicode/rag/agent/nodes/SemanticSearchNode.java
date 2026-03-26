package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.agent.SerializableDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Performs semantic (embedding-based) similarity search against Qdrant.
 * <p>
 * Results are stored in the {@code semanticDocs} state channel. Together with
 * {@link KeywordSearchNode} this forms the fork side of the parallel hybrid retrieval.
 * {@link MergeDocumentsNode} joins the two channels before grading.
 * </p>
 */
@Slf4j
@Component
public class SemanticSearchNode {

    private static final int TOP_K = 5;

    private final VectorStore vectorStore;

    public SemanticSearchNode(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Map<String, Object> process(SelfRagState state) {
        String query = state.effectiveQuery();
        log.debug("Semantic search for: {}", query);

        List<SerializableDocument> docs = vectorStore
                .similaritySearch(SearchRequest.builder().query(query).topK(TOP_K).build())
                .stream()
                .map(SerializableDocument::from)
                .toList();

        log.info("Semantic search returned {} documents", docs.size());
        return Map.of(SelfRagState.SEMANTIC_DOCS, docs);
    }
}
