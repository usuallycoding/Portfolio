# Proposal

## Why

The client and admin apps have no layering: view models call `PortfolioApi` (an HTTP client) directly, and dependencies are built by hand with `remember {}` and a `CompositionLocal`. Three app folders (`androidApp`, `iosApp`, `desktopApp`) also break the `client*` / `admin*` naming that `clientWebApp` and `adminWebApp` already follow.

## What Changes

- Rename `app/androidApp` to `app/clientAndroidApp`, `app/iosApp` to `app/clientIosApp`, and `app/desktopApp` to `app/adminDesktopApp`, including everything that references them. Done first, as its own commit.
- Add a new `app/sharedData` module that takes `PortfolioApi`, `createHttpClient`, `ApiException` and the per-platform Ktor engines out of `core`. `core` keeps only the DTOs and `ApiPaths`, which the server also uses.
- Restructure `app/clientShared` and `app/adminShared` into `domain`, `data`, `presentation` and `di` packages. Repository interfaces live in `domain`, implementations over `PortfolioApi` live in `data`, and screens and view models in `presentation` depend only on `domain`.
- DTOs are used directly as domain models. There are no mappers and no use-case classes.
- Introduce Koin for dependency injection. Each shared module exposes its Koin modules, and each shell starts Koin and supplies platform values (server URL, theme store, platform kind). This replaces `remember { PortfolioApi(...) }` and `LocalAdminSession`.
- No user-visible behavior changes.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
None. This is a pure refactor and renaming change, so the change sets `skip_specs: true`.

## Impact

- Modules: `core`, new `app/sharedData`, `app/sharedUi`, `app/clientShared`, `app/adminShared`, and all five shells.
- Build: `project.yaml`, every `module.yaml` that depends on a renamed module, `libs.versions.toml` (Koin).
- iOS: `project.pbxproj`, the Swift entry point, `.idea` module and run configuration files, `.gitignore`.
- Docs: README module table and run commands.
- Tests: client, admin and `core` tests move with their code; fakes are provided through Koin module overrides. Server code and tests are untouched, apart from any import changes caused by `PortfolioApi` leaving `core`.
- Out of scope: restructuring the `server` module, which gets its own change.
