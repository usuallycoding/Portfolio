# Tasks

## 1. Renames

- [x] 1.1 `git mv` `app/androidApp` -> `app/clientAndroidApp`, `app/iosApp` -> `app/clientIosApp`, `app/desktopApp` -> `app/adminDesktopApp`, and update `project.yaml`; verify with `./kotlin build` that every module still resolves
- [x] 1.2 Update iOS references (`project.pbxproj`, Swift entry point, `.idea` iml and run configurations, `.gitignore` pattern); verify with `grep -r iosApp` showing only intended leftovers and an iOS build if Xcode is available
- [x] 1.3 Update the README module table and run commands (`-m clientAndroidApp`, `-m clientIosApp`, `-m adminDesktopApp`); verify the documented commands match module names
- [x] 1.4 Run `./kotlin test` and commit the renames on their own

## 2. sharedData module

- [x] 2.1 Create `app/sharedData` (kmp lib) and add it to `project.yaml`; move `PortfolioApi`, `createHttpClient`, `ApiException` and the engine dependencies out of `core`; verify `core` contains only DTOs and `ApiPaths`
- [x] 2.2 Move `PortfolioApiTest` to `sharedData`, update `clientShared`, `adminShared`, and `server` test dependencies and imports; verify `./kotlin test` passes for `core`, `sharedData` and `server`

## 3. Koin and client layering

- [x] 3.1 Add Koin to `libs.versions.toml` and the shared modules' `module.yaml` files; verify the dependencies resolve on every target
- [x] 3.2 In `clientShared`, add the `domain` repository interface and `data` implementation, move screens and view models into `presentation`, and build `di` modules; verify no `presentation` file imports Ktor or `PortfolioApi`
- [x] 3.3 Update client tests for the new packages, run view-model tests over the repository with a mock HTTP engine, and add a Koin wiring test; verify `./kotlin test -m clientShared` passes
- [x] 3.4 Start Koin in `clientAndroidApp`, `clientIosApp` and `clientWebApp` with their platform modules; verify each shell builds and the client app loads data from a running server

## 4. Admin layering

- [x] 4.1 In `adminShared`, add the `domain` repository interface and `data` implementation, inject `AdminSession` through Koin in place of `LocalAdminSession`, move screens and view models into `presentation`, and build `di` modules; verify no `presentation` file imports Ktor or `PortfolioApi`
- [x] 4.2 Update admin tests for the new packages, run view-model tests over the repository with a mock HTTP engine, and add a Koin wiring test; verify `./kotlin test -m adminShared` passes, including the 401-logs-out behavior
- [x] 4.3 Start Koin in `adminDesktopApp` and `adminWebApp` with their platform modules; verify login, editing and image upload work against a running server

## 5. Integration

- [x] 5.1 Update the README with the architecture and DI description; verify it matches the module layout
- [x] 5.2 Run the full `./kotlin build` and `./kotlin test`, and confirm no `server` source changes beyond imports/dependencies
