# LLM Benchmarking Service — Completed Specification

**Audience:** Software architects, backend & frontend engineers, and AI agents responsible for implementing the system.
**Goal:** Complete and consistent specification (REST API, WebSocket events, frontend components, flows, DTOs, implementation notes) that fills in missing details from the provided spec while *not* adding new task/run statuses or other concepts beyond those already declared.

---

## Table of contents

1. [High-level summary / goals (short)](#high-level-summary)
2. [Implementation constraints & choices — clarifications](#constraints)
3. [REST API specification (complete)](#rest-api)
4. [WebSocket / Real-time schema](#websocket)
5. [Backend: services, orchestrator behavior, DB access patterns & locking (detailed)](#backend)
6. [Frontend: UI component map, pages, component props, interactions and flows (React + TypeScript + plain HTML/CSS; no UI frameworks)](#frontend)
7. [Data transfer objects (DTOs) and schema examples (JSON shapes)](#dtos)
8. [Security & secrets (encryption at rest, config, env)](#security)
9. [CSV export and file formats](#csv)
10. [Build & deploy (Docker, docker-compose and runtime layout)](#deploy)
11. [Operational notes: pause/resume, retries, recovery, logging, metrics](#ops)
12. [Appendix: useful SQL snippets and sample WebSocket messages](#appendix)

---

<a name="high-level-summary"></a>

## 1. High-level summary / goals

* Single-user, local-first benchmarking service packaged as a single Docker image (fat JAR) and a static SPA inside it. Uses SQLite for persistence.
* A `Benchmark Run` covers: one judge model (from configured providers), one-or-more *target* benchmark models (from possibly multiple providers), and one-or-more task collections (which contain tasks).
* Execution is sequential and grouped by `(provider, model)` to avoid local resource contention.
* Two execution phases for each run: `BENCHMARKING` → `JUDGING`. A `benchmark_item` (task × target model) is created in DB and progresses through statuses: `NEW` → `WAITING_FOR_JUDGE` → `COMPLETED`, or `FAILED`/`CANT_BE_FINISHED`.
* Client UI interacts via REST for CRUD and run control, and WebSocket (STOMP) for live updates.
* **Important constraint**: We will **not** introduce any extra `benchmark_item` or `benchmark_run` statuses beyond those defined in the spec. To implement atomic claiming/processing we use safe DB update patterns & metadata fields (`attempts`, `last_attempt_at`, `next_retry_at`, `updated_at`) and transactional `UPDATE ... WHERE` claiming patterns. See Backend section.

---

<a name="constraints"></a>

## 2. Implementation constraints & clarifications

* **No new statuses**: We will *not* add `IN_PROGRESS` or similar enums. Claiming is implemented via an atomic DB update that sets `last_attempt_at`, increases `attempts`, and sets `next_retry_at` appropriately while keeping `status` unchanged (or marking it to `WAITING_FOR_JUDGE` only after response is persisted). See "Claiming pattern" below.
* **Frontend stack** (per user instruction): React + TypeScript + Vite, plain HTML + CSS. No Tailwind, no UI component libraries. Use React Context + `useReducer` (simple, well-contained). WebSocket client uses a minimal STOMP client (e.g., `@stomp/stompjs`) — this is JavaScript-only and not a full UI framework.
* **Backend stack**: Spring Boot (Java 25 LTS), Spring Web, Spring Data JPA, WebSocket (STOMP), HikariCP (lightweight connection pooling for SQLite), Jackson for JSON.
* **Database**: SQLite.
* **Provider model lists and inference endpoints**: REST calls to provider `models_endpoint` and `inference_endpoint`. Provider headers may contain secrets; those are encrypted at rest.
* **Single running orchestrator**: The app runs only one `BenchmarkOrchestrator` for the whole process; it enforces only one run active at a time.

---

<a name="rest-api"></a>

## 3. REST API — full spec

**Base path:** `/api`

All requests and responses are JSON unless otherwise stated. For endpoints that modify state (start/pause/resume), the server validates constraints and returns appropriate HTTP codes:

* `200 OK` — success with body
* `201 Created` — created resource (Location header + body)
* `204 No Content` — success with no body (delete)
* `400 Bad Request` — validation error
* `404 Not Found` — resource not present
* `409 Conflict` — business conflict (e.g., attempt to start a run while another active run exists)
* `500 Internal Server Error` — unexpected errors

> **Note**: All endpoints are unauthenticated as per spec.

---

### 3.1 Provider Management

**GET /api/providers**
List all providers (non-secret header values masked).

**Response (200)**:

```json
[
  {
    "id": 10,
    "name": "local-ollama",
    "type": "OLLAMA",
    "baseUrl": "http://localhost:11434",
    "modelsEndpoint": "/v1/models",
    "inferenceEndpoint": "/v1/chat/completions",
    "headers": [
      { "id": 1, "key": "Authorization", "valueMasked": "Bearer ****abcd", "isSecret": true }
    ],
    "createdAt": "2025-10-20T09:00:00Z"
  }
]
```

**GET /api/providers/{id}**
Return provider details including headers with masked values (`valueMasked`). For internal use the server will decrypt.

**POST /api/providers**
Create provider config.

**Request**:

```json
{
  "name": "ollama-local",
  "type": "OLLAMA",
  "baseUrl": "http://localhost:11434",
  "modelsEndpoint": "/v1/models",
  "inferenceEndpoint": "/v1/chat/completions",
  "headers": [
    { "key": "Authorization", "value": "Bearer secret-key", "isSecret": true }
  ]
}
```

**Response (201 Created)**: created provider DTO.

**PUT /api/providers/{id}**
Edit provider. If header `isSecret` true and value omitted, server keeps existing secret.

**DELETE /api/providers/{id}**
Delete provider (cascade deletes provider_header rows).

**POST /api/providers/{id}/test-models**
Fetch provider models using `modelsEndpoint` and return list or error message (used by UI "Refresh Models"). Server uses provider headers and baseUrl to call `{baseUrl}{modelsEndpoint}`. Response includes normalized model ids and any backend-detected model metadata.

**POST /api/providers/{id}/test-inference**
Body:

```json
{ "model": "gpt-3", "prompt": "Hello" }
```

Performs one inference call (warmup/test) and returns raw provider response and success boolean.

---

### 3.2 Tasks & Collections

**GET /api/tasks**
List all tasks (with pagination optional). Return full fields.

**POST /api/tasks**
Create task.

**Request**:

```json
{
  "taskId": "proof_001",
  "category": "Proofreading",
  "subcategory": "Grammar",
  "question": "Fix grammar in: 'This are wrong.'",
  "excellent": "This is wrong.",
  "good": "This is wrong.",
  "pass": "This is wrong.",
  "incorrectAnswerDirection": "missing_subject"
}
```

**PUT /api/tasks/{id}**, **DELETE /api/tasks/{id}** — standard semantics. Duplicate `taskId` must be rejected with `409 Conflict`.

**GET /api/collections**
List collections.

**POST /api/collections**
Create collection (name unique).

**PUT /api/collections/{id}** — rename, etc.

**DELETE /api/collections/{id}** — deletes mapping rows (collection_task).

**POST /api/collections/{id}/tasks**
Assign a list of task ids to a collection (accepts array of existing task IDs). Server must reject duplicates.

**DELETE /api/collections/{id}/tasks/{taskId}**
Remove mapping.

---

### 3.3 Benchmark Runs & Control

**GET /api/runs**
List runs with summary fields (run_id, run_date, status, judge info). Supports `?status=PENDING` filter.

**GET /api/runs/{runId}**
Full run details, list of associated target models, associated collections, number of items, counts per status, timestamps.

**POST /api/runs** — *Create and Start a run* (per FR-13). This endpoint both creates run records and immediately starts execution. Because the service must allow pausing/resuming, the orchestrator begins processing in background after the DB records are created.

**Request**:

```json
{
  "runId": "run-2025-10-20-01",    // optional; server generates if missing
  "judgeProviderConfigId": 2,
  "judgeModelName": "judge-v1",
  "targetModels": [
    { "providerConfigId": 3, "modelName": "gpt-small" },
    { "providerConfigId": 4, "modelName": "gpt-xl" }
  ],
  "collectionIds": [1, 2]
}
```

**Validation:**

* Exactly one judge model (FR-10).
* At least one target model (FR-11).
* At least one collection (FR-12).
* Reject if another run exists with `status = PENDING` and has uncompleted items. Respond with `409 Conflict`.

**Response (201)**: run summary (runId, id).

**POST /api/runs/{runId}/pause**
Pause the running run. If run not active, return 400. Response 200 with current status. Pause sets an in-memory paused flag (persist run log). The currently processing item is allowed to finish (per spec).

**POST /api/runs/{runId}/resume**
Resume a previously paused run. If run is not paused or already finished, reject appropriately.

**POST /api/runs/{runId}/abort** *(optional but helpful)*
Because spec doesn't define abort explicitly, do not create destructive abort. If implemented, must be consistent with "Do not add statuses" — so prefer only `pause`/`resume`. We'll omit abort to adhere to "do not add extra statuses".

**GET /api/runs/{runId}/items**
List `benchmark_item`s for run with filter `?status=` and pagination. Useful for "View Results" and re-run failed judge attempts.

**POST /api/runs/{runId}/items/{itemId}/retry-judge**
Re-run judge evaluation for a single `benchmark_item` where `status` is `WAITING_FOR_JUDGE` or `FAILED` due to judge failure. This corresponds to FR-33. The server will enqueue a judge attempt by adjusting `next_retry_at` and setting `attempts` appropriately.

**POST /api/runs/{runId}/export**
Export run results (CSV or Markdown). Body:

```json
{ "format": "CSV", "includeDetailed": true }
```

Returns a downloadable file (Content-Disposition) or a server-generated temp URL. (In our local-first system, responding with file bytes is fine.)

**DELETE /api/runs/{runId}**
Delete run and all its items/results. Must be allowed per UI delete action. If run active (pending items) reject with `409 Conflict` or only allow if user confirms; spec allows delete so implement confirmation in UI.

---

### 3.4 Logs & Diagnostics

**GET /api/runs/{runId}/logs**
List run logs (append-only). Supports `since` filter and `limit`.

---

### 3.5 Misc endpoints

**GET /api/providers/{id}/models** — quick model list cached briefly by server.

**GET /api/status** — service health and whether an orchestrator run is active.

---

<a name="websocket"></a>

## 4. WebSocket (STOMP) — real-time UI updates

**Endpoint:** `/ws` (SockJS fallback allowed). STOMP with topics:

* **Subscribe**: `/topic/run/{runId}/events` — broadcast of all run-level events (phase changes, progress, summary).
* **Subscribe**: `/topic/run/{runId}/items` — per-item updates (append-only log of item events).
* **Subscribe**: `/topic/run/{runId}/log` — newline-ordered run logs.

**Client destination**: `/app/run/{runId}/control` — control actions via STOMP (not required; REST is primary). We'll implement only server → client messages; UI uses REST to issue pause/resume and uses STOMP purely for events.

### Message schemas

**RunEvent** (sent to `/topic/run/{runId}/events`)

```json
{
  "type": "RUN_STATUS", // or "PHASE_CHANGE", "PROGRESS_SUMMARY"
  "timestamp": "2025-10-20T09:01:01Z",
  "payload": {
    "runId": "run-2025-10-20-01",
    "status": "PENDING", // or FINISHED
    "phase": "BENCHMARKING", // or JUDGING
    "completedItems": 15,
    "totalItems": 100
  }
}
```

**ItemEvent** (sent to `/topic/run/{runId}/items`)

```json
{
  "type": "ITEM_UPDATE",
  "timestamp": "...",
  "payload": {
    "itemId": 123,
    "benchmarkTaskId": 5,
    "targetProviderConfigId": 3,
    "targetModelName": "gpt-small",
    "status": "WAITING_FOR_JUDGE",
    "llmResponseJson": { /* provider raw */ },
    "timeTakenMs": 512,
    "tokensGenerated": 45,
    "attempts": 1,
    "errorMsg": null
  }
}
```

**RunLogMessage** (sent to `/topic/run/{runId}/log`)

```json
{ "timestamp": "...", "level": "INFO", "message": "Warmup success for provider X model Y" }
```

> **Client behavior:** Subscribe to `/topic/run/{runId}/events` and `/topic/run/{runId}/items` when viewing a run in the UI. If WebSocket disconnects, client should re-subscribe on reconnect and can fetch current run state via `GET /api/runs/{runId}`.

---

<a name="backend"></a>

## 5. Backend — services, orchestrator & DB access patterns

This section outlines core services, how the orchestrator claims items without adding statuses, transactional patterns, retry/backoff, warmup, and failure handling.

### 5.1 Key Spring services

* `ProviderService` — CRUD for providers, header encryption/decryption, test models/inference.
* `TaskService` — CRUD for tasks.
* `CollectionService` — CRUD for collections and collection-task mapping.
* `RunService` — create run records, compute run summaries, export.
* `BenchmarkItemService` — create items for run, query by status, atomic claim/update operations.
* `BenchmarkOrchestrator` — central service that executes runs (singleton bean).
* `WebSocketPublisher` — publishes STOMP messages.
* `RunLogService` — append-only run logs.

### 5.2 BenchmarkOrchestrator responsibilities

* Ensure only one run active at a time. Use a DB check + in-memory `currentActiveRunId` with synchronized access. If process restarts, the DB check is authoritative: `SELECT COUNT(*) FROM benchmark_item WHERE benchmark_run_id = ? AND status != 'COMPLETED'` to determine pending items on startup; resurrect orchestrator loop if needed.

* Execution loop outline (pseudocode):

```text
startOrchestratorForRun(runId):
  if EXISTS other run with pending items -> reject start
  set currentActiveRunId = runId
  publish RUN_STATUS "BENCHMARKING started"
  // BENCHMARKING phase
  while pauseFlag == false and exists items with status = 'NEW':
    // want to process grouped by (provider, model)
    groups = SELECT DISTINCT target_provider_config_id, target_model_name 
             FROM benchmark_item WHERE benchmark_run_id = runId AND status = 'NEW'
             AND (next_retry_at IS NULL OR next_retry_at <= now())
    for each group in groups:
      perform warmup for (provider, model)
      if warmup fails:
        UPDATE benchmark_item SET status='FAILED', error_msg=... WHERE benchmark_run_id=runId AND target_provider_config_id=? AND target_model_name=?
        publish events & logs
        continue next group
      // process tasks in group sequentially
      loop:
        // atomic claim: try to claim one NEW item from this group
        in one transaction:
          SELECT id, attempts FROM benchmark_item 
           WHERE benchmark_run_id = ? AND target_provider_config_id=? AND target_model_name=? AND status='NEW'
             AND (next_retry_at IS NULL OR next_retry_at <= now()) ORDER BY id LIMIT 1 FOR UPDATE
          if none -> break
          UPDATE benchmark_item SET attempts = attempts + 1, last_attempt_at = now(), next_retry_at = now() + makeIntervalSeconds(backoffSec)
          WHERE id = :id
        execute provider inference call
        on success:
          UPDATE benchmark_item SET llm_response_json = :json, status='WAITING_FOR_JUDGE', time_taken_ms=..., tokens_generated=... , updated_at=now() WHERE id=:id
          publish ITEM_UPDATE event
        on transient failure:
          // attempts incremented already, set next_retry_at to future based on exponential backoff
          UPDATE benchmark_item SET error_msg = :msg, updated_at=now() WHERE id=:id
          publish log
```

**Claiming pattern**: We rely on `SELECT ... FOR UPDATE` inside a single DB transaction on SQLite (SQLite supports transactional locking via `BEGIN IMMEDIATE` etc.). Because adding `IN_PROGRESS` status is not allowed, we mark attempts and set `next_retry_at` to a time in the future to avoid double-claiming. The `attempts` incremented ensures idempotency and detection of persistent failures.

**Important:** Implementation must use proper SQLite transactional semantics. If running on local but DB could be mounted, ensure single process owns orchestrator. We must use `updated_at` optimistic concurrency too if desired.

### 5.3 Judging phase

After no `NEW` items remain:

* Publish `PHASE_CHANGE` to `JUDGING`.
* Query `benchmark_item WHERE status = 'WAITING_FOR_JUDGE'` and process grouped by (provider, model) for the runner judge model? Wait — judge model is one selected *judge* model (single model chosen in run). But judge LLM is not a separate system: judge is a selected model from provider list. So the "judging" calls always go to the judge provider/model for the run. We'll call the judge model once per item, passing the original prompt, the model response, and the "reference answers" (excellent/good/pass) to produce a structured score.

Judging logic:

* For each item:

  * Atomically claim via the same `attempts`/`next_retry_at` pattern.
  * Perform judge invocation — pass structured data and instruct the judge to return a JSON result with fields: `score` (0-100), `reason` (string), `structured` (true/false), `raw` (raw JSON).
  * Validate response structure. If valid, update `benchmark_item`:

    * `judge_result_json = raw`
    * `evaluation_score = score`
    * `evaluation_reason = reason`
    * `status = 'COMPLETED'`
    * update timestamps/metrics
  * If judge returns invalid structured output or an error:

    * increment `attempts`, set `next_retry_at` as exponential backoff
    * if attempts exceed configured max (default 3), mark `FAILED` and store `error_msg`.
    * Support re-run via API `POST /api/runs/{runId}/items/{itemId}/retry-judge` to reset `next_retry_at` and attempts so orchestrator will pick it up again.

### 5.4 Retry policy & backoff

* Default max attempts: 3. Configurable via application properties.
* Exponential backoff base: 2^attempts * base (e.g., base 5 seconds → attempts 1→10s, 2→20s).
* For judge-specific failures, the `attempts` field tracks judge attempts separately from inference attempts. To keep single attempts column, each item’s attempt increments for either inference or judge stage. The orchestrator must check whether `llm_response_json` is present to determine the phase for the item. If `llm_response_json` is present and status is `WAITING_FOR_JUDGE`, attempt is judge attempt.

### 5.5 Warmup call

* Implemented as `POST {baseUrl}{inferenceEndpoint}` with prompt `"Hello, world!"` or small instruction. If warmup fails, mark all group items `FAILED` with error explaining warmup error.

### 5.6 Recovery after crash

* On application restart:

  * `BenchmarkOrchestrator` checks DB for any run with items not `COMPLETED`. If exists pick the earliest run with `status = PENDING` and resume orchestrator loop unless user had explicitly paused (persist paused state is not required in DB; we store last pause in run_log and have `paused` persisted? The spec uses in-memory `paused` flag. For crash-safe pause we must persist pause. However spec says "The orchestrator checks an in-memory paused flag." It also says "pause and resume must work." To preserve pause across restarts, store `run_paused` table or an entry in `benchmark_run`? The existing model doesn't include a `paused` column. The spec forbids adding statuses but not columns. To avoid adding fields, we will persist pause state as a `run_log` entry and on restart assume resume unless `run_log` contains a last event "PAUSED" without "RESUMED" and the orchestrator should respect it. That satisfies requirement without adding columns.)
  * Recompute `next_retry_at` and attempt counts to pick items.

---

<a name="frontend"></a>

## 6. Frontend — UI design, components, props and flows

**Stack**: React + TypeScript + Vite, plain HTML & CSS (BEM-style), `@stomp/stompjs` for WebSocket STOMP client. Use `React Context` + `useReducer` for app state; use `fetch()` for REST calls. No external UI framework.

### 6.1 Routing / top-level pages

* `/` — Dashboard (three tabs in main area: New Run / Continue Run / View Results)
* `/settings` — Settings (Providers / Task Collections)
* `/runs/{runId}` — Real-time run view (progress, logs, cancel/pause/resume)
* `/results/{runId}` — Result viewer (also accessible from View Results)

### 6.2 Global layout

* `TopBar` — left: app name; right: `Settings` button and `StatusIndicator` (shows whether orchestrator active).
* `LeftNav` (optional) — Tab links (Dashboard, Settings).
* `Main` — central content area with tabs/pages.

### 6.3 Components (names + responsibilities)

> Use plain HTML/CSS components. Each component described with required props and behaviors.

#### 6.3.1 `NewRunPage`

* Shows selection boxes and start button.
* Subcomponents:

  * `JudgeSelector`
  * `TargetModelsSelector`
  * `CollectionsSelector`
  * `StartRunButton`
* Behavior:

  * `JudgeSelector`:

    * Lists providers (`GET /api/providers`) dropdown; on provider select, calls `GET /api/providers/{id}/models` to populate model dropdown.
    * Allows refresh models.
  * `TargetModelsSelector`:

    * For each provider show a collapsible provider group: provider name and "Refresh Models" button.
    * Within group multi-select checkbox list of models. Prevent selecting duplicate model entries (same provider and model selected twice).
  * `CollectionsSelector`:

    * Multi-select list of collections (GET /api/collections).
  * `StartRunButton`:

    * Enabled only if judge selected and at least one target model selected and at least one collection selected.
    * On click POST /api/runs (create & start). On success navigate to `/runs/{runId}`.

#### 6.3.2 `ContinueRunPage`

* `RunList` — lists `GET /api/runs?status=PENDING`. For each item show runId, date, progress summary.
* Allow selection of one run and a `Continue` button which calls `POST /api/runs/{runId}/resume` (if paused) or navigates to run view if orchestrator is active. If run already active by orchestrator, UI should show `Resume` only if paused. This page calls `GET /api/status` to decide.

#### 6.3.3 `RunView` (real-time)

* `RunHeader` — show runId, judge model/provider, status, phase, start time, progress bar (completed/total).
* `Controls` — `Pause` (POST /api/runs/{runId}/pause), `Resume` (POST /api/runs/{runId}/resume), `Export CSV` (POST /api/runs/{runId}/export).
* `LiveLogPanel` — subscribes to `/topic/run/{runId}/log` and shows append-only logs (timestamp + level + message).
* `LiveItemsPanel` — subscribes to `/topic/run/{runId}/items` and shows latest item updates (task name/id, provider/model, status, time taken, tokens). Supports filtering by provider or status.
* `RawEventsPanel` — collapsible panel showing raw JSON events for debugging.
* Behavior:

  * On mount, fetch run summary (`GET /api/runs/{runId}`) and subscribe to topics.
  * If WebSocket disconnects, show "Reconnecting..." and periodically re-fetch run state.

#### 6.3.4 `ResultsPage`

* `RunsList` — all runs (GET /api/runs).
* When a run selected:

  * `RunSummaryCard` — run metadata.
  * `AveragePerformanceTable` — Table 1 (provider, model, AVG Time, AVG TPS, AVG Score). Provides `Export CSV`, `Export MD`.
  * `DetailedResultsTable` — Table 2 (per item). Columns as spec. Rows expandable to show judge reasoning and raw JSON. `Export CSV`, `Export MD`.
* `DeleteRunButton` — delete selected run (DELETE /api/runs/{runId}). Confirmation modal required.

#### 6.3.5 `SettingsPage`

Two tabs: Providers and Task Collections.

* **Providers tab**

  * `ProviderList` — each provider row with `Edit`, `Delete`, `Refresh Models`, `Test Inference`.
  * `ProviderFormModal` — Add/Edit provider. Fields per spec including list of header rows (component `KeyValueList`).
  * `TestProviderPanel` — uses `POST /api/providers/{id}/test-models` and `POST /api/providers/{id}/test-inference`.
  * Secrets displayed masked (last 4 characters) in lists and form preview. Changing provider without value will leave secret unchanged.

* **Tasks & Collections tab**

  * `TaskList` — view tasks with `Add`, `Edit`, `Delete`.
  * `TaskFormModal` — create/edit tasks: `taskId`, `category`, `subcategory`, `question`, `excellent`, `good`, `pass`, `incorrectAnswerDirection`.
  * `CollectionList` — add/edit/delete collections.
  * `CollectionEditView` — show tasks in collection, add/remove tasks via multi-select. Enforce unique membership; duplicates rejected.

### 6.4 UI CSS / styling guidance

* Small, clear layout: minimal, responsive.
* Use CSS variables for colors and spacing. BEM style classes.
* Accessible components (labels, keyboard focus, aria-* attributes).

### 6.5 UX flows (end-to-end)

1. **Create provider**: `Settings` → `Providers` → `Add New` → fill data → `Save`. Use `Test Models` to verify connectivity.
2. **Create tasks/collections**: `Settings` → `Tasks Collections` → add tasks and collections. Add tasks into collections.
3. **Start new run**:

   * `New Run` tab → select judge provider+model (use Refresh if necessary) → select target models from multiple provider groups → select task collections → `Start Benchmark`.
   * On success, the server creates run/target models/items and orchestrator starts processing. Client navigates to `RunView` to see real-time updates.
4. **Pause**: Click `Pause` in `RunView`. Orchestrator completes current item and halts. UI shows "Paused". Resume via `Resume`.
5. **Re-run failed judge**: In `Results` view, find `FAILED` item and click `Retry Judge` to re-queue item for judging.

---

<a name="dtos"></a>

## 7. DTOs and JSON shapes (complete)

> The server returns DTOs; controllers accept DTOs. Below are canonical shapes.

### 7.1 Provider DTOs

**ProviderDto**

```json
{
  "id": 3,
  "name": "local-ollama",
  "type": "OLLAMA",
  "baseUrl": "http://localhost:11434",
  "modelsEndpoint": "/v1/models",
  "inferenceEndpoint": "/v1/chat/completions",
  "headers": [
    { "id": 1, "key": "Authorization", "valueMasked": "Bearer ****abcd", "isSecret": true }
  ],
  "createdAt": "2025-10-20T09:00:00Z"
}
```

**ProviderCreateRequest**

```json
{
  "name": "ollama-local",
  "type": "OLLAMA",
  "baseUrl": "http://localhost:11434",
  "modelsEndpoint": "/v1/models",
  "inferenceEndpoint": "/v1/chat/completions",
  "headers": [
    { "key": "Authorization", "value": "Bearer XXXX", "isSecret": true }
  ]
}
```

### 7.2 Task DTOs

**TaskDto**

```json
{
  "id": 11,
  "taskId": "proof_001",
  "category": "Proofreading",
  "subcategory": "Grammar",
  "question": "...",
  "excellent": "...",
  "good": "...",
  "pass": "...",
  "incorrectAnswerDirection": "missing_subject",
  "createdAt": "..."
}
```

### 7.3 Collection DTOs

**CollectionDto**

```json
{
  "id": 5,
  "name": "Proofreading Suite",
  "taskIds": [11, 12]
}
```

### 7.4 Run DTOs

**RunCreateRequest**

```json
{
  "runId": "optional-id",
  "judgeProviderConfigId": 2,
  "judgeModelName": "judge-v1",
  "targetModels": [
    { "providerConfigId": 3, "modelName": "gpt-small" }
  ],
  "collectionIds": [1, 2]
}
```

**RunSummaryDto**

```json
{
  "id": 77,
  "runId": "run-2025-10-20-01",
  "status": "PENDING",
  "runDate": "2025-10-20T09:00:00Z",
  "judgeProviderConfigId": 2,
  "judgeModelName": "judge-v1",
  "totalItems": 100,
  "completedItems": 23,
  "phase": "BENCHMARKING" // derived by orchestrator (not persisted)
}
```

### 7.5 Item DTO

**BenchmarkItemDto**

```json
{
  "id": 123,
  "benchmarkRunId": 77,
  "benchmarkTaskId": 11,
  "targetProviderConfigId": 3,
  "targetModelName": "gpt-small",
  "status": "WAITING_FOR_JUDGE",
  "llmResponseJson": { "text": "..." },
  "judgeResultJson": null,
  "evaluationScore": null,
  "evaluationReason": null,
  "errorMsg": null,
  "timeTakenMs": 501,
  "tokensGenerated": 45,
  "attempts": 1,
  "lastAttemptAt": "2025-10-20T09:05:00Z",
  "nextRetryAt": "2025-10-20T09:05:10Z",
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

<a name="security"></a>

## 8. Security & secrets

**Secret encryption at rest** (NFR-05):

* Use a symmetric encryption approach (AES-256-GCM) to encrypt provider header values before storing them in DB (column `provider_header.header_value`). The server has a master key stored in a file on disk mounted into the container (path configurable via env var `APP_MASTER_KEY_PATH`) or passed as environment variable `APP_MASTER_KEY_BASE64`. Prefer file-based to avoid accidental shell history leaks.
* On startup, server reads master key and logs truncated fingerprint. If key missing, server runs in "insecure local dev mode" and warns user (but still encrypts with an ephemeral key stored temporarily) — but production users must supply the key.
* Secrets returned via REST must be masked: `valueMasked` but server decrypts internally for use with providers.
* When editing providers: if header marked `isSecret` and incoming value omitted/empty → keep old encrypted value. If non-empty → re-encrypt and store.

**Transport security**: Whole system is local-first, but if user binds to network, TLS is recommended. For local Docker, `http` is fine by default.

**Provider headers**: Validate length and characters. Ensure header keys are case-preserved when sending to provider.

---

<a name="csv"></a>

## 9. CSV & Markdown export

**CSV export** for results (per FR-34):

* Exported CSV has two types: "average" and "detailed".
* `Average CSV` columns:

  * provider_name, model_name, avg_time_per_task_ms, avg_tokens_per_second, avg_score, tasks_count
* `Detailed CSV` columns:

  * provider_name, model_name, task_id, task_name, task_status, spent_time_ms, tokens_generated, tokens_per_second, score, judge_reason, llm_response_text, error_msg

**CSV formatting specifics:**

* Use `UTF-8` encoding.
* Use `,` as delimiter, escape fields with double quotes if they contain `,` or newlines.
* Timestamp fields in ISO 8601.

**Markdown export**:

* `Average` → markdown table.
* `Detailed` → collapsible per model with markdown tables.

**API**: `POST /api/runs/{runId}/export` returns `Content-Disposition: attachment; filename="run-{runId}-detailed.csv"` and body of bytes.

---

<a name="deploy"></a>

## 10. Build & Deploy

### 10.1 Maven & JAR packaging

* Backend: Maven builds fat JAR containing static `frontend/dist` files under `resources/static` so Spring Boot can serve the SPA.
* Build pipeline:

  1. `cd frontend && npm install && npm run build` → outputs `dist/`.
  2. Maven `resources` include `frontend/dist` to be packaged into JAR.
  3. `mvn -Pprod package` builds fat jar.

### 10.2 Dockerfile (sample)

```dockerfile
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app
COPY target/llmbench-fat.jar app.jar
# optional: create default mounted directory for sqlite DB
VOLUME /data/llmbench
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
```

### 10.3 docker-compose.yml (sample)

```yaml
version: "3.8"
services:
  llmbench:
    image: llmbench:latest
    build: .
    ports:
      - "8080:8080"
    volumes:
      - ./data:/data/llmbench   # host folder contains sqlite DB file and master key
    environment:
      - APP_DB_PATH=/data/llmbench/llmbench.db
      - APP_MASTER_KEY_PATH=/data/llmbench/master.key
    restart: unless-stopped
```

**Notes for Linux / host.docker.internal:** Document override: `extra_hosts` or configure providers to use container accessible hostnames.

### 10.4 Initial default config

* On first run if DB empty, application creates default providers (e.g., sample OpenAI-compatible endpoints) and sample task collections. Provide a `--reset-defaults` endpoint or `Settings -> Reset to Defaults` button.

---

<a name="ops"></a>

## 11. Operational notes (pause/resume, retries, reliability)

### 11.1 Pause / resume

* Pause sets in-memory `paused` flag (and writes `PAUSED` entry in `run_log`).
* The orchestrator finishes the currently processing item and then stops the main loop. The UI must consider the run paused if it sees run log last message `PAUSED` without a subsequent `RESUMED`.
* Resuming clears paused flag and the orchestrator resumes processing.

### 11.2 Safe resume after crash

* On startup, if there are runs with uncompleted items:

  * If the last run log shows `PAUSED` and no `RESUMED`, the orchestrator will **not** automatically continue; it will set the run to be continued manually via `Continue Previous Run`. This prevents unexpected CPU usage after a crash.
  * Otherwise the orchestrator can resume processing automatically or prompt the user. Default choice: **do not auto-start** — rely on UI `Continue` to begin. This is safer for local deployments.

### 11.3 Attempt counters & backoff

* `attempts` increments on each claimed attempt (both inference and judge). When `attempts` > configured limit, mark `FAILED` or `CANT_BE_FINISHED`.
* `next_retry_at` governs when an item becomes claimable again.

### 11.4 Logs & metrics

* Run-level logs persisted into `run_log` table.
* For performance monitoring, store `timeTakenMs` and `tokens_generated` per item.
* Aggregate metrics computed when building results.

---

<a name="appendix"></a>

## 12. Appendix — SQL and sample WebSocket messages

### 12.1 Atomic claim SQL (example pseudocode for JPA / JDBC)

```sql
BEGIN TRANSACTION;

-- find one item to claim
SELECT id, attempts
FROM benchmark_item
WHERE benchmark_run_id = :runId
  AND target_provider_config_id = :providerId
  AND target_model_name = :modelName
  AND status = 'NEW'
  AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)
ORDER BY id
LIMIT 1
FOR UPDATE;

-- if found
UPDATE benchmark_item
SET attempts = attempts + 1,
    last_attempt_at = CURRENT_TIMESTAMP,
    next_retry_at = datetime('now', '+' || :backoffSeconds || ' seconds')
WHERE id = :id;

COMMIT;
```

*(Translate to Spring `@Transactional` and `@Lock`/native query for SQLite)*

### 12.2 Sample WebSocket `ItemEvent` (JSON)

```json
{
  "type": "ITEM_UPDATE",
  "timestamp": "2025-10-20T09:34:00Z",
  "payload": {
    "itemId": 456,
    "benchmarkTaskId": 11,
    "targetProviderConfigId": 3,
    "targetModelName": "gpt-small",
    "status": "WAITING_FOR_JUDGE",
    "llmResponseJson": { "text": "Corrected sentence: This is wrong." },
    "timeTakenMs": 410,
    "tokensGenerated": 12,
    "attempts": 1,
    "errorMsg": null
  }
}
```

---

## Final remarks & decisions made to resolve ambiguities

1. **No new statuses added**: To satisfy the requirement "Do not add additional statuses", we implemented a **claiming pattern** relying on `attempts`, `last_attempt_at`, and `next_retry_at` with transactional `SELECT ... FOR UPDATE`. This gives equivalent behavior to `IN_PROGRESS` without introducing new enums.
2. **Pause persistence**: Pause is stored as a log message. UI and orchestrator use the presence/absence of `PAUSED` vs `RESUMED` in `run_log` to decide whether to auto-resume after a restart (default: do not auto-start — user must continue).
3. **Frontend frameworks**: Although original spec mentioned Redux/Tailwind, the user's instruction requested plain HTML/CSS and React+TypeScript. The delivered UI design is framework-free (React + Context/hooks), and the REST contract remains identical to what a Redux-based implementation would call.
4. **Judge model**: The judge is one chosen model from provider configs. Judging calls are made to that model; judge prompt format is a standardized wrapper and the judge is expected to return a structured JSON; backend validates structure and retries if invalid.
5. **Start endpoint**: `POST /api/runs` both creates run rows and starts orchestrator, as required (FR-13). If another run is active, return `409 Conflict`.
6. **Exports**: CSV/MD exports available on API and UI.

---
