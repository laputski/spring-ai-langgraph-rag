package io.callicode.rag.agent;

import io.callicode.rag.api.dto.ChatRequest;
import io.callicode.rag.api.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.NodeOutput;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service layer that drives the Self-RAG {@link SelfRagGraph} for a single chat request.
 */
@Slf4j
@Service
public class SelfRagService {

    private final SelfRagGraph selfRagGraph;

    public SelfRagService(SelfRagGraph selfRagGraph) {
        this.selfRagGraph = selfRagGraph;
    }

    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.sessionId() != null
                ? request.sessionId()
                : UUID.randomUUID().toString();

        Map<String, Object> initial = Map.of(
                SelfRagState.QUERY,      request.query(),
                SelfRagState.SESSION_ID, sessionId
        );

        try {
            SelfRagState finalState = null;
            for (NodeOutput<SelfRagState> output : selfRagGraph.getCompiledGraph().stream(initial)) {
                finalState = output.state();
            }

            if (finalState == null || finalState.isBlocked()) {
                return new ChatResponse("", "BLOCKED", false);
            }
            if (finalState.isCacheHit()) {
                return new ChatResponse(finalState.answer(), "CACHE", true);
            }
            return new ChatResponse(finalState.answer(), "RAG", false);

        } catch (Exception e) {
            log.error("Self-RAG graph execution failed for query: {}", request.query(), e);
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }
}
