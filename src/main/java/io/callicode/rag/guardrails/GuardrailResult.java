package io.callicode.rag.guardrails;

/**
 * Result of a guardrail check — either allowed or blocked with a reason.
 *
 * <p>Named factory methods make call-sites readable:
 * {@code GuardrailResult.allowed()} and {@code GuardrailResult.blocked("reason")}.
 */
public final class GuardrailResult {

    private final boolean allowed;
    private final String reason;

    private GuardrailResult(boolean allowed, String reason) {
        this.allowed = allowed;
        this.reason  = reason;
    }

    public static GuardrailResult allowed() {
        return new GuardrailResult(true, null);
    }

    public static GuardrailResult blocked(String reason) {
        return new GuardrailResult(false, reason);
    }

    public boolean isAllowed()  { return allowed; }
    public String  reason()     { return reason; }
    public boolean isBlocked()  { return !allowed; }
}
