"""
Minimal Guardrails AI-compatible validation server.

Implements the REST contract that GuardrailsAiClient expects:
  POST /guards/{guard_id}/validate
  GET  /health

Extend the _validate() function below to add real validation logic,
or configure validators via environment variables.
"""

import os
import re
import logging

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("guardrails-server")

app = FastAPI(title="Guardrails AI Validation Server")


# ── Request / Response models ────────────────────────────────────────────────

class ValidateRequest(BaseModel):
    llmOutput: str = ""


class ValidateResponse(BaseModel):
    validationPassed: bool
    validatedOutput: str | None = None
    error: str | None = None


# ── Validation logic ─────────────────────────────────────────────────────────

# Patterns for simple, dependency-free checks
_SECRET_PATTERNS = [
    re.compile(r"sk-[A-Za-z0-9]{20,}"),          # OpenAI-style key
    re.compile(r"(?i)api[_\-]?key\s*[:=]\s*\S+"), # generic API key
    re.compile(r"(?i)password\s*[:=]\s*\S+"),     # password assignment
    re.compile(r"AKIA[0-9A-Z]{16}"),              # AWS access key
]

_PROFANITY_WORDS = {
    w.strip() for w in os.environ.get("BLOCKED_WORDS", "").split(",") if w.strip()
}


def _validate(guard_id: str, text: str) -> ValidateResponse:
    """
    Apply checks appropriate for the given guard.
    Returns ValidateResponse with validationPassed=False and an error code if blocked.
    """
    # -- secrets check (all guards) --
    for pattern in _SECRET_PATTERNS:
        if pattern.search(text):
            log.warning("[%s] secrets_present detected", guard_id)
            return ValidateResponse(validationPassed=False, error="secrets_present")

    # -- configurable blocked words (toxic_language proxy) --
    lower = text.lower()
    for word in _PROFANITY_WORDS:
        if word in lower:
            log.warning("[%s] toxic_language detected (word: %s)", guard_id, word)
            return ValidateResponse(validationPassed=False, error="toxic_language")

    return ValidateResponse(validationPassed=True, validatedOutput=text)


# ── Routes ───────────────────────────────────────────────────────────────────

@app.post("/guards/{guard_id}/validate", response_model=ValidateResponse)
def validate(guard_id: str, request: ValidateRequest):
    log.info("Validating output for guard '%s' (%d chars)", guard_id, len(request.llmOutput))
    return _validate(guard_id, request.llmOutput)


@app.get("/health")
def health():
    return {"status": "ok"}


# ── Entry point ──────────────────────────────────────────────────────────────

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")
