# **Software Specification: LLM Benchmarking Platform**

## **1. Introduction**

### **1.1 Purpose**

This document specifies the requirements, architecture, data model, and user flows for a local, self-contained
application designed to benchmark multiple Large Language Models (LLMs) against a curated set of tasks. A separate
“judge” LLM evaluates the responses and assigns scores. The system provides real-time progress tracking and persistent
results for analysis.

### **1.2 Scope**

The application is a single-user, local-first platform that can be run via Docker. It includes a web-based frontend for
configuration, execution, and result visualization. It supports multiple LLM providers (e.g., Ollama, OpenAI, LM Studio)
and allows users to define custom tasks and task collections. The system operates in a strictly sequential execution
mode to manage local resource constraints.

### **1.3 Definitions**

- **Judge Model**: The LLM responsible for evaluating the quality of responses from benchmark models.
- **Benchmark Model(s)**: The LLM(s) being tested against a set of tasks.
- **Task**: A single prompt with optional reference answers and failure modes.
- **Task Collection**: A named group of tasks used as a benchmarking unit.
- **Benchmark Run**: A single execution instance involving one judge, one or more benchmark models, and one or more task
  collections.

---

## **2. Overall Description**

### **2.1 Product Perspective**

The system is a standalone monolithic web application composed of a Spring Boot backend and a React SPA frontend, backed
by a PostgreSQL database. It is not intended for multi-tenant or public cloud deployment.

### **2.2 User Classes and Characteristics**

- **Primary User**: AI/ML engineer or researcher who needs to compare LLM performance on domain-specific tasks.
- **Assumptions**: User has access to one or more LLM providers (local or remote) and understands basic LLM concepts.

### **2.3 Operating Environment**

- **Local**: Runs via `docker-compose` (Spring Boot app + PostgreSQL).
- **Private Cloud**: Can be deployed to a private network with no external authentication.

### **2.4 Design and Implementation Constraints**

- No user authentication or authorization.
- Only one benchmark run may be active at any time.
- Must support pausing and resuming of a benchmark run.
- Must provide real-time UI updates during execution.
- Execution is strictly sequential per `(provider, model)` group to manage local resource usage.

---

## **3. Functional Requirements**

### **3.1 Configuration Management**

| ID    | Requirement                                                                                                                                                                                  |
|-------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FR-01 | The system shall allow the user to create, edit, and delete **Provider Configurations**.                                                                                                     |
| FR-02 | A Provider Configuration shall include: name, base URL, models endpoint, inference endpoint, and zero or more HTTP headers (e.g., for API keys). Each header shall have an `is_secret` flag. |
| FR-03 | The system shall allow the user to create, edit, and delete **Benchmark Tasks**.                                                                                                             |
| FR-04 | A Benchmark Task shall include: task ID, category, subcategory, question (prompt), and optional reference answers (`excellent`, `good`, `pass`) and `incorrect_answer_direction`.            |
| FR-05 | The system shall allow the user to create, edit, and delete **Task Collections** and assign tasks to them.                                                                                   |

### **3.2 Benchmark Execution**

| ID    | Requirement                                                                                                                                                                                         |
|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FR-10 | The user shall be able to select exactly one **Judge Model** from any configured provider.                                                                                                          |
| FR-11 | The user shall be able to select one or more **Benchmark Models** from one or more providers.                                                                                                       |
| FR-12 | The user shall be able to select **one or more Task Collections** for the benchmark.                                                                                                                |
| FR-13 | The user shall be able to **start** a new benchmark run. The `POST /api/runs` endpoint shall both create the run record and immediately start the execution process.                                |
| FR-14 | The user shall be able to **pause** a running benchmark at any time.                                                                                                                                |
| FR-15 | The user shall be able to **resume** a previously paused benchmark.                                                                                                                                 |
| FR-16 | The system shall enforce that only **one benchmark run is active** at a time. Any attempt to start a second run shall be rejected.                                                                  |
| FR-17 | The benchmark execution shall be split into two distinct, sequential phases: `BENCHMARKING` and `JUDGING`.                                                                                          |
| FR-18 | During the `BENCHMARKING` phase, the system shall **group all tasks by their target model and provider** and execute all requests for a single model consecutively before moving to the next model. |
| FR-19 | The `JUDGING` phase shall only begin after the `BENCHMARKING` phase has completed for all selected models and tasks.                                                                                |
| FR-20 | The system shall persist the state of every task-model combination (`benchmark_item`) after each step to enable safe pause/resume at any point in either phase.                                     |
| FR-21 | Before processing the first task for a new `(provider, model)` pair, the system shall perform a **warmup call** (e.g., "Hello, World!") to ensure the model is loaded and ready.                    |

