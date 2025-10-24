## API Endpoints Table

| HTTP Method | Endpoint URL                                        | Description                                                     |
|-------------|-----------------------------------------------------|-----------------------------------------------------------------|
| GET         | `/api/v1/providers`                                 | Get list of all providers                                       |
| GET         | `/api/v1/providers/{providerId}` includeModels=true | Get provider details (includeModels query parameter for models) |
| GET         | `/api/v1/collections`                               | Get all task collections                                        |
| GET         | `/api/v1/collections/{collectionId}/tasks`          | Get tasks in a specific collection                              |
| GET         | `/api/v1/tasks`                                     | Get all tasks                                                   |
| GET         | `/api/v1/tasks/{taskId}`                            | Get details of a specific task                                  |
| POST        | `/api/v1/runs`                                      | Start a new benchmark run                                       |
| GET         | `/api/v1/runs`  status=completed,failed,etc         | Get list of all benchmark runs                                  |
| GET         | `/api/v1/runs/{runId}/tasks`                        | Get tasks associated with a specific run                        |
| POST        | `/api/v1/runs/{runId}/continue`                     | Continue a paused benchmark run                                 |
| GET         | `/api/v1/runs/{runId}/reports/summary`              | Get summary report data for a run                               |
| GET         | `/api/v1/runs/{runId}/reports/details`              | Get detailed report data for a run                              |
| POST        | `/api/v1/runs/{runId}/stop`                         | Stop an active benchmark run                                    |
| WS          | `/api/v1/runs/{runId}/logs`                         | Real-time log stream for a run                                  |
| WS          | `/api/v1/runs/{runId}/status`                       | Real-time status updates for a run                              |
| PUT         | `/api/v1/providers/{providerId}`                    | Update a provider's configuration                               |
| POST        | `/api/v1/providers`                                 | Create a new provider                                           |
| DELETE      | `/api/v1/providers/{providerId}`                    | Delete a provider                                               |
| POST        | `/api/v1/providers/{providerId}/tests/models`       | Test provider's model list retrieval                            |
| POST        | `/api/v1/providers/{providerId}/tests/inference`    | Test provider's inference capability                            |
| PUT         | `/api/v1/tasks/{taskId}`                            | Update a task's configuration                                   |
| DELETE      | `/api/v1/tasks/{taskId}`                            | Delete a task                                                   |
| POST        | `/api/v1/tasks`                                     | Create a new task                                               |
| PUT         | `/api/v1/collections/{collectionId}`                | Update a task collection                                        |
| DELETE      | `/api/v1/collections/{collectionId}`                | Delete a task collection                                        |
| POST        | `/api/v1/collections`                               | Create a new task collection                                    |
| GET         | `/api/v1/collections/{collectionId}`                | Get details of a task collection                                |
| POST        | `/api/v1/collections/{collectionId}/tasks`          | Add a task to a collection                                      |
| DELETE      | `/api/v1/collections/{collectionId}/tasks/{taskId}` | Remove a task from a collection                                 |

---

## Endpoints Grouped by UI Components

### Dashboard Run Tab (Benchmark Setup)

- `GET /api/v1/providers` - Get list of all providers
- `GET /api/v1/providers/{providerId}` - Get provider details
- `GET /api/v1/collections` - Get all task collections
- `GET /api/v1/collections/{collectionId}/tasks` - Get tasks in a specific collection
- `GET /api/v1/tasks` - Get all tasks
- `GET /api/v1/tasks/{taskId}` - Get details of a specific task
- `POST /api/v1/runs` - Start a new benchmark run

### Dashboard Continue Tab (Benchmark Management)

- `GET /api/v1/runs` - Get list of all benchmark runs
- `GET /api/v1/runs/{runId}/tasks` - Get tasks associated with a specific run
- `POST /api/v1/runs/{runId}/continue` - Continue a paused benchmark run

### Dashboard Results Tab (Benchmark Results)

