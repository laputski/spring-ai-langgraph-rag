package io.callicode.rag.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.Set;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SemanticCacheServiceTest {

    @Mock
    private RedisTemplate<String, CacheEntry> redisTemplate;
    @Mock
    private ValueOperations<String, CacheEntry> valueOps;
    @Mock
    private EmbeddingModel embeddingModel;

    private SemanticCacheService cacheService;

    // Fixed embedding vectors for testing cosine similarity
    // Two identical vectors → similarity = 1.0 (above any threshold)
    private static final float[] QUERY_EMBEDDING   = {1.0f, 0.0f, 0.0f};
    // Similar vector → cosine sim ≈ 0.95
    private static final float[] SIMILAR_EMBEDDING = {0.95f, 0.31f, 0.0f};
    // Different vector → cosine sim = 0.0
    private static final float[] DIFF_EMBEDDING    = {0.0f, 1.0f, 0.0f};

    @BeforeEach
    void setUp() {
        CacheProperties props = new CacheProperties();
        props.setSimilarityThreshold(0.92);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        cacheService = new SemanticCacheService(redisTemplate, embeddingModel, props);
    }

    @Test
    void get_returnsCachedAnswer_whenSimilarityAboveThreshold() {
        String storedKey = "semantic-cache:stored-key";
        CacheEntry entry = new CacheEntry("What is Spring Boot?", SIMILAR_EMBEDDING, "Spring Boot is a framework.");

        when(embeddingModel.embed(anyString())).thenReturn(QUERY_EMBEDDING);
        when(redisTemplate.keys("semantic-cache:*")).thenReturn(Set.of(storedKey));
        when(valueOps.get(storedKey)).thenReturn(entry);

        Optional<String> result = cacheService.get("What is Spring Boot?");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("Spring Boot is a framework.");
    }

    @Test
    void get_returnsEmpty_whenSimilarityBelowThreshold() {
        String storedKey = "semantic-cache:stored-key";
        CacheEntry entry = new CacheEntry("What is Docker?", DIFF_EMBEDDING, "Docker is a container platform.");

        when(embeddingModel.embed(anyString())).thenReturn(QUERY_EMBEDDING);
        when(redisTemplate.keys("semantic-cache:*")).thenReturn(Set.of(storedKey));
        when(valueOps.get(storedKey)).thenReturn(entry);

        Optional<String> result = cacheService.get("What is Spring Boot?");

        assertThat(result).isEmpty();
    }

    @Test
    void get_returnsEmpty_whenCacheIsEmpty() {
        when(embeddingModel.embed(anyString())).thenReturn(QUERY_EMBEDDING);
        when(redisTemplate.keys("semantic-cache:*")).thenReturn(Set.of());

        Optional<String> result = cacheService.get("What is Kubernetes?");

        assertThat(result).isEmpty();
    }
}