### **3.3 Real-Time Monitoring & Results**

| ID    | Requirement                                                                                                                                                                                                               |
|-------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FR-30 | During a benchmark run, the UI shall display in real-time: current task, current model/provider, overall progress (e.g., 15/100 tasks completed), and current phase (`BENCHMARKING` or `JUDGING`).                        |
| FR-31 | The UI shall display a live, append-only log of prompts sent and responses received.                                                                                                                                      |
| FR-32 | After completion (or at any point), the user shall be able to view all results for a benchmark run, including: prompt, model response, judge score, judge reasoning, errors, and performance metrics (time, tokens, TPS). |
| FR-33 | The system shall support **re-running failed judging attempts** for items where the judge LLM failed to produce a valid structured output.                                                                                |
| FR-34 | The user shall be able to **export a benchmark run's results to CSV**.                                                                                                                                                    |

---

## **4. Non-Functional Requirements**

| ID     | Requirement                                                                                                                                                                                                                                             |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-01 | **Performance**: The system must be able to handle benchmark runs with hundreds of tasks and multiple models without UI lag. The UI must remain responsive with thousands of `benchmark_item` records using virtualized tables and incremental updates. |
| NFR-02 | **Reliability**: A benchmark run must be recoverable after an application crash. The system must use a robust `benchmark_item` lifecycle with `IN_PROGRESS` state, attempt counters, and safe claim patterns.                                           |
| NFR-03 | **Usability**: The UI must be intuitive for a technical user. Secrets in provider configurations must be masked in the UI (e.g., show only last 4 characters). A “Test connection” button should be available for providers.                            |
| NFR-04 | **Deployability**: The entire system must be deployable via a single `docker-compose.yml` file. Documentation must cover overrides for `host.docker.internal` on Linux.                                                                                 |
| NFR-05 | **Security**: Secret header values in provider configurations must be encrypted at rest in the database.                                                                                                                                                |

---

## **5. System Architecture**

### **5.1 High-Level Architecture**

The system is a **modular monolith** with the following layers:

- **Frontend**: React SPA (TypeScript) with state management (Redux Toolkit).
- **Backend**: Spring Boot (Java 21/25) with:
    - REST Controllers for CRUD operations.
    - STOMP-over-WebSocket Controller for real-time benchmark events.
    - `BenchmarkOrchestrator` service for managing the sequential benchmark lifecycle.
    - `ProviderClient` strategy pattern for abstracting LLM provider APIs, with a focus on OpenAI-compatibility.
- **Database**: PostgreSQL for persistent storage.

### **5.2 Key Design Patterns**

- **Strategy Pattern**: For `ProviderClient` implementations to handle provider-specific nuances while using a common
  OpenAI-compatible interface.
- **Observer Pattern**: `BenchmarkOrchestrator` publishes events; WebSocket controller subscribes and pushes to the UI
  via STOMP topics.
- **Repository Pattern**: Spring Data JPA repositories for data access.
- **Claim Pattern**: The orchestrator uses atomic database updates with `FOR UPDATE SKIP LOCKED` to safely claim and
  process `benchmark_item` records sequentially.

---

## **6. Data Model (DDL)**

