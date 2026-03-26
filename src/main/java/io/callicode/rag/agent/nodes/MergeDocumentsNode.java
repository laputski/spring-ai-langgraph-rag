package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.agent.SerializableDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Join node — merges results from {@link SemanticSearchNode} and {@link KeywordSearchNode}.
 * <p>
 * LangGraph4j triggers this node only after BOTH upstream nodes have written their
 * results to the state. This is the join side of the fork/join parallel execution
 * pattern — a key differentiator from LangChain's sequential chain model.
 * </p>
 * Deduplication is performed by document ID; documents appearing in both sets are
 * kept once (semantic result preferred for metadata).
 */
@Slf4j
@Component
public class MergeDocumentsNode {

    public Map<String, Object> process(SelfRagState state) {
        List<SerializableDocument> semantic = state.semanticDocs();
        List<SerializableDocument> keyword  = state.keywordDocs();

        // Deduplicate by document ID, preserving insertion order
        Map<String, SerializableDocument> deduped = new LinkedHashMap<>();
        for (SerializableDocument doc : semantic) {
            deduped.put(doc.getId(), doc);
        }
        for (SerializableDocument doc : keyword) {
            deduped.putIfAbsent(doc.getId(), doc);
        }

        List<SerializableDocument> merged = new ArrayList<>(deduped.values());
        log.info("Merged {} semantic + {} keyword → {} unique documents",
                semantic.size(), keyword.size(), merged.size());

        return Map.of(SelfRagState.DOCUMENTS, merged);
    }
}
