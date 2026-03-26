package io.callicode.rag.agent;

import io.callicode.rag.agent.nodes.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Self-RAG agent graph built with LangGraph4j.
 *
 * <h2>Why LangGraph4j over simple LangChain-style chains?</h2>
 * <ol>
 *   <li><b>Cycle</b>: {@code rewriteQuery → retrieve} loop for iterative retrieval
 *       improvement — impossible in DAG-only chains.</li>
 *   <li><b>Fork/Join (parallel)</b>: {@code retrieve} fans out to {@code semanticSearch}
 *       and {@code keywordSearch} simultaneously; {@code mergeDocuments} joins both
 *       results before continuing.</li>
 *   <li><b>Multi-exit conditional routing</b>: 3 different terminal paths based on
 *       runtime state — input blocked, cache hit, or normal completion.</li>
 *   <li><b>Cross-node state persistence</b>: {@code retryCount} and other fields
 *       survive across cycle iterations without external coordination.</li>
 * </ol>
 *
 * <h2>Graph topology</h2>
 * <pre>
 * START → inputGuardrail ──(blocked)──────────────────────────────► END
 *               │
 *           (allowed)
 *               ▼
 *       checkSemanticCache ──(cache hit)────────────────────────────► END
 *               │
 *           (miss)
 *               ▼
 *           retrieve ──┬──► semanticSearch ──┐
 *                      └──► keywordSearch ───► mergeDocuments ──► gradeDocuments
 *                                                                       │
 *                                                  (relevant docs)      │  (not relevant, retry&lt;2)
 *                                                                       │          │
 *                                                                  generate    rewriteQuery
 *                                                                       │          │
 *                                                                       │     [back to retrieve ↑]
 *                                                                       ▼
 *                                                              outputGuardrail ──(blocked)──► END
 *                                                                       │
 *                                                                   (allowed)
 *                                                                       ▼
 *                                                                  cacheResult ─────────────► END
 * </pre>
 */
@Slf4j
@Component
public class SelfRagGraph {

    private final InputGuardrailNode     inputGuardrailNode;
    private final SemanticCacheCheckNode cacheCheckNode;
    private final RetrieveNode           retrieveNode;
    private final SemanticSearchNode     semanticSearchNode;
    private final KeywordSearchNode      keywordSearchNode;
    private final MergeDocumentsNode     mergeDocumentsNode;
    private final GradeDocumentsNode     gradeDocumentsNode;
    private final GenerateNode           generateNode;
    private final OutputGuardrailNode    outputGuardrailNode;
    private final RewriteQueryNode       rewriteQueryNode;
    private final CacheResultNode        cacheResultNode;

    @Getter
    private CompiledGraph<SelfRagState> compiledGraph;

    public SelfRagGraph(
            InputGuardrailNode inputGuardrailNode,
            SemanticCacheCheckNode cacheCheckNode,
            RetrieveNode retrieveNode,
            SemanticSearchNode semanticSearchNode,
            KeywordSearchNode keywordSearchNode,
            MergeDocumentsNode mergeDocumentsNode,
            GradeDocumentsNode gradeDocumentsNode,
            GenerateNode generateNode,
            OutputGuardrailNode outputGuardrailNode,
            RewriteQueryNode rewriteQueryNode,
            CacheResultNode cacheResultNode) {
        this.inputGuardrailNode  = inputGuardrailNode;
        this.cacheCheckNode      = cacheCheckNode;
        this.retrieveNode        = retrieveNode;
        this.semanticSearchNode  = semanticSearchNode;
        this.keywordSearchNode   = keywordSearchNode;
        this.mergeDocumentsNode  = mergeDocumentsNode;
        this.gradeDocumentsNode  = gradeDocumentsNode;
        this.generateNode        = generateNode;
        this.outputGuardrailNode = outputGuardrailNode;
        this.rewriteQueryNode    = rewriteQueryNode;
        this.cacheResultNode     = cacheResultNode;
    }

    @PostConstruct
    public void build() throws Exception {
        log.info("Compiling Self-RAG graph...");

        this.compiledGraph = new StateGraph<>(SelfRagState.SCHEMA, SelfRagState::new)

                // ── Nodes ────────────────────────────────────────────────────────
                .addNode("inputGuardrail",     node_async(inputGuardrailNode::process))
                .addNode("checkSemanticCache", node_async(cacheCheckNode::process))
                .addNode("retrieve",           node_async(retrieveNode::process))

                // Fork: parallel hybrid retrieval
                .addNode("semanticSearch",     node_async(semanticSearchNode::process))
                .addNode("keywordSearch",      node_async(keywordSearchNode::process))

                // Join: merge after both search nodes complete
                .addNode("mergeDocuments",     node_async(mergeDocumentsNode::process))

                .addNode("gradeDocuments",     node_async(gradeDocumentsNode::process))
                .addNode("generate",           node_async(generateNode::process))
                .addNode("outputGuardrail",    node_async(outputGuardrailNode::process))
                .addNode("rewriteQuery",       node_async(rewriteQueryNode::process))
                .addNode("cacheResult",        node_async(cacheResultNode::process))

                // ── Edges ────────────────────────────────────────────────────────
                .addEdge(START, "inputGuardrail")

                .addConditionalEdges("inputGuardrail",
                        edge_async(s -> s.isBlocked() ? "blocked" : "check"),
                        Map.of("blocked", END, "check", "checkSemanticCache"))

                .addConditionalEdges("checkSemanticCache",
                        edge_async(s -> s.isCacheHit() ? "end" : "retrieve"),
                        Map.of("end", END, "retrieve", "retrieve"))

                // Fork: retrieve → semanticSearch AND keywordSearch (parallel)
                .addEdge("retrieve", "semanticSearch")
                .addEdge("retrieve", "keywordSearch")

                // Join: LangGraph4j fires mergeDocuments only after BOTH deliver their updates
                .addEdge("semanticSearch", "mergeDocuments")
                .addEdge("keywordSearch",  "mergeDocuments")

                .addEdge("mergeDocuments", "gradeDocuments")

                .addConditionalEdges("gradeDocuments",
                        edge_async(s -> {
                            boolean hasRelevant = s.documents().stream()
                                    .anyMatch(d -> Boolean.TRUE.equals(d.getMetadata().get("relevant")));
                            // Generate if docs are relevant OR we've exhausted retries
                            return (hasRelevant || s.retryCount() >= 2) ? "generate" : "rewrite";
                        }),
                        Map.of("generate", "generate", "rewrite", "rewriteQuery"))

                // Cycle: rewriteQuery loops back to retrieve (terminated by retryCount >= 2)
                .addEdge("rewriteQuery", "retrieve")

                .addEdge("generate", "outputGuardrail")

                .addConditionalEdges("outputGuardrail",
                        edge_async(s -> s.isBlocked() ? "blocked" : "cache"),
                        Map.of("blocked", END, "cache", "cacheResult"))

                .addEdge("cacheResult", END)

                .compile();

        log.info("Self-RAG graph compiled successfully");
    }

}
