# Tasks

## 1. Module restructure and dependencies

- [x] 1.1 Create the module layout from design.md (client shared, admin shared, shared-ui, client web, admin web) and update `project.yaml`; verify `./kotlin build` succeeds with placeholder screens
- [x] 1.2 Point shells at the right modules (Android, iOS, client web -> client; desktop, admin web -> admin); verify each shell launches its own placeholder
- [x] 1.3 Add and verify dependencies compile for their targets: Ktor server plugins (content negotiation, auth, multipart, caching headers), SQLite access, Ktor client engines, image loading; verify a build for jvm, android, ios and wasmJs
- [x] 1.4 Remove the greeting scaffold and sample UI; verify no references remain

## 2. Shared contract (core)

- [x] 2.1 Define DTOs for profile, link, project, image and login with serialization; verify round-trip serialization tests on jvm
- [x] 2.2 Define API paths and the `ApiClient` (public and admin calls, bearer token support); verify against a test server in a core or server test

## 3. Server persistence and auth

- [x] 3.1 Create SQLite schema and migrations for profile, images, links, projects, project_images, auth_tokens; verify a test creates a fresh database and enforces foreign keys
- [x] 3.2 Implement owner login, token table with expiry, admin route guard and login throttling; verify tests for correct password, wrong password, missing and expired token, throttling and refusal to start without a password
- [x] 3.3 Document server configuration (password, database path, images directory) in README and verify the documented command starts the server

## 4. Server image storage

- [x] 4.1 Implement `POST /admin/images` with magic-byte sniffing, size and dimension limits and unique naming; verify tests for valid JPEG/PNG, renamed non-image, oversized file and duplicate names
- [x] 4.2 Generate thumbnail variants (same format, alpha kept) and serve originals and `?size=thumb` with caching headers; verify tests for thumbnail size, PNG transparency and 404 on unknown names
- [x] 4.3 Enforce public visibility on image serving (staged and private-only images return 404); verify tests
- [x] 4.4 Implement attach-time ratio validation with tolerance; verify tests for 1:1, 3:1 and 16:9 accept and reject
- [x] 4.5 Implement the daily orphan cleanup (rows plus both files, plus file-without-row sweep) with an injectable clock; verify tests for 61-minute orphan deleted, 10-minute orphan kept, referenced image kept

## 5. Server content API

- [x] 5.1 Implement profile read with placeholders and admin update including image attach and detach; verify tests for fresh install, update, wrong ratio and replaced image becoming orphan
- [x] 5.2 Implement social links CRUD, visibility filtering and reorder with validation; verify tests for private hidden, reorder, incomplete order and delete
- [x] 5.3 Implement projects CRUD, thumbnail, ordered images, visibility filtering, reorder and delete; verify tests for private 404, image order, incomplete order and delete orphaning images

## 6. Client apps

- [x] 6.1 Build the shared image loading, theme, loading, error and empty components; verify previews or a manual run on Android and web
- [x] 6.2 Build the profile screen (cover, picture, name, description, links with icons, open address); verify with server seeded and with fresh placeholder profile
- [x] 6.3 Build the project list and project detail with full-size image viewing and back navigation; verify on Android, iOS and web against a seeded server
- [x] 6.4 Add retry on network failure and per-app server URL configuration; verify offline state shows retry without crashing

## 7. Admin apps

- [x] 7.1 Build the login gate and token handling with return to login on 401; verify manual run on desktop and web
- [x] 7.2 Implement the platform image codec interface (decode, crop, resize, encode JPEG/PNG) for JVM and wasm; verify unit tests or a manual run on both with a large photo
- [x] 7.3 Build the cropper (fixed-ratio frame, zoom, positioning) and wire profile picture, cover, link icon and project thumbnail to it; verify cropped output has the exact ratio on both platforms
- [x] 7.4 Build profile editing; verify save and reload on desktop and web
- [x] 7.5 Build social links editing with visibility, icon and reordering; verify on desktop and web
- [x] 7.6 Build the upload queue (concurrency 3, per-file progress, per-file retry) and project editing with image reordering and project reordering; verify a 10-file upload with one forced failure, and abandoning a form leaves the project unchanged

## 8. Integration

- [x] 8.1 End-to-end pass: admin edits everything, client apps show the public subset; verify private links and projects never appear and their images return 404
- [x] 8.2 Run the cleanup against a populated dev instance; verify referenced images survive and abandoned uploads are removed