- `GET /api/v1/runs` - Get list of all benchmark runs
- `GET /api/v1/runs/{runId}/reports/summary` - Get summary report data for a run
- `GET /api/v1/runs/{runId}/reports/details` - Get detailed report data for a run
- `GET /api/v1/runs/{runId}/reports/summary?format=md` - Download as Markdown
- `GET /api/v1/runs/{runId}/reports/summary?format=csv` - Download as CSV
- `GET /api/v1/runs/{runId}/reports/details?format=md` - Download as Markdown
- `GET /api/v1/runs/{runId}/reports/details?format=csv` - Download as CSV

### Running Benchmark View (Active Execution)

- `POST /api/v1/runs/{runId}/stop` - Stop an active benchmark run
- `WS /api/v1/runs/{runId}/logs` - Real-time log stream for a run
- `WS /api/v1/runs/{runId}/status` - Real-time status updates for a run

### Settings Providers View

- `GET /api/v1/providers` - Get list of all providers
- `GET /api/v1/providers/{providerId}` - Get provider details
- `PUT /api/v1/providers/{providerId}` - Update provider configuration
- `POST /api/v1/providers` - Create new provider
- `DELETE /api/v1/providers/{providerId}` - Delete provider
- `POST /api/v1/providers/{providerId}/tests/models` - Test model list retrieval
- `POST /api/v1/providers/{providerId}/tests/inference` - Test inference capability

### Settings Tasks View

- `GET /api/v1/tasks` - Get all tasks
- `GET /api/v1/tasks/{taskId}` - Get task details
- `PUT /api/v1/tasks/{taskId}` - Update task configuration
- `DELETE /api/v1/tasks/{taskId}` - Delete task
- `POST /api/v1/tasks` - Create new task
- `GET /api/v1/collections` - Get all task collections
- `GET /api/v1/collections/{collectionId}` - Get collection details
- `PUT /api/v1/collections/{collectionId}` - Update collection configuration
- `DELETE /api/v1/collections/{collectionId}` - Delete collection
- `POST /api/v1/collections` - Create new collection
- `GET /api/v1/collections/{collectionId}/tasks` - Get tasks in a collection
- `POST /api/v1/collections/{collectionId}/tasks` - Add task to collection
- `DELETE /api/v1/collections/{collectionId}/tasks/{taskId}` - Remove task from collection



# Backend Controllers Drafts

### ProvidersController Interface
**File Name:** `ProvidersController.java`  
**Interface Name:** `ProvidersController`  
**Rationale:** This interface consolidates all endpoints related to provider management (CRUD operations) and provider-specific testing (model retrieval and inference tests). Grouping these under a single controller aligns with RESTful resource ownership principles, where `/providers` is a top-level resource. The inclusion of test endpoints (`/tests/models`, `/tests/inference`) under provider-specific paths ensures contextual testing tied to individual providers.

```java
public interface ProvidersController {
    @GetMapping("/api/v1/providers")
    ResponseEntity<Object> getAllProviders();

    @GetMapping("/api/v1/providers/{providerId}")
    ResponseEntity<Object> getProviderDetails(
        @PathVariable String providerId,
        @RequestParam(name = "includeModels", required = false) Boolean includeModels
    );

    @PutMapping("/api/v1/providers/{providerId}")
    ResponseEntity<Object> updateProvider(
        @PathVariable String providerId,
        @RequestBody Object providerConfig
    );

    @PostMapping("/api/v1/providers")
    ResponseEntity<Object> createProvider(@RequestBody Object providerConfig);

    @DeleteMapping("/api/v1/providers/{providerId}")
    ResponseEntity<Object> deleteProvider(@PathVariable String providerId);

    @PostMapping("/api/v1/providers/{providerId}/tests/models")
    ResponseEntity<Object> testModelRetrieval(
        @PathVariable String providerId,
        @RequestBody Object testRequest
    );

    @PostMapping("/api/v1/providers/{providerId}/tests/inference")
    ResponseEntity<Object> testInferenceCapability(
        @PathVariable String providerId,
        @RequestBody Object testRequest
    );
}
```

---

### TasksController Interface
**File Name:** `TasksController.java`  
**Interface Name:** `TasksController`  
**Rationale:** This interface handles standalone task operations (CRUD), independent of collections. Separating tasks from collections ensures single-responsibility design, as tasks can exist independently (e.g., `/api/v1/tasks` returns all tasks regardless of collections). This aligns with REST sub-resource patterns where tasks are primary resources, and collection associations are managed separately.

