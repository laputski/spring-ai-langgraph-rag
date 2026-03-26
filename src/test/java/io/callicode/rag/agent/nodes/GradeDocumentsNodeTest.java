package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import io.callicode.rag.agent.SerializableDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeDocumentsNodeTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callSpec;

    @Test
    void process_marksDocumentRelevant_whenLlmRespondsYes() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("YES");

        GradeDocumentsNode node = new GradeDocumentsNode(chatClientBuilder);

        SerializableDocument doc = new SerializableDocument("id1",
                "Spring Boot is a framework for building Java applications.", Map.of());
        SelfRagState state = new SelfRagState(Map.of(
                SelfRagState.QUERY, "What is Spring Boot?",
                SelfRagState.DOCUMENTS, List.of(doc)
        ));

        Map<String, Object> result = node.process(state);

        @SuppressWarnings("unchecked")
        List<SerializableDocument> gradedDocs = (List<SerializableDocument>) result.get(SelfRagState.DOCUMENTS);
        assertThat(gradedDocs).hasSize(1);
        assertThat(gradedDocs.getFirst().getMetadata()).containsEntry("relevant", true);
    }

    @Test
    void process_marksDocumentIrrelevant_whenLlmRespondsNo() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("NO");

        GradeDocumentsNode node = new GradeDocumentsNode(chatClientBuilder);

        SerializableDocument doc = new SerializableDocument("id2",
                "The weather in Paris is usually pleasant in spring.", Map.of());
        SelfRagState state = new SelfRagState(Map.of(
                SelfRagState.QUERY, "What is Spring Boot?",
                SelfRagState.DOCUMENTS, List.of(doc)
        ));

        Map<String, Object> result = node.process(state);

        @SuppressWarnings("unchecked")
        List<SerializableDocument> gradedDocs = (List<SerializableDocument>) result.get(SelfRagState.DOCUMENTS);
        assertThat(gradedDocs.getFirst().getMetadata()).containsEntry("relevant", false);
    }
}
