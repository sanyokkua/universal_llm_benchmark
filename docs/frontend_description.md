# Application UI Structure

## Global Elements (Present Across All Screens)

### Top Bar
- **Title**:
    - "Dashboard" on all dashboard screens
    - "Run of Benchmark" on benchmark execution screen
    - "Settings" on all settings screens
- **Navigation Button**:
    - "Settings" button (top-right) on dashboard and benchmark screens
    - "Back to Dashboard" button (top-right) on all settings screens

## Dashboard State (Main Application View)

### Common Elements
- **Navigation Tabs**: Three horizontal tabs for switching between dashboard views:
    - "Run"
    - "Continue"
    - "Results"

### 1. Run Tab (Benchmark Setup)
- **Three-Column Layout**:
    - **Left Column ("Select Judge")**:
        - Provider sections (e.g., "Provider Name 1", "Provider Name 2")
        - Each provider has model list with checkboxes
        - "Chosen: Provider-Model" status text at bottom
    - **Middle Column ("Select Models")**:
        - Provider sections with model lists and checkboxes
    - **Right Column ("Select Tasks")**:
        - Task collection sections (e.g., "Task Collection 1", "Task Collection 2")
        - Each collection has tasks with "View" buttons and checkboxes
- **Action Buttons**:
    - "Reload" (bottom-left)
    - "Start New Benchmark" (bottom-center)

### 2. Continue Tab (Benchmark Management)
- **Two-Column Layout**:
    - **Left Column ("Runs" Table)**:
        - "Name" and "Status" columns
        - Rows for each benchmark run with status values (e.g., "New", "Finish", "Pending", "Error")
    - **Right Column ("Tasks" Table)**:
        - "Name" and "Status" columns
        - Rows for each task with status values (e.g., "Pending", "Judge", "Error", "Reval", "Finish")
- **Action Button**:
    - "Continue" (bottom-center)

### 3. Results Tab (Benchmark Results)
- **Two-Column Layout**:
    - **Left Column ("Runs" Table)**:
        - "Name" and "Status" columns
        - Rows for completed benchmark runs with status values (e.g., "Err", "New", "Finish")
    - **Right Column (Results Viewer)**:
        - Nested tabs: "Summary" (default) and "Detailed"
        - Data grid showing benchmark results
        - Export buttons: "Export CSV" and "Export MD" at bottom
- **Action Buttons**:
    - Export functionality in results viewer

## Run of Benchmark State (Active Benchmark Execution)

### Screen Layout
- **Two-Column Layout**:
    - **Left Column (Status Panel)**:
        - "Status: [value]" (e.g., "Running")
        - "Tasks Total: [number]"
        - "Processed: [number]"
        - "Current task: [name]"
        - "Current Model: [provider/name]"
        - "Time: [duration]"
    - **Right Column (Log Panel)**:
        - Task entries (e.g., "Task 1", "Task 2")
        - Each task shows:
            - "Q: [question text]"
            - "Act: [action text]"
- **Central Control**:
    - "Stop" button between columns

## Settings State (Configuration)

### Common Elements
- **Navigation Tabs**: Two horizontal tabs:
    - "Providers"
    - "Tasks"

### 1. Providers Management
- **Providers List View**:
    - Table with "Name", "Edit", "Del" columns
    - Rows for each provider (e.g., "Ollama", "LM Studio", "Open AI")
    - "Add New" button at bottom
- **Provider Configuration View**:
    - "Provider Name" text field
    - "Base URL" text field
    - "Models List" text field
    - "Inference" text field
    - "Headers" section with key-value pairs
    - "URL Params" section with key-value pairs
    - "Test Models URL" with test button and response field
    - "Test Inference" with test button and response field
    - "Cancel" and "Save" buttons at bottom

### 2. Tasks Management
- **Tasks List View**:
    - Left: "Tasks" table with "Name", "Edit", "Del" columns
    - Right: "Collections" table with "Name", "Edit", "Del" columns
    - "Add" buttons below each table
- **Task Configuration View**:
    - "Task Name" text field
    - "Category" text field
    - "Sub Category" text field
    - Additional configuration fields
    - "Cancel" and "Save" buttons at bottom
- **Task Collection Configuration View**:
    - "Collection Name" text field
    - Left section: "Tasks All" list with "+" buttons
    - Right section: "Added" tasks list with "X" buttons for removal
    - "Cancel" and "Save" buttons at bottom

## Key Application Flow
1. **Dashboard → Run Tab**: Configure benchmark (select judge, models, tasks)
2. **Start New Benchmark**: Transitions to "Run of Benchmark" state
3. **Run of Benchmark**: Monitor execution, stop if needed
4. **Dashboard → Continue Tab**: Resume or manage ongoing benchmarks
5. **Dashboard → Results Tab**: View completed benchmark results
6. **Settings**: Configure providers and tasks used in benchmarks
    - "Providers" tab manages API connections
    - "Tasks" tab manages benchmark tasks and collections

## Structural Patterns
- **Tab-based navigation** is used at multiple levels (main app navigation, settings navigation)
- **Table-based data presentation** is used for runs, tasks, providers, and collections
- **Form-based configuration** for detailed settings with consistent "Cancel"/"Save" pattern
- **Two-column layout** is standard for information display and management
- **Action buttons** consistently appear at bottom of relevant sections
- **Status indicators** are used throughout to show current state of operations
