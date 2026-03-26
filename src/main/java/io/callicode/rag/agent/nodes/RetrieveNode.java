package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fan-out trigger node.
 * <p>
 * Acts as the entry point for the parallel hybrid retrieval phase.
 * Two edges lead from this node to {@link SemanticSearchNode} and
 * {@link KeywordSearchNode} simultaneously — demonstrating LangGraph4j's
 * fork capability (impossible in LangChain which is DAG-only).
 * </p>
 * This node itself is a pass-through; its value is the branching structure.
 */
@Slf4j
@Component
public class RetrieveNode {

    public Map<String, Object> process(SelfRagState state) {
        log.info("Starting hybrid retrieval for: {}", state.effectiveQuery());
        // Pass-through — the graph wiring fans out to semanticSearch and keywordSearch
        return Map.of();
    }
}
