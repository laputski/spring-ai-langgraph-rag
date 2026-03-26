package io.callicode.rag.guardrails;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "guardrails")
public class GuardrailsProperties {

    private Sidecar nemo = new Sidecar();
    private Sidecar guardrailsAi = new Sidecar();

    @Getter
    @Setter
    public static class Sidecar {
        private String baseUrl = "http://localhost:8001";
        private boolean failOpen = true;
    }
}
