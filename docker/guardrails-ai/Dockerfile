FROM python:3.11-slim

WORKDIR /app

# Minimal dependencies: FastAPI + uvicorn only — no hub authentication required
RUN pip install --no-cache-dir fastapi "uvicorn[standard]" pydantic

COPY server.py .

# PORT env var controls the listen port; override via docker-compose environment if needed
CMD ["python", "server.py"]
