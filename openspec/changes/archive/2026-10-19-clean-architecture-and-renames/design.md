# Design

## Context

See proposal.md for motivation. The project is built with Amper (`module.yaml`, `project.yaml`), not Gradle.

- `core` holds the DTOs, `ApiPaths`, and `PortfolioApi` with its Ktor engines. `server` depends on `core`; `server` tests also use `adminShared` and `PortfolioApi` (`ContentTestSupport.kt`, `TestSupport.kt`).
- `clientShared` view models take `PortfolioApi` directly; `ClientApp` builds the API and `HttpClient` with `remember {}`.
- `adminShared` builds `AdminSession` (token state, client, API) in `AdminApp` and exposes it through `LocalAdminSession`.
- Shells pass the server URL and platform values into the shared apps by hand.

## Goals / Non-Goals

**Goals:**
- Presentation code never imports Ktor or `PortfolioApi`; it depends on repository interfaces.
- All object graphs are built by Koin; shells only start Koin and provide platform values.
- Existing tests keep passing, with fakes supplied through Koin overrides or constructor arguments.

**Non-Goals:**
- No change to the `server` module's structure or tests beyond imports.
- No behavior or UI changes.
- No DTO-to-domain mappers, and no per-action use-case classes.
- No new modules per layer; layers are packages.

## Decisions

**1. Renames land first, alone.** `git mv` for the three folders plus every reference (`project.yaml`, dependent `module.yaml` files, README, `.idea` files, iOS project files, `.gitignore`). Alternative: rename during the architecture work. Rejected because rename-only commits keep history readable.

**2. Keep identifiers that are not folder names.** Android `applicationId`/`namespace` and the desktop `mainClass` do not change. Alternative: rename them too. Rejected: it changes the installed app's identity for no layout benefit.

**3. New `app/sharedData` module (kmp lib: jvm, android, iosArm64, iosSimulatorArm64, wasmJs).** It takes `PortfolioApi`, `createHttpClient`, `ApiException` and the per-platform engine dependencies from `core`. `core` keeps DTOs and `ApiPaths` plus serialization. The name mirrors `sharedUi`. Alternative: leave `PortfolioApi` in `core`. Rejected because it mixes the contract with a data source. The `core/test` `PortfolioApiTest` moves with it. `server` test support gains a `sharedData` test dependency.

**4. Layers as packages inside `clientShared` and `adminShared`.**

```
domain/        repository interfaces (+ HomeData aggregate); DTOs used as models
data/          repository implementations over PortfolioApi (admin: also AdminSession)
presentation/  screens, view models, navigation
di/            Koin modules
```

Dependencies point inward: `presentation` -> `domain` <- `data`. The client repository is read-only; the admin repository covers read and write. Alternatives: separate modules per layer (rejected, more build surface for a small app); use-case classes (rejected, each action is one repository call).

**5. Koin.** Add `koin-core`, `koin-compose` and `koin-compose-viewmodel` to the version catalog (jvm, android, ios and wasmJs are supported). Each shared module exposes `clientModule` / `adminModule`. View models are registered with the `viewModel` DSL and obtained with `koinViewModel`. Parameters such as a project id are passed with `parametersOf`. The shared root composable (`ClientApp` / `AdminApp`) creates an isolated Koin with `koinApplication` and provides it with `KoinIsolatedContext`, closing it on dispose. The shells pass the server URL, theme store and `ClientPlatform` as arguments, and the root turns them into a platform module. The compose `KoinApplication` and `startKoin` are avoided on purpose: they use one global Koin that is reused, so a second composition (a test, or an Android activity recreate) would silently keep the first one's modules and HTTP client. Alternative: manual DI through constructors. Rejected: this is what the requirement asks for, and it removes the `remember`/`CompositionLocal` wiring.

**6. `AdminSession` stays** as the login-state holder, registered as a singleton and injected into the admin repository and the root composable, replacing `LocalAdminSession`. The 401-logs-out behavior is unchanged.

**7. Order of work.** Renames, then `sharedData` extraction, then client, then admin. Each step builds and passes tests before the next starts.

## Risks / Trade-offs

- [Koin lifecycle on wasmJs and iOS differs from jvm] -> Start Koin once per shell and smoke-test each target that can be run locally.
- [Koin global-state reuse across compositions] -> Use an isolated `koinApplication` per root composition (decision 5); covered by a wiring test and by the `ClientApp` UI tests, which failed under the global `KoinApplication`.
- [DTOs as domain models couples presentation to wire format] -> Accepted deliberately; the repository interface is the seam if mappers are ever needed.
- [`server` tests rely on `PortfolioApi` and `adminShared`] -> Update dependencies and imports, and run the server tests as a check.
- [iOS project file edits are hand-made] -> Verify with an iOS simulator build, or note it as unverified if no Xcode run is possible.
