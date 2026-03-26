FROM python:3.11-slim

WORKDIR /app

# annoy (required by nemoguardrails) compiles C++ extension — needs g++
RUN apt-get update && apt-get install -y --no-install-recommends \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Install NeMo Guardrails — no NGC account required
RUN pip install --no-cache-dir nemoguardrails==0.10.0

# Config is mounted at runtime via docker-compose volume
# Default command is overridden in docker-compose.yml
CMD ["nemoguardrails", "server", "--config", "/app/config", "--port", "8001"]
