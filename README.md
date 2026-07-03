# CivicFlow AI — Jakarta EE Hourglass AI Modeler

## Challenge

This is a template project for the Jakarta EE Hourglass AI Modeler challenge.

## What we built — CivicFlow AI

The skeleton was an empty `process()` stub — no AI Service, no tools. On top of it we built
**CivicFlow AI**: an agentic app that turns a **domain** + **use case** into a real **hourglass model**
for urban mobility & the energy transition — many **stakeholders** and **capabilities** converging on a
narrow waist of **verified** interoperability standards, as structured JSON. The point is
**real, not generic**: every answer is grounded in verified standards and real local data, and a
guardrail rejects anything invented.

**How it works.** The LLM agent reasons in a loop — it calls tools for real facts, retrieves standards
from a knowledge base, **distils** the few cross-cutting standards into the waist, and returns
structured JSON that a guardrail validates before the user sees it.

Built on top of the scaffold:

- **Skill** — a LangChain4j AI Service (`HourglassModeler`) wired via `@RegisterAIService`.
- **4 tools** (function calling, each logged): standards catalog, web concept lookup (Wikipedia),
  real EV charging operators (Open Charge Map), real transit agencies (Transitland).
- **RAG** — in-memory knowledge base of ~18 curated standard + EU-regulation summaries.
- **Output guardrail** — rejects invented standards, keeps the waist narrow (≤7), forces self-correction;
  plus input validation (400) and clean errors (503).
- **Frontend** — static hourglass visualization at `http://localhost:8080/clepsammia/`.

Stack: Jakarta EE 11 · MicroProfile · Open Liberty · LangChain4j · OpenAI.

### Run (Open Liberty dev mode)

```bash
./dev.sh    # sets JDK 25 + PATH, loads .env.local keys, runs 'mvn clean liberty:dev'
```

Then open `http://localhost:8080/clepsammia/`, or call the API:

```bash
curl -s -X POST http://localhost:8080/clepsammia/api/hourglass/analyze \
  -H 'Content-Type: application/json' \
  -d '{"domain":"Urban mobility & energy transition","useCase":"City-wide MaaS in Amsterdam"}'
```

Needs `OPENAI_API_KEY` (model); `OPENCHARGEMAP_API_KEY` / `TRANSITLAND_API_KEY` are optional (the
tools degrade gracefully without them). Keys go in `.env.local` (gitignored).

## Requirements

- Java 23 or later (Java 25 for GlassFish)
- Maven 3.8 or later
- One of the supported Jakarta EE servers:
  - GlassFish 8.0.0 or later
  - Payara 7.2025.2 or later
  - WildFly 39.0.1.Final or later
  - Open Liberty 26.0.0.3-beta or later

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd hourglass-template
mvn clean package
```

### 2. Run on Your Preferred Server

#### GlassFish
```bash
mvn clean package cargo:run -Pglassfish
```

#### Payara
```bash
mvn clean package cargo:run -Ppayara
```

#### WildFly
```bash
mvn clean package wildfly:run
```

#### Open Liberty
```bash
mvn clean package liberty:run
```

### 3. Test the Server

Smoke-test endpoint: `http://localhost:8080/clepsammia/api/hello`


