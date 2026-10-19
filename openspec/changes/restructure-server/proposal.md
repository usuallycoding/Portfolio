# Proposal

## Why

The server is one flat package where a `ServerContext` object holding every service is passed to every route, routes manage transactions and pass raw `java.sql.Connection`s to repositories, and the HTTP error type (`ApiError`, built on Ktor's `HttpStatusCode`) is thrown from repositories and the image store. Several files do more than one job (`ImageStore`, `OwnerAuth`, `ImageModel.kt`, the login logic inside `Application.kt`). The client and admin apps were just restructured into layers with Koin; the server should follow the same principles.

## What Changes

- Split `server` into packages: `api` (Ktor routes, plugins, error mapping), `service` (validation and business rules), `domain` (repository interfaces, ports, domain types and errors, `Transactor`), `data` (SQLite repositories, file storage, image processing, password hashing), and `di` (Koin modules).
- Remove `ServerContext` and `createContext`. Dependencies are passed by constructor and wired by Koin, installed with the `KoinIsolated` plugin so each Ktor application (including each test application) has its own Koin.
- Replace `ApiError` with domain errors that carry no HTTP types; the `api` layer maps them to the same status codes and bodies as today.
- Introduce a `Transactor` in `domain` so a unit of work can span several repositories atomically while `java.sql.Connection` stays inside `data`.
- Split the oversized files: the repositories, the image SQL predicates, token persistence, file storage, AWT image processing and password hashing each get their own class behind an interface where the service layer needs one.
- The `core` DTOs remain the models, with no mappers, as in the apps.
- No behavior changes: every HTTP route, status code, response body and the image cleanup schedule stay as they are.
- Tests that reach into server internals are rewritten against the new classes (fetching them from Koin); the HTTP-level tests are unchanged and stay green through every step. New unit tests cover the services with fake repositories, plus a Koin wiring test.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. This is a pure refactor, so the change sets `skip_specs: true`.

## Impact

- Code: everything under `server/src` and the `server/test` files that touch internals.
- Build: `libs.versions.toml` and `server/module.yaml` gain `koin-ktor` (and `koin-core`).
- Docs: the README gains a short server architecture section.
- Unchanged: the `core` module, the apps, the HTTP API and the database schema.
