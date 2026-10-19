# Tasks

## 1. Koin on the server

- [x] 1.1 Add `koin-ktor` and `koin-core` to `libs.versions.toml` and `server/module.yaml`; verify `./kotlin build -m server` resolves and compiles with Ktor 3.6.0
- [x] 1.2 Add a wiring test that installs `KoinIsolated` in two `testApplication`s with different definitions and asserts they see different instances; verify it passes

## 2. Foundation: domain types, errors and Koin replace ServerContext

- [x] 2.1 Create the `domain` package with `ImageFormat`, `AspectRatio`, `ImageRecord`, `Validation`, `Clock`, the domain error types and `ServerConfig` (in `config`), moved from their current files; verify the build and add unit tests for `AspectRatio` and `Validation`
- [x] 2.2 Replace `ApiError` / `badRequest` / `notFound` with domain errors and map them in `api/Errors.kt` to the same statuses and `ErrorDto` bodies; verify `./kotlin test -m server` passes unchanged
- [x] 2.3 Add `di/ServerModules.kt` (functions, not top-level values) providing the existing classes, install `KoinIsolated` in `main` and in the test helper `withServer`, and delete `ServerContext` and `createContext`; verify the HTTP tests pass and `Database` is closed by Koin when the application stops

## 3. Auth slice

- [x] 3.1 Add `AuthTokenRepository` (interface in `domain`, SQLite in `data`), `PasswordHasher` (PBKDF2 in `data`), `AuthService` and `LoginThrottle` in `service`; verify unit tests with fakes cover login, token expiry and throttling
- [x] 3.2 Move the login route to `api/LoginRoute.kt`, have the bearer plugin and the image route's owner check use `AuthService`, and delete `OwnerAuth`; verify `AuthTest` and the HTTP tests pass

## 4. Content slice (profile, links, projects)

- [x] 4.1 Add `Transactor` and the repository interfaces in `domain`, and the SQLite implementations in `data` (split from `ContentRepositories.kt`, with `Connection` private to `data`); verify the existing database and rollback tests pass
- [x] 4.2 Add `ProfileService`, `LinkService` and `ProjectService` holding the validation and ordering rules, with unit tests over fake repositories; verify the tests pass
- [x] 4.3 Replace the content routes with `api` route files taking services, and delete `ContentRoutes.kt` and `ContentRepositories.kt`; verify the profile, links and projects HTTP tests pass unchanged

## 5. Images slice

- [x] 5.1 Add `ImageRepository` (including the referenced/unreferenced SQL), `ImageFiles` (file storage) and `ImageProcessor` (AWT decode and thumbnails) behind interfaces; verify the build
- [x] 5.2 Add `ImageService` (sniff, validate, store, rollback of files on failure, serve rules) and `ImageCleanupService` with the scheduler, with unit tests over fakes; verify they pass
- [x] 5.3 Replace `ImageRoutes.kt` with `api` routes taking `ImageService`, delete `ImageStore`, `ImageCleanup` and `ImageModel.kt`, and rewrite `ImageRatioTest`, `ImageUploadTest`, `ImageCleanupTest` and `CleanupScheduleTest` to fetch services from Koin; verify all image HTTP tests pass

## 6. Finish

- [x] 6.1 Remove dead code and unused imports, confirm no file in `domain` or `service` imports Ktor or `java.sql`, and confirm no file outside `data` imports `java.sql`; verify with grep
- [x] 6.2 Add a Koin wiring test for the server modules, and a README section describing the server layers and the `KoinIsolated` rule; verify the README matches the packages
- [x] 6.3 Run the full `./kotlin build` and `./kotlin test` and start the server against the real database to confirm it serves the profile; verify all tests pass and the endpoint responds
