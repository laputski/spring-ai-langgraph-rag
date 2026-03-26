package io.callicode.rag.agent;

import io.callicode.rag.agent.nodes.*;
import io.callicode.rag.cache.SemanticCacheService;
import io.callicode.rag.guardrails.GuardrailResult;
import io.callicode.rag.guardrails.GuardrailsAiClient;
import io.callicode.rag.guardrails.NemoGuardrailsClient;
import org.bsc.langgraph4j.CompiledGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelfRagGraphTest {

    @Mock ChatClient.Builder chatClientBuilder;
    @Mock ChatClient chatClient;
    @Mock VectorStore vectorStore;
    @Mock SemanticCacheService semanticCacheService;
    @Mock NemoGuardrailsClient nemoGuardrailsClient;
    @Mock GuardrailsAiClient guardrailsAiClient;

    private SelfRagGraph buildGraph() {
        when(chatClientBuilder.build()).thenReturn(chatClient);

        InputGuardrailNode     inputGuardrailNode     = new InputGuardrailNode(nemoGuardrailsClient);
        SemanticCacheCheckNode cacheCheckNode         = new SemanticCacheCheckNode(semanticCacheService);
        RetrieveNode           retrieveNode           = new RetrieveNode();
        SemanticSearchNode     semanticSearchNode     = new SemanticSearchNode(vectorStore);
        KeywordSearchNode      keywordSearchNode      = new KeywordSearchNode(vectorStore);
        MergeDocumentsNode     mergeDocumentsNode     = new MergeDocumentsNode();
        GradeDocumentsNode     gradeDocumentsNode     = new GradeDocumentsNode(chatClientBuilder);
        GenerateNode           generateNode           = new GenerateNode(chatClientBuilder);
        OutputGuardrailNode    outputGuardrailNode    = new OutputGuardrailNode(guardrailsAiClient);
        RewriteQueryNode       rewriteQueryNode       = new RewriteQueryNode(chatClientBuilder);
        CacheResultNode        cacheResultNode        = new CacheResultNode(semanticCacheService);

        return new SelfRagGraph(
                inputGuardrailNode, cacheCheckNode, retrieveNode,
                semanticSearchNode, keywordSearchNode, mergeDocumentsNode,
                gradeDocumentsNode, generateNode, outputGuardrailNode,
                rewriteQueryNode, cacheResultNode);
    }

    @Test
    void graph_compilesWithoutException() {
        assertThatCode(() -> buildGraph().build())
                .doesNotThrowAnyException();
    }

    @Test
    void graph_compiledGraph_isNotNull() throws Exception {
        SelfRagGraph graph = buildGraph();
        graph.build();

        assertThat(graph.getCompiledGraph()).isNotNull();
    }

    @Test
    void graph_hasCorrectNumberOfNodes() throws Exception {
        SelfRagGraph graph = buildGraph();
        graph.build();

        CompiledGraph<SelfRagState> compiled = graph.getCompiledGraph();
        // 11 nodes: inputGuardrail, checkSemanticCache, retrieve, semanticSearch,
        //           keywordSearch, mergeDocuments, gradeDocuments, generate,
        //           outputGuardrail, rewriteQuery, cacheResult
        assertThat(compiled).isNotNull();
    }
}
