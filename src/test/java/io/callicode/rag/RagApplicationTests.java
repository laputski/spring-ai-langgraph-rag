package io.callicode.rag;

import io.callicode.rag.cache.SemanticCacheService;
import io.callicode.rag.guardrails.GuardrailsAiClient;
import io.callicode.rag.guardrails.NemoGuardrailsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest(properties = "app.seeder.enabled=false")
@EnableAutoConfiguration(exclude = ChatClientAutoConfiguration.class)
class RagApplicationTests {

    // Mock all external integration points so the context loads without
    // a running Ollama, Qdrant, or Valkey instance during CI/test
    @MockitoBean VectorStore vectorStore;
    @MockitoBean SemanticCacheService semanticCacheService;
    @MockitoBean NemoGuardrailsClient nemoGuardrailsClient;
    @MockitoBean GuardrailsAiClient guardrailsAiClient;
    @MockitoBean(enforceOverride = false) ChatClient.Builder chatClientBuilder;

    @Test
    void contextLoads() {
        // Passes if the Spring context starts without errors
    }
}
