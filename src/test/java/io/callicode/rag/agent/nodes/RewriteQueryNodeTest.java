package io.callicode.rag.agent.nodes;

import io.callicode.rag.agent.SelfRagState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewriteQueryNodeTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callSpec;

    @Test
    void process_rewritesQuery_andIncrementsRetryCount() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("Spring Boot framework configuration and auto-configuration");

        RewriteQueryNode node = new RewriteQueryNode(chatClientBuilder);

        SelfRagState state = new SelfRagState(Map.of(
                SelfRagState.QUERY, "spring boot",
                SelfRagState.RETRY_COUNT, 0
        ));

        Map<String, Object> result = node.process(state);

        assertThat(result.get(SelfRagState.REWRITTEN_QUERY))
                .isEqualTo("Spring Boot framework configuration and auto-configuration");
        assertThat(result.get(SelfRagState.RETRY_COUNT)).isEqualTo(1);
    }

    @Test
    void process_incrementsRetryCountOnEachCall() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn("rewritten query");

        RewriteQueryNode node = new RewriteQueryNode(chatClientBuilder);

        SelfRagState state = new SelfRagState(Map.of(
                SelfRagState.QUERY, "some query",
                SelfRagState.RETRY_COUNT, 1
        ));

        Map<String, Object> result = node.process(state);

        assertThat(result.get(SelfRagState.RETRY_COUNT)).isEqualTo(2);
    }
}
