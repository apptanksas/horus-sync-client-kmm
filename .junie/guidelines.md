# Horusync Client KMM - Project Guidelines

## Project Overview
Horusync Client is a Kotlin Multiplatform (KMM) library designed to facilitate local data storage and seamless synchronization with a remote server (typically powered by the Horus PHP SDK). It ensures data integrity and security while providing features like offline support, file uploads, and entity-level restrictions.

### Tech Stack
- **Languages**: Kotlin (Multiplatform)
- **Database**: [SQLDelight](https://cashapp.github.io/sqldelight/) for local SQLite storage.
- **Networking**: [Ktor](https://ktor.io/) for HTTP requests.
- **Serialization**: `kotlinx-serialization`.
- **Date/Time**: `kotlinx-datetime`.
- **Testing**: [Mokkery](https://mokkery.dev/) for mocking and Robolectric for Android unit tests.
- **Dependency Injection**: Internal `HorusContainer` (Service Locator pattern).

## Project Structure
The project follows a standard Kotlin Multiplatform structure, centered around the `:client` module.

- **`:client`**: The primary module containing all library logic.
    - `src/commonMain`: Core logic, database helpers, synchronization managers, and the public `HorusDataFacade`.
    - `src/androidMain` / `src/iosMain`: Platform-specific implementations (e.g., SQLite drivers, life-cycle observers, network validators).
    - `src/commonTest`: Shared unit and integration tests.
    - `src/androidUnitTest`: Android-specific unit tests (running with Robolectric).

## Development Guidelines

### 1. Code Style & Standards
- Strictly follow the existing Kotlin coding style in the project.
- All code, comments, logs, and documentation must be in English. No Spanish text is allowed.
- Use **Spaces** (4) for indentation, consistent with the existing codebase.
- Keep the `HorusDataFacade` as the single entry point for end-users whenever possible.

### 2. Working with Data
- **Data Map**: Records are handled using `DataMap` (typealias for `Map<String, Any?>`).
- **IDs**: IDs are always `UUID` strings generated internally by Horus. Do not pass manual IDs in `insert` operations.
- **SQL Operations**: Use the `QueryBuilder` implementations (e.g., `SimpleQueryBuilder`, `JoinableQueryBuilder`, `UnionQueryBuilder`) for database interactions.

### 3. Testing Requirements
- **Mandatory Testing**: Before submitting any logic changes, run the relevant tests.
- **How to Run Tests**:
    - All tests: `./gradlew :client:allTests`
    - Common tests: `./gradlew :client:commonTest`
    - Android unit tests: `./gradlew :client:testDebugUnitTest`
- **Mokkery**: Use Mokkery for creating mocks in tests. Note that Mokkery requires the Kotlin plugin to be enabled.

### 4. Build & Environment
- The project uses **Java 17**.
- **Windows environment**: Ensure commands use PowerShell syntax (`;` for chaining, `\` for paths).
- **macOS Requirement**: iOS-related tasks (compilation/tests) can only be executed on macOS. On Windows, they are automatically disabled in the Gradle build.

### 5. Documentation & Versioning
- **README**: Update `readme.md` if the public API changes or new features are added.
- **Guidelines**: These guidelines (`.junie/guidelines.md`) should be kept up to date as the project evolves.

## Synchronization Architecture
- **SyncControlDatabaseHelper**: Manages the `horus_sync_control` and `horus_queue_actions` tables.
- **SynchronizatorManager**: Handles the flow of pulling changes from the server and pushing local pending actions.
- **Tasks**: Long-running operations are encapsulated in `BaseTask` implementations (e.g., `SynchronizeDataTask`, `RefreshReadableEntitiesTask`).
