from guardrails import Guard

# Output safety guard for the Self-RAG pipeline.
#
# Validators (toxic_language, secrets_present) are installed from Guardrails Hub
# and require a valid API token (run `guardrails configure` with your token from
# https://guardrailsai.com/hub/keys).
#
# Without hub authentication the validator hub install steps in the Dockerfile are
# skipped (they run with `|| true`), and this guard acts as a pass-through.
# The Spring Boot application is configured fail-open, so validation failures are
# handled gracefully and do not block responses.
guard = Guard(name="output-safety-guard")
