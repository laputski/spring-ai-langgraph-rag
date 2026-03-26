package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.cache.SemanticCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stores the validated answer in the semantic cache (Valkey) for future reuse.
 * <p>
 * Only reached when {@link OutputGuardrailNode} passes — ensuring we never
 * cache unsafe or invalid responses.
 * </p>
 */
@Slf4j
@Component
public class CacheResultNode {

    private final SemanticCacheService cacheService;

    public CacheResultNode(SemanticCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public Map<String, Object> process(SelfRagState state) {
        try {
            cacheService.put(state.query(), state.answer());
            log.debug("Answer cached for query: {}", state.query());
        } catch (Exception e) {
            // Non-fatal — caching failure should not prevent returning the answer
            log.warn("Failed to cache answer: {}", e.getMessage());
        }
        return Map.of();
    }
}
