package io.callicode.rag.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Semantic cache backed by Valkey (Redis-protocol compatible).
 * <p>
 * Queries are embedded and compared against stored embeddings using cosine
 * similarity. A cache hit occurs when the highest similarity exceeds the
 * configured threshold (default 0.92).
 * </p>
 */
@Slf4j
@Service
public class SemanticCacheService {

    static final String KEY_PREFIX = "semantic-cache:";

    private final RedisTemplate<String, CacheEntry> redisTemplate;
    private final EmbeddingModel embeddingModel;
    private final CacheProperties props;

    public SemanticCacheService(
            RedisTemplate<String, CacheEntry> redisTemplate,
            EmbeddingModel embeddingModel,
            CacheProperties props) {
        this.redisTemplate = redisTemplate;
        this.embeddingModel = embeddingModel;
        this.props = props;
    }

    /**
     * Looks up a semantically similar cached answer for the given query.
     *
     * @param query the user query
     * @return the cached answer, or empty if no similar entry found
     */
    public Optional<String> get(String query) {
        float[] queryEmbedding = embeddingModel.embed(query);

        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys.isEmpty()) {
            return Optional.empty();
        }

        String bestKey = null;
        double bestSimilarity = 0.0;

        for (String key : keys) {
            CacheEntry entry = redisTemplate.opsForValue().get(key);
            if (entry == null || entry.getEmbedding() == null) continue;

            double sim = cosineSimilarity(queryEmbedding, entry.getEmbedding());
            if (sim > bestSimilarity) {
                bestSimilarity = sim;
                bestKey = key;
            }
        }

        if (bestSimilarity >= props.getSimilarityThreshold() && bestKey != null) {
            CacheEntry hit = redisTemplate.opsForValue().get(bestKey);
            if (hit != null) {
                log.debug("Semantic cache hit (similarity={}) for query: {}", String.format("%.3f", bestSimilarity), query);
                return Optional.of(hit.getAnswer());
            }
        }

        return Optional.empty();
    }

    /**
     * Stores a query-answer pair in the cache with its embedding.
     *
     * @param query  the original query
     * @param answer the generated answer to cache
     */
    public void put(String query, String answer) {
        float[] embedding = embeddingModel.embed(query);
        CacheEntry entry = new CacheEntry(query, embedding, answer);
        String key = KEY_PREFIX + Integer.toHexString(java.util.Arrays.hashCode(embedding));
        redisTemplate.opsForValue().set(key, entry);
        if (props.getTtl() != null) {
            redisTemplate.expire(key, props.getTtl());
        }
        log.debug("Cached answer for query: {}", query);
    }

    /**
     * Computes cosine similarity between two float vectors.
     */
    static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
