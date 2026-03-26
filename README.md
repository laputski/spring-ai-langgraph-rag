# Spring AI + LangGraph4j Self-RAG Starter

A best-practice starter project demonstrating a Self-RAG AI agent built with **Spring AI**, **LangGraph4j**, and guardrails integration. Designed for Java developers learning production-ready patterns for LLM-powered applications.

## Overview

This project implements a **Self-Reflective RAG** (Retrieval-Augmented Generation) agent that:

1. Validates user input via **NeMo Guardrails** (jailbreak/off-topic detection)
2. Checks a **semantic cache** (Valkey) for previously computed answers
3. Performs **hybrid parallel retrieval** — semantic search and keyword search simultaneously
4. **Grades retrieved documents** for relevance, rewriting the query and retrying if needed
5. Generates an answer using **Ollama** (local LLM) with retrieved context
6. Validates the output via **Guardrails AI** (toxicity/secrets detection)
7. Caches the validated answer for future reuse

## Why LangGraph4j?

This project uses LangGraph4j to demonstrate graph-based agent workflows that are **impossible or awkward** with LangChain-style sequential chains:

| Feature | LangChain chains | LangGraph4j |
|---|---|---|
| **Cycles / retry loops** | ❌ DAG only | ✅ `rewriteQuery → retrieve` cycle |
| **Parallel execution (fork/join)** | ❌ Sequential | ✅ `semanticSearch` ‖ `keywordSearch` |
| **Multi-exit conditional routing** | ❌ Limited | ✅ 3 terminal paths based on runtime state |
| **Cross-node state persistence** | ❌ Manual | ✅ `retryCount`, `blocked` survive cycles |

## Architecture

```
START
  │
  ▼
inputGuardrail ──(blocked)────────────────────────────────────────► END
  │
(allowed)
  ▼
checkSemanticCache ──(cache hit)──────────────────────────────────► END
  │
(miss)
  ▼
retrieve ──┬──► semanticSearch ──┐
           │                      ▼
           └──► keywordSearch ──► mergeDocuments ──► gradeDocuments
                                                          │
                                         (relevant docs)  │  (not relevant, retry<2)
                                                          │          │
                                                     generate    rewriteQuery
                                                          │          │
                                                          │     [back to retrieve ↑]
                                                          ▼
                                                   outputGuardrail ──(blocked)──► END
                                                          │
                                                      (allowed)
                                                          ▼
                                                     cacheResult ──────────────► END
```

## Technology Stack

| Component | Technology | Version |
|---|---|---|
| Framework | Spring Boot | 3.5.3 |
| AI / LLM | Spring AI + Ollama | 1.1.3 |
| Agent Graph | LangGraph4j | 1.5.14 |
| Vector Store | Qdrant | v1.13.6 |
| Semantic Cache | Valkey (Redis-compatible) | 8.0 |
| Input Guardrails | NVIDIA NeMo Guardrails | 0.10.0 |
| Output Guardrails | Guardrails AI | latest |
| Language | Java | 21 |

## Prerequisites

- **Java 21+** — `java -version`
- **Docker + Docker Compose** — `docker compose version`
- **Ollama** — [install](https://ollama.com/download), then:

```bash
ollama serve          # start Ollama (if not already running as a service)
ollama pull llama3.2
ollama pull nomic-embed-text
```

## Quick Start

```bash
# 1. Start infrastructure (Qdrant, Valkey, NeMo Guardrails, Guardrails AI)
docker compose up -d

# 2. Build and run the application
./gradlew bootRun
```

On startup, the application automatically seeds 5 sample tech documents into Qdrant.

## API Reference

### POST /api/v1/chat

Ask a question to the RAG agent.

**Request:**
```json
{
  "query": "What is Spring Boot auto-configuration?",
  "sessionId": "optional-session-id"
}
```

**Response:**
```json
{
  "answer": "Spring Boot auto-configuration automatically configures...",
  "source": "RAG",
  "cached": false
}
```

`source` values: `RAG` (fresh retrieval), `CACHE` (semantic cache hit), `BLOCKED` (guardrail triggered)

### POST /api/v1/documents

Ingest custom documents into the vector store.

**Request:**
```json
{
  "texts": [
    "Your custom documentation text here...",
    "Another document..."
  ]
}
```

### GET /actuator/health

Application and infrastructure health status.

## Configuration

Key settings in `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `spring.ai.ollama.chat.options.model` | `llama3.2` | Chat model |
| `spring.ai.ollama.embedding.options.model` | `nomic-embed-text` | Embedding model |
| `spring.ai.vectorstore.qdrant.dimensions` | `768` | Must match embedding model output |
| `cache.similarity-threshold` | `0.92` | Cosine similarity threshold for cache hits |
| `cache.ttl` | `PT1H` | Cache entry TTL (ISO 8601 duration) |
| `guardrails.nemo.fail-open` | `true` | Allow requests if NeMo is unreachable |
| `guardrails.guardrails-ai.fail-open` | `true` | Allow responses if Guardrails AI is unreachable |
| `app.seeder.enabled` | `true` | Seed sample docs on startup |

## How Guardrails Integrate

```
User Query
    │
    ▼
NeMo Guardrails (Python sidecar, port 8001)
    │ OpenAI-compatible REST API
    │ Colang flows: jailbreak detection, off-topic filtering
    │
    ▼ (allowed)
[... RAG pipeline ...]
    │
    ▼
Generated Answer
    │
    ▼
Guardrails AI (Python sidecar, port 8000)
    │ REST API: POST /guards/output-safety-guard/validate
    │ Validators: toxic_language, secrets_present
    │
    ▼ (passed)
Cache + Return
```

Both sidecars use **fail-open** strategy by default — if a sidecar is unreachable, the request proceeds normally. Set `fail-open: false` to enforce guardrails strictly.

### Customizing NeMo Rails

Edit `config/nemo-guardrails/main.co` to add/modify Colang flows. Changes take effect on container restart:

```bash
docker compose restart nemo-guardrails
```

### Customizing Guardrails AI Validators

Edit `config/guardrails-ai/guards.json` to add validators from the [Guardrails Hub](https://hub.guardrailsai.com/).

## Extending the Project

### Adding a New Graph Node

1. Create `src/main/java/io/callicode/rag/agent/nodes/MyNewNode.java` annotated with `@Component`
2. Implement `Map<String, Object> process(SelfRagState state)`
3. Register in `SelfRagGraph.java`: `.addNode("myNode", node_async(myNewNode::process))`
4. Wire edges: `.addEdge("existingNode", "myNode")` etc.

### Switching the LLM

Update `application.yml`:
```yaml
spring.ai.ollama.chat.options.model: mistral
```
Run `ollama pull mistral` to download the model.

### Adding Document Sources

Use the `/api/v1/documents` endpoint or extend `DocumentSeeder` with additional `ClassPathResource` entries.

## Running Tests

```bash
./gradlew test
```

Tests use `@MockBean` to mock all external dependencies. No running services are required.

## Project Structure

```
src/main/java/io/callicode/rag/
├── RagApplication.java           Entry point
├── config/                       Spring configuration beans
├── api/                          REST controllers and DTOs
├── agent/                        LangGraph4j state graph and nodes
│   ├── SelfRagGraph.java         Graph topology (topology documented here)
│   ├── SelfRagState.java         State channels shared across all nodes
│   ├── SelfRagService.java       Drives graph execution per request
│   └── nodes/                    One class per graph node
├── rag/                          Document ingestion and seeding
├── guardrails/                   NeMo and Guardrails AI REST clients
└── cache/                        Semantic cache backed by Valkey
```
