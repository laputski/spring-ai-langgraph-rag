package io.callicode.rag.guardrails;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "guardrails")
public class GuardrailsProperties {

    private Sidecar nemo = new Sidecar();
    private Sidecar guardrailsAi = new Sidecar();

    public Sidecar getNemo() { return nemo; }
    public void setNemo(Sidecar nemo) { this.nemo = nemo; }

    public Sidecar getGuardrailsAi() { return guardrailsAi; }
    public void setGuardrailsAi(Sidecar guardrailsAi) { this.guardrailsAi = guardrailsAi; }

    public static class Sidecar {
        private String baseUrl = "http://localhost:8001";
        private boolean failOpen = true;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public boolean isFailOpen() { return failOpen; }
        public void setFailOpen(boolean failOpen) { this.failOpen = failOpen; }
    }
}
