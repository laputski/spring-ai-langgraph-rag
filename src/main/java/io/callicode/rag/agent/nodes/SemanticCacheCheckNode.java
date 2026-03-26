package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.cache.SemanticCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Checks the semantic cache (Valkey) for a previously computed answer
 * to a semantically similar query.
 * <p>
 * On a cache hit the answer is written directly to state and the graph
 * exits early — bypassing all retrieval and generation nodes.
 * </p>
 */
@Slf4j
@Component
public class SemanticCacheCheckNode {

    private final SemanticCacheService cacheService;

    public SemanticCacheCheckNode(SemanticCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public Map<String, Object> process(SelfRagState state) {
        String query = state.query();
        log.debug("Checking semantic cache for: {}", query);

        Optional<String> cached = cacheService.get(query);

        if (cached.isPresent()) {
            log.info("Semantic cache hit for: {}", query);
            return Map.of(
                    SelfRagState.CACHE_HIT, true,
                    SelfRagState.ANSWER, cached.get()
            );
        }

        return Map.of(SelfRagState.CACHE_HIT, false);
    }
}