```sql
-- V1__init.sql

-- 1. Provider Configuration
CREATE TYPE provider_type AS ENUM ('OPENAI', 'OLLAMA', 'LM_STUDIO', 'OPENROUTER', 'CUSTOM_OPEN_AI_COMPATIBLE');

CREATE TABLE provider_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type provider_type NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    models_endpoint VARCHAR(2048) NOT NULL,
    inference_endpoint VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE provider_header (
    id BIGSERIAL PRIMARY KEY,
    provider_config_id BIGINT NOT NULL REFERENCES provider_config(id) ON DELETE CASCADE,
    header_key VARCHAR(255) NOT NULL,
    header_value TEXT NOT NULL,            -- encrypted when is_secret = true
    is_secret BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Benchmark Task
CREATE TABLE benchmark_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(255) NOT NULL UNIQUE,  -- user-defined short ID
    category VARCHAR(255) NOT NULL,
    subcategory VARCHAR(255),
    question TEXT NOT NULL,
    excellent TEXT,
    good TEXT,
    pass TEXT,
    incorrect_answer_direction TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Task Collection
CREATE TABLE tasks_collection (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE collection_task (
    collection_id BIGINT NOT NULL REFERENCES tasks_collection(id) ON DELETE CASCADE,
    task_id BIGINT NOT NULL REFERENCES benchmark_task(id) ON DELETE CASCADE,
    PRIMARY KEY (collection_id, task_id)
);

-- 4. Benchmark Run
CREATE TYPE benchmark_run_status AS ENUM ('PENDING', 'FINISHED');

CREATE TABLE benchmark_run (
    id BIGSERIAL PRIMARY KEY,
    run_id VARCHAR(255) NOT NULL UNIQUE,
    run_date TIMESTAMP NOT NULL DEFAULT NOW(),
    status benchmark_run_status NOT NULL DEFAULT 'PENDING', -- PENDING until all items COMPLETED
    judge_provider_config_id BIGINT NOT NULL REFERENCES provider_config(id) ON DELETE RESTRICT,
    judge_model_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 4.a Benchmark run -> collections (many-to-many)
CREATE TABLE benchmark_run_collections (
    benchmark_run_id BIGINT NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
    collection_id BIGINT NOT NULL REFERENCES tasks_collection(id) ON DELETE RESTRICT,
    PRIMARY KEY (benchmark_run_id, collection_id)
);

-- 5. Benchmark Run Target Models
CREATE TABLE benchmark_run_target_model (
    benchmark_run_id BIGINT NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
    provider_config_id BIGINT NOT NULL REFERENCES provider_config(id) ON DELETE RESTRICT,
    model_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (benchmark_run_id, provider_config_id, model_name)
);

-- 6. Benchmark Item (per task per model)
CREATE TYPE benchmark_item_status AS ENUM (
    'NEW',               -- not executed yet
    'IN_PROGRESS',       -- actively executing benchmarking (claimed)
    'WAITING_FOR_JUDGE', -- LLM response stored; waiting for judge
    'COMPLETED',         -- benchmarking + judging finished
    'FAILED'             -- error state; can be retried
);

CREATE TABLE benchmark_item (
    id BIGSERIAL PRIMARY KEY,
    benchmark_run_id BIGINT NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
    benchmark_task_id BIGINT NOT NULL REFERENCES benchmark_task(id) ON DELETE RESTRICT,
    target_provider_config_id BIGINT NOT NULL REFERENCES provider_config(id) ON DELETE RESTRICT,
    target_model_name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_run_target_model 
        FOREIGN KEY (benchmark_run_id, target_provider_config_id, target_model_name)
        REFERENCES benchmark_run_target_model(benchmark_run_id, provider_config_id, model_name),
    status benchmark_item_status NOT NULL DEFAULT 'NEW',
    -- Raw structured LLM response
    llm_response_json JSONB,
    -- Raw judge evaluation result (JSON)
    judge_result_json JSONB,
    -- Parsed fields for queries
    evaluation_score INTEGER,
    evaluation_reason TEXT,
    error_msg TEXT,
    time_taken_ms INTEGER,
    tokens_generated INTEGER,
    -- lifecycle/attempts
    attempts INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    worker_id VARCHAR(255),              -- optional; useful for debugging
    in_progress_at TIMESTAMP,
    request_meta JSONB,                  -- parameters used for the request (temperature, max_tokens, system prompt, etc.)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indices for fast queries / recovery
CREATE INDEX idx_bi_run_status ON benchmark_item (benchmark_run_id, status);
CREATE INDEX idx_bi_run_next_retry ON benchmark_item (benchmark_run_id, next_retry_at);
CREATE INDEX idx_bi_target_model ON benchmark_item (target_provider_config_id, target_model_name);

-- 7. Run logs (append-only)
CREATE TABLE run_log (
    id BIGSERIAL PRIMARY KEY,
    benchmark_run_id BIGINT NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    level VARCHAR(16),
    message TEXT
);
CREATE INDEX idx_run_log_run_created ON run_log (benchmark_run_id, created_at DESC);

-- 8. Optional: store schema version of judge contract that was used
CREATE TABLE judge_schema_version (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES benchmark_run(id) ON DELETE CASCADE,
    schema_name VARCHAR(255),
    schema_version VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## **7. Technology Stack**

| Layer                 | Technology                                                                        |
|-----------------------|-----------------------------------------------------------------------------------|
| **Backend Language**  | Java 25 (LTS)                                                                     |
| **Backend Framework** | Spring Boot 3.x, Spring Web, Spring WebSocket (STOMP), Spring Data JPA, Spring AI |
| **Frontend**          | React 18+, TypeScript, Vite, Tailwind CSS, Redux Toolkit, RTK Query               |
| **Database**          | PostgreSQL 15+                                                                    |
| **Build & Deploy**    | Maven, Docker, Docker Compose                                                     |
| **Communication**     | REST (HTTP/1.1), WebSockets (STOMP over SockJS)                                   |

---

## **8. User Flow: Benchmark Execution & Recovery**

### **Execution Logic**

The `BenchmarkOrchestrator`'s logic is driven entirely by queries on `benchmark_item`.

1. **Start a Run**: Create the `benchmark_run` with status `PENDING`, link to selected collections via
   `benchmark_run_collections`, create `benchmark_run_target_model` entries, and create all `benchmark_item` records
   with status `NEW`.
2. **Benchmarking Phase**:
    - Query: `SELECT * FROM benchmark_item WHERE benchmark_run_id = ? AND status = 'NEW'`.
    - Group results by `(target_provider_config_id, target_model_name)`.
    - For each group:
        - Perform a **warmup call** to the model. If it fails, mark all items in the group as `FAILED` and log the
          error.
        - If warmup succeeds, process each task **sequentially**.
        - Claim an item atomically (set status to `IN_PROGRESS`), execute the request, and on success update it to
          `WAITING_FOR_JUDGE`.
3. **Judging Phase**:
    - Query: `SELECT * FROM benchmark_item WHERE benchmark_run_id = ? AND status = 'WAITING_FOR_JUDGE'`.
    - Process items **sequentially**.
    - Execute judging by calling the judge model. Validate the output against the judge JSON schema.
    - On a successful, valid response, update the item to `COMPLETED`. On failure, increment `attempts` and either
      schedule a retry or mark as `FAILED`.
4. **Finalize Run**:
    - After a judging pass, check if
      `SELECT COUNT(*) FROM benchmark_item WHERE benchmark_run_id = ? AND status != 'COMPLETED'` returns `0`.
    - If yes, update the `benchmark_run` status to `FINISHED`.

### **Pause/Resume Logic**

- **Pausing**: The orchestrator checks an in-memory `paused` flag. Pausing sets this flag and logs the event. The
  current task completes before the loop halts.
- **Resuming**: The orchestrator clears the `paused` flag and restarts its main loop, picking up from the next
  uncompleted item.
- **Crash Recovery**: On startup, the orchestrator scans for `IN_PROGRESS` items with an `in_progress_at` timestamp
  older than a threshold (e.g., 5 minutes) and resets their status to `NEW` to be retried.

### **Retry Policies**

- Both inference and judge calls use a retry policy (default: 3 attempts) with exponential backoff.
- The `attempts`, `last_attempt_at`, and `next_retry_at` fields in `benchmark_item` track this state.

---

## **9. Detailed Architectural Design**

This section elaborates on the architectural implementation of the three core system components—Frontend, Backend, and
Database Initialization—ensuring alignment with software engineering best practices (DRY, KISS, separation of concerns)
and full coverage of functional requirements, including the newly specified robustness and compatibility features.

---

## **9.1 Frontend Architecture**

### **9.1.1 Technology Stack & Structure**

- **Framework**: React 18+ with TypeScript, built using Vite for fast development and optimized production builds.
- **Styling**: Tailwind CSS with `@headlessui/react` for accessible, unstyled components (e.g., modals, dropdowns).
- **State Management**: Redux Toolkit (RTK) with RTK Query for server-state caching and synchronization.
- **Routing**: React Router v6 for SPA navigation.
- **Real-Time Updates**: STOMP client (e.g., `@stomp/stompjs`) connected to `/ws` endpoint. Subscribes to run-specific
  topics.

The frontend is organized into feature-based modules:

```
src/
├── features/
│   ├── providers/          # CRUD for provider configs
│   ├── tasks/              # CRUD for tasks and collections
│   ├── benchmark-runs/     # Run creation, control, and results
│   └── shared/             # Reusable components (e.g., DataTable, FormFields)
├── services/               # API clients (REST + STOMP)
├── store/                  # Redux store configuration
└── App.tsx                 # Main routing and layout
```

### **9.1.2 Feature Coverage & UI Behavior**

#### **1. Provider Configuration Management**

- **UI Components**:
    - List view with cards/table showing name, type, base URL.
    - Form modal for create/edit with fields: name, type (dropdown), base URL, models/inference endpoints, and dynamic
      key-value header pairs. Headers marked as secret are masked (e.g., `••••••abcd`).
- **Validation**:
    - Required fields enforced via Zod schema.
    - Unique name constraint validated on blur and submit via API.
    - "Test Connection" button to validate provider settings.
- **Operations**: Full CRUD via REST endpoints (`/api/providers`). Deletion cascades headers automatically.

#### **2. Benchmark Task & Collection Management**

- **Task Form**:
    - Fields: `task_id` (user-defined unique string), category, subcategory, question (textarea), and optional reference
      answers (`excellent`, `good`, `pass`, `incorrect_answer_direction`).
    - Real-time uniqueness validation for `task_id`.
- **Collection Management**:
    - Dedicated screen to create collections and assign/unassign tasks via a searchable multi-select.
    - Visual feedback on task count per collection.
- **Operations**:
    - Tasks: CRUD via `/api/tasks`.
    - Collections: CRUD via `/api/collections`; task assignment via PATCH `/api/collections/{id}/tasks`.

#### **3. Benchmark Execution Control**

- **Run Creation Flow**:
    - Step 1: Select judge model (dropdown populated from all providers’ models via `/api/providers/{id}/models`).
    - Step 2: Select one or more benchmark models (multi-select with provider grouping).
    - Step 3: Select **one or more** task collections.
    - Validation prevents submission if no judge, no benchmark models, or no collections are selected.
    - **Atomic Action**: The `POST /api/runs` endpoint both creates the run record and immediately starts the execution
      process.
- **Run Control**:
    - Active run displayed in a dedicated panel with:
        - Phase indicator (`BENCHMARKING`/`JUDGING`).
        - Progress bar (e.g., “15/100 items completed”).
        - Current task/model context.
        - **Live Log**: Append-only log showing for each step:
            - **Benchmarking Phase**: Current prompt sent to the LLM and its full response.
            - **Judging Phase**: The judge's prompt, its response, the extracted score, and reasoning.
        - Elapsed time.
    - Buttons: `Pause` (enabled only when running), `Resume` (enabled only when paused).
- **Real-Time Updates**:
    - Subscribes to STOMP topics:
        - `/topic/run/{runId}/progress` for progress events.
        - `/topic/run/{runId}/log` for log messages.
    - Uses virtualized tables and incremental updates to handle large result sets efficiently.

#### **4. Benchmark Run Management & Analysis**

- **Run List View**:
    - Table of all runs with columns: run ID, date, judge model, collections, status, and action buttons (view, delete,
      export).
    - Filtering by status or date range.
- **Run Detail View**:
    - **Summary Dashboard**: Cards showing key aggregated metrics:
        - Average score across all tasks/models.
        - Average time per task (in ms).
        - Average tokens per second (TPS).
    - **Detailed Results Table**: An interactive, virtualized table of all `benchmark_item` records with every available
      field from the DB:
        - Task ID, Category, Model, Status, Score, Response, Reasoning, Error, Time, Tokens, TPS.
        - Inline editing disabled (results are immutable post-completion).
    - **Aggregation Views**:
        - Per-model performance: A table showing for each benchmarked model its average score, avg. time, and avg. TPS.
        - Per-task analysis: A table showing for each task its average score across all models.
        - Failed items list with “Retry Judging” button (triggers FR-33).
- **Export**: `Export CSV` button triggers `GET /api/runs/{id}/export?format=csv`.
- **Deletion**: Hard delete via `/api/runs/{id}` with cascade to `benchmark_item`.

### **9.1.3 Design Principles Applied**

- **DRY**: Shared form components (e.g., `KeyValuePairsInput` for headers) and API hooks (`useProviders()`) reused
  across features.
- **KISS**: Minimal state—RTK Query auto-manages caching/loading/error states for REST data; STOMP state is isolated to
  the benchmark context.
- **Completeness**: All non-generated DB fields are editable via UI; no hidden or inaccessible operations.

---

## **9.2 Backend Architecture**

### **9.2.1 Layered Structure & Best Practices**

The backend follows a strict layered architecture:

```
com.llmbench/
├── controller/     # REST and WebSocket endpoints
├── service/        # Business logic (stateless)
├── repository/     # Spring Data JPA interfaces
├── dto/            # Data Transfer Objects (input/output)
├── model/          # JPA entities (mapped to DB)
├── exception/      # Global exception handler
└── config/         # Bean configurations (e.g., WebSocket, clients)
```

Key principles:

- **DTOs Everywhere**: Controllers accept/return DTOs, never JPA entities. Prevents lazy-loading issues and
  over-fetching.
- **Single Responsibility**: Each service handles one domain (e.g., `ProviderService`, `BenchmarkOrchestratorService`).
- **No Unused Code**: Endpoints mapped 1:1 to frontend requirements.

### **9.2.2 REST API Endpoints**

| Resource        | Method | Path                           | DTOs Used                               | Purpose                                     |
|-----------------|--------|--------------------------------|-----------------------------------------|---------------------------------------------|
| Provider Config | GET    | `/api/providers`               | `ProviderDto`                           | List all providers                          |
|                 | POST   | `/api/providers`               | `CreateProviderDto` → `ProviderDto`     | Create provider                             |
|                 | PUT    | `/api/providers/{id}`          | `UpdateProviderDto` → `ProviderDto`     | Update provider                             |
|                 | DELETE | `/api/providers/{id}`          | —                                       | Delete provider                             |
|                 | GET    | `/api/providers/{id}/models`   | `ModelInfoDto`                          | Fetch available models (calls provider API) |
| Task            | GET    | `/api/tasks`                   | `TaskDto`                               | List tasks                                  |
|                 | POST   | `/api/tasks`                   | `CreateTaskDto` → `TaskDto`             | Create task                                 |
|                 | PUT    | `/api/tasks/{id}`              | `UpdateTaskDto` → `TaskDto`             | Update task                                 |
|                 | DELETE | `/api/tasks/{id}`              | —                                       | Delete task                                 |
| Task Collection | GET    | `/api/collections`             | `CollectionDto`                         | List collections                            |
|                 | POST   | `/api/collections`             | `CreateCollectionDto` → `CollectionDto` | Create collection                           |
|                 | PUT    | `/api/collections/{id}`        | `UpdateCollectionDto` → `CollectionDto` | Update collection                           |
|                 | DELETE | `/api/collections/{id}`        | —                                       | Delete collection                           |
|                 | PATCH  | `/api/collections/{id}/tasks`  | `AssignTasksDto`                        | Assign/unassign tasks                       |
| Benchmark Run   | GET    | `/api/runs`                    | `RunSummaryDto`                         | List runs                                   |
|                 | POST   | `/api/runs`                    | `CreateRunDto` → `RunSummaryDto`        | **Create and start** a new run              |
|                 | GET    | `/api/runs/{id}`               | `RunDetailDto`                          | Get run results                             |
|                 | POST   | `/api/runs/{id}/pause`         | —                                       | Pause run                                   |
|                 | POST   | `/api/runs/{id}/resume`        | —                                       | Resume run                                  |
|                 | POST   | `/api/runs/{id}/retry-judging` | `RetryJudgingDto`                       | Re-judge failed items                       |
|                 | DELETE | `/api/runs/{id}`               | —                                       | Delete run                                  |
| Export          | GET    | `/api/runs/{id}/export`        | — (streams CSV)                         | Export run results to CSV                   |

### **9.2.3 Real-Time Communication**

- **WebSocket Endpoint**: `/ws` (STOMP broker endpoint).
- **Protocol**: STOMP over SockJS.
- **Topics**:
    - `/topic/run/{runId}/progress`: For progress updates.
    - `/topic/run/{runId}/log`: For append-only log messages.
- **Integration**: `BenchmarkOrchestratorService` publishes events via `SimpMessagingTemplate`.

### **9.2.4 Core Services**

#### **`BenchmarkOrchestratorService`**

- **State Management**:
    - Uses a `volatile` flag to enforce single active run (FR-16).
    - Pause/resume controlled by an `AtomicBoolean`.
- **Execution Flow**: As described in Section 8, using the **Claim Pattern** for safe sequential processing.
- **Warm-up**: Before processing a new `(provider, model)` group, sends a "Hello, World!" prompt to verify availability
  and trigger model loading.

#### **`ProviderClient` Strategy & OpenAI Compatibility**

- **Core Principle**: All providers are assumed to be **OpenAI-compatible**.
- **Implementation**: A single `OpenAiCompatibleClient` is used. It sends a standard `ChatCompletionRequest` and handles
  minor provider-specific quirks through configuration or adapters.
- **Robust Communication**: Uses `WebClient` with configurable timeouts and a retry policy (exponential backoff) for all
  LLM calls.

#### **Judge Output Enforcement**

- The orchestrator validates the judge's response against a predefined JSON Schema.
- If validation fails, the raw output is stored in `error_msg`, `attempts` is incremented, and the item is either
  scheduled for retry or marked as `FAILED`.

#### **Secrets Handling**

- **Encryption**: Secret header values (`is_secret = true`) are encrypted at rest using a symmetric key (e.g., AES-GCM)
  derived from an application `MASTER_KEY` environment variable.
- **Security**: The `MASTER_KEY` must be protected by the user. The system never logs or exposes full secret values.

### **9.2.5 Error Handling & Validation**

- **Global Exception Handler**: Converts exceptions to HTTP 4xx/5xx with JSON error bodies.
- **Input Validation**: DTOs use Bean Validation.
- **Operational Errors**: All LLM call errors are caught, logged to `run_log`, and persisted in the `error_msg` field of
  the `benchmark_item`.

---

## **9.3 Database Initialization & Migration**

### **9.3.1 Schema Management**

- **Tool**: Flyway DB (integrated with Spring Boot).
- **Migration Strategy**:
    - Versioned SQL migrations (`V1__init.sql`, `V2__add_indexes.sql`, etc.) in `src/main/resources/db/migration`.
    - Baseline: The complete schema from Section 6 is `V1__init.sql`.

### **9.3.2 Initial Data Creation**

- **Default Providers**: On the very first application start, a repeatable migration (`R__default_providers.sql`)
  creates two default providers:
    1. **Ollama**:
        - `name`: "Local Ollama"
        - `type`: "OLLAMA"
        - `base_url`: Configurable via environment, with documentation for `host.docker.internal` vs. Linux.
        - `models_endpoint`: "/v1/models"
        - `inference_endpoint`: "/v1/chat/completions"
    2. **LM Studio**:
        - `name`: "Local LM Studio"
        - `type`: "LM_STUDIO"
        - `base_url`: Configurable via environment.
        - `models_endpoint`: "/v1/models"
        - `inference_endpoint`: "/v1/chat/completions"

### **9.3.3 Best Practices Applied**

- **Idempotency**: Migrations are versioned and immutable.
- **KISS**: Explicit SQL schema definition.
- **DRY**: Common patterns defined once in the initial migration.
