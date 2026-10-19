# Design

## Context

See proposal.md for motivation. Today's server (`server/src`, one package, 13 files):

- `Application.kt` builds a `ServerContext` (config, database, clock, auth, throttle, images, cleanup), stores it in an application attribute, and mounts routes that take the whole context.
- Routes call `db.transaction { repos.x.y(conn, ...) }`; repositories take a `Connection` and also validate input.
- `ApiError(HttpStatusCode, message)` is thrown from repositories, `ImageStore` and `Images.requireForAttach`.
- `ImageStore` validates, decodes and thumbnails with AWT, writes files and inserts the DB row. `ImageCleanup` deletes unreferenced images and stray files and is scheduled by `runPeriodically`. `OwnerAuth` hashes the password and persists token hashes with SQL. `LoginThrottle` lives in the same file.
- Tests use `createContext(config, clock)` and `module(context)`; about eight files read internals directly.

The apps already follow the same principles (see the archived `clean-architecture-and-renames` change): repository interfaces, DTOs as models, Koin, isolated per root.

## Goals / Non-Goals

**Goals:**
- Routes depend on services; services depend on `domain` only; `data` implements `domain`. No Ktor type below `api`, no `Connection` outside `data`.
- One wiring place (Koin) with test overrides for the clock and config.
- Unchanged HTTP behavior and database schema, proven by the existing HTTP-level tests staying green at every step.

**Non-Goals:**
- No new modules, no change to `core`, the apps, the API or the schema.
- No new ORM or query library; the SQL stays hand-written JDBC.
- No new features (for example token refresh or different throttling).

## Decisions

**1. Packages inside `server`, as in the apps.** `api`, `service`, `domain`, `data`, `di`, plus `config` for `ServerConfig`. Alternative: a module per layer. Rejected as before: more build surface for a small server.

**2. DTOs from `core` remain the models; no mappers.** The server already returns them straight from its repositories. Services take the `*Input` DTOs and return the DTOs.

**3. A service layer, unlike the apps.** The server has real rules (validation, aspect ratios, throttling, token lifetime, cleanup policy), so `ProfileService`, `LinkService`, `ProjectService`, `ImageService`, `AuthService` and `ImageCleanupService` own them. Repositories become plain data access. Alternative: keep rules in repositories. Rejected: that is the current mixing.

**4. `Transactor` keeps `Connection` inside `data`.** `domain` declares `interface Transactor { suspend fun <T> transaction(block: Repositories.() -> T): T }`, where `Repositories` bundles the repository interfaces. `data` implements it by running the block on the IO dispatcher under the single-connection lock and binding the repositories to that connection, so a unit of work such as "validate images then save a project" stays atomic. The block is non-suspending because it runs while holding the lock. Alternative: one transaction per repository call. Rejected: it would break atomicity across image checks and writes.

**5. Domain errors replace `ApiError`.** `Invalid(message)`, `NotFound(message)`, `PayloadTooLarge`, `UnsupportedImage`, `TooManyAttempts`, `Unauthorized`. `api/Errors.kt` maps them in `StatusPages` to exactly today's statuses (400, 404, 413, 415, 429, 401) and `ErrorDto` bodies. The `ApiError` helpers (`badRequest`, `notFound`) are replaced by domain throws.

**6. Ports for I/O the services must not know about.** `ImageFiles` (write, read, delete, list stored files), `ImageProcessor` (decode and thumbnail, AWT in `data`), `PasswordHasher` (PBKDF2 in `data`). This lets service tests use fakes.

**7. Koin with the isolated Ktor plugin.** `install(KoinIsolated) { modules(serverModules(config, clock)) }`. The plain `Koin` plugin calls `startKoin` on the global context and stops an existing one, so it cannot give each test application its own instance. Modules are built by functions, never top-level values, because a Koin `Module` owns its singleton instances. `Database` is registered with `onClose { it.close() }`, replacing the manual close in tests. Constructor injection only; `inject()` is used only where routes are mounted.

**8. Auth stays a Ktor plugin; token logic moves to `AuthService`.** The bearer `authenticate` block calls `AuthService.isValid`. The public image route's "is this the owner" check calls the same method instead of re-parsing the header itself.

**9. Cleanup scheduling stays on the application's coroutine scope.** `ImageCleanupService.run()` holds the logic; `runPeriodically` and `startImageCleanup` move to `api`/`service` unchanged in behavior.

**10. Order of work: vertical slices, tests green throughout.** (a) foundation: domain types, errors and Koin replacing `ServerContext`; (b) auth; (c) content (profile, links, projects) with the `Transactor`; (d) images and cleanup; (e) tidy-up. Each slice moves its code and its tests together.

## Risks / Trade-offs

- [Atomicity lost when splitting repositories] -> `Transactor` runs the whole unit under the one connection lock; existing tests for rollback behavior stay and run each slice.
- [Image upload writes files and a row and cleans up on failure] -> Keep the same order (files, then row, delete files on any failure) inside `ImageService`; the upload and cleanup tests cover it.
- [Koin Ktor plugin behavior differs from expectations] -> A first task adds a wiring test proving two test applications get separate Koin instances before any real wiring depends on it.
- [`koin-ktor` is built against a Ktor version close to the one in use (3.6.0)] -> Resolve and compile it in the first task; if it is incompatible, stop and report rather than pin an older Ktor.
- [Large diff] -> Slices keep each step reviewable; no behavior change is allowed, so any failing HTTP test means a bug in the move.
- [Tests coupled to internals] -> Rewrite them to fetch services from Koin or to run as unit tests over fakes, never to assert SQL where behavior can be asserted.