```java
public interface TasksController {
    @GetMapping("/api/v1/tasks")
    ResponseEntity<Object> getAllTasks();

    @GetMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<Object> getTaskDetails(@PathVariable String taskId);

    @PutMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<Object> updateTask(
        @PathVariable String taskId,
        @RequestBody Object taskConfig
    );

    @DeleteMapping("/api/v1/tasks/{taskId}")
    ResponseEntity<Object> deleteTask(@PathVariable String taskId);

    @PostMapping("/api/v1/tasks")
    ResponseEntity<Object> createTask(@RequestBody Object taskConfig);
}
```

---

### CollectionsController Interface
**File Name:** `CollectionsController.java`  
**Interface Name:** `CollectionsController`  
**Rationale:** This interface manages task collections and their relationships with tasks. Collection-specific endpoints (e.g., `/collections/{id}/tasks`) are grouped here to enforce resource hierarchy: collections own task associations. This avoids bloating the `TasksController` with collection-scoped logic and adheres to REST sub-resource conventions (e.g., `POST /collections/{id}/tasks` adds a task to a collection).

```java
public interface CollectionsController {
    @GetMapping("/api/v1/collections")
    ResponseEntity<Object> getAllCollections();

    @GetMapping("/api/v1/collections/{collectionId}")
    ResponseEntity<Object> getCollectionDetails(@PathVariable String collectionId);

    @PutMapping("/api/v1/collections/{collectionId}")
    ResponseEntity<Object> updateCollection(
        @PathVariable String collectionId,
        @RequestBody Object collectionConfig
    );

    @DeleteMapping("/api/v1/collections/{collectionId}")
    ResponseEntity<Object> deleteCollection(@PathVariable String collectionId);

    @PostMapping("/api/v1/collections")
    ResponseEntity<Object> createCollection(@RequestBody Object collectionConfig);

    @GetMapping("/api/v1/collections/{collectionId}/tasks")
    ResponseEntity<Object> getCollectionTasks(@PathVariable String collectionId);

    @PostMapping("/api/v1/collections/{collectionId}/tasks")
    ResponseEntity<Object> addTaskToCollection(
        @PathVariable String collectionId,
        @RequestBody Object taskRequest
    );

    @DeleteMapping("/api/v1/collections/{collectionId}/tasks/{taskId}")
    ResponseEntity<Object> removeTaskFromCollection(
        @PathVariable String collectionId,
        @PathVariable String taskId
    );
}
```

---

### RunController Interface
**File Name:** `RunController.java`  
**Interface Name:** `RunController`  
**Rationale:** This interface centralizes all benchmark run operations, including lifecycle management (`start`, `continue`, `stop`), task associations, and report generation. Grouping these under `/runs` maintains resource cohesion, as runs are the central entity for benchmark execution. Report endpoints (`/reports/summary`, `/reports/details`) are nested under runs to reflect their dependency on run context. Query parameters (e.g., `status`, `format`) are explicitly declared to handle filtering and output customization.

```java
public interface RunController {
    @PostMapping("/api/v1/runs")
    ResponseEntity<Object> startBenchmarkRun(@RequestBody Object runConfig);

    @GetMapping("/api/v1/runs")
    ResponseEntity<Object> getBenchmarkRuns(
        @RequestParam(name = "status", required = false) List<String> status
    );

    @GetMapping("/api/v1/runs/{runId}/tasks")
    ResponseEntity<Object> getRunTasks(@PathVariable String runId);

    @PostMapping("/api/v1/runs/{runId}/continue")
    ResponseEntity<Object> continueBenchmarkRun(@PathVariable String runId);

    @GetMapping("/api/v1/runs/{runId}/reports/summary")
    ResponseEntity<Object> getSummaryReport(
        @PathVariable String runId,
        @RequestParam(name = "format", required = false) String format
    );

    @GetMapping("/api/v1/runs/{runId}/reports/details")
    ResponseEntity<Object> getDetailedReport(
        @PathVariable String runId,
        @RequestParam(name = "format", required = false) String format
    );

    @PostMapping("/api/v1/runs/{runId}/stop")
    ResponseEntity<Object> stopBenchmarkRun(@PathVariable String runId);
}
```
