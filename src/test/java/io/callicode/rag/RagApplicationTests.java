package io.callicode.rag;

import io.callicode.rag.cache.SemanticCacheService;
import io.callicode.rag.guardrails.GuardrailsAiClient;
import io.callicode.rag.guardrails.NemoGuardrailsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

@SpringBootTest(properties = "app.seeder.enabled=false")
class RagApplicationTests {

    // Mock all external integration points so the context loads without
    // a running Ollama, Qdrant, or Valkey instance during CI/test
    @MockBean VectorStore vectorStore;
    @MockBean SemanticCacheService semanticCacheService;
    @MockBean NemoGuardrailsClient nemoGuardrailsClient;
    @MockBean GuardrailsAiClient guardrailsAiClient;
    @MockBean ChatClient.Builder chatClientBuilder;

    @Test
    void contextLoads() {
        // Passes if the Spring context starts without errors
    }
}
