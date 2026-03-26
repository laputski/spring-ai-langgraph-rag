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
 * Performs keyword-biased retrieval to complement semantic search.
 * <p>
 * Uses the same VectorStore with a higher similarity threshold but lower k,
 * providing more precise (less broad) results. Combined with
 * {@link SemanticSearchNode} results in {@link MergeDocumentsNode} for hybrid retrieval.
 * </p>
 * Note: Qdrant's native sparse vector support (BM25) can replace this node
 * when the collection is configured with named vectors — a simple extension point.
 */
@Slf4j
@Component
public class KeywordSearchNode {

    private static final int TOP_K = 3;
    private static final double SIMILARITY_THRESHOLD = 0.75;

    private final VectorStore vectorStore;

    public KeywordSearchNode(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Map<String, Object> process(SelfRagState state) {
        String query = state.effectiveQuery();
        log.debug("Keyword search for: {}", query);

        List<SerializableDocument> docs = vectorStore
                .similaritySearch(SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .stream()
                .map(SerializableDocument::from)
                .toList();

        log.info("Keyword search returned {} documents", docs.size());
        return Map.of(SelfRagState.KEYWORD_DOCS, docs);
    }
}
