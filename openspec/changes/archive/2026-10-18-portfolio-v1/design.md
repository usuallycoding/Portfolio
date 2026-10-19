# Design

## Context

The repo is a Kotlin Multiplatform scaffold built with the Kotlin Toolchain (`module.yaml` files, `project.yaml`). Modules today: `core` (kmp lib: jvm, android, ios, wasmJs; one greeting helper), `server` (jvm app, Ktor + Netty, one route), `app/shared` (Compose kmp lib with `App()`), and shells `app/androidApp`, `app/iosApp`, `app/desktopApp`, `app/webApp` (wasm-js). There are no specs or persistence. See proposal.md for motivation.

Constraints: one owner only; SQLite; images on local disk; clients are read-only; admin is desktop and web; clients are Android, iOS and web.

## Goals / Non-Goals

**Goals:**
- One shared contract (DTOs and an API client) used by the server, admin apps and client apps.
- Clear separation of the public read API and the authenticated admin API.
- Image handling that is simple to operate: files plus rows in one SQLite file.

**Non-Goals:**
- Multiple users, roles, or OAuth.
- Image formats beyond JPEG and PNG, or server-side cropping.
- Offline caching and push notifications in clients.
- CDN, object storage, or horizontal scaling of the server.
- Localization of portfolio content.

## Decisions

### Module layout
```
core/                 DTOs, API paths, ApiClient interface (kmp: jvm, android, ios, wasmJs)
server/               Ktor app
app/shared-ui/        small shared Compose pieces (theme, image loading, error/empty states)
app/client/shared/    client screens + view models (kmp: android, ios, wasmJs)
app/admin/shared/     admin screens, cropper, upload queue (kmp: jvm, wasmJs)
app/androidApp, app/iosApp, app/clientWebApp   -> client/shared
app/desktopApp, app/adminWebApp                -> admin/shared
```
Rationale: the platform sets differ, so one shared UI module would force Android/iOS code to compile admin-only code and the reverse. Existing `app/shared` is renamed or split rather than extended. Alternative considered: one app with an admin mode behind login. Rejected: ships editing code to every visitor and conflicts with the stated separation.

### API shape
- Public, no auth: `GET /api/profile`, `GET /api/links`, `GET /api/projects`, `GET /api/projects/{id}`, `GET /images/{fileName}` with optional `?size=thumb`.
- Admin, bearer token: `POST /admin/login` (public by necessity), `PUT /admin/profile`, `/admin/links` CRUD plus `PUT /admin/links/order`, `/admin/projects` CRUD plus `PUT /admin/projects/order`, `POST /admin/images`.
- A project save carries its thumbnail image id and an ordered list of image ids, so attach and ordering are one atomic write.
Rationale: separate prefixes make "everything under /admin requires auth" a single route-level rule. Alternative: one API with role-dependent responses; rejected as easier to leak private data.

### Auth
The password is read from server configuration (environment variable) and compared against a stored hash created at startup. Login returns a random opaque token kept in a server-side table with an expiry. Failed logins are throttled per remote address. Alternative: stateless JWT. Rejected: with one owner, a token table is simpler and permits revocation.

### Persistence
SQLite with tables: `profile` (single row), `images`, `social_links`, `projects`, `project_images`, `auth_tokens`. Order is stored as an integer `position` per list; reorder endpoints take the full ordered list of ids and reject incomplete or duplicate lists. `images` holds `id`, `file_name`, `format`, `width`, `height`, `uploaded_at`. References are foreign keys from profile, links, projects and project_images; an image is an orphan when nothing references it. Alternative: reference counts. Rejected: derived data that can drift.

### Image pipeline (Option B: server-generated thumbnail)
```
POST /admin/images -> sniff magic bytes -> read dimensions only
   -> reject if too big -> decode -> write <uuid>.<ext>
   -> write <uuid>_thumb.<ext> (max edge ~400px, same format, alpha kept)
   -> insert images row (uploaded_at = now)
```
Uses JDK `ImageIO` (JPEG and PNG only, which is why the admin app does not upload WebP). Ratio is not checked at upload because the target field is unknown; it is checked when an image is attached to a field with a ratio, using stored width and height with a small tolerance. Thumbnails are produced synchronously on upload; lazy generation was rejected because it adds races and caching for no gain at this scale.

Public serving checks that the image is reachable from public content (profile, public link, public project); staged and private-only images return 404.

### Orphan cleanup
A coroutine on the server runs once per day and deletes `images` rows with no references and `uploaded_at` older than 60 minutes, then removes both files. It also sweeps files in the images directory that have no row and are older than 60 minutes. Delete the row inside a transaction before removing files so a crash leaves at worst a file the sweep removes.

### Multi-upload
One file per request, concurrency of 3, per-file state (queued, uploading, done, failed), retry per file, then a single project save with ids in order. Alternative: one multipart request with many parts; rejected as all-or-nothing with no per-file progress.

### Cropping and downscaling
A shared Compose cropper (fixed-ratio frame; pinch/scroll zoom and drag) in admin shared code. Decoding, cropping, resizing and encoding sit behind a small `expect/actual`-style interface per platform (JVM and wasm). Output is capped to a maximum edge (about 2000px) and encoded as JPEG or PNG; PNG is kept for images with transparency.

### Client networking and images
A shared `ApiClient` in `core` built on Ktor client with platform engines. Clients load images through a Compose image-loading library that works on Android, iOS and wasm; thumbnails for lists and avatars, originals for covers and full-screen viewing. Server base URL comes from per-app configuration.

### Adjustments made during implementation
- **Module names.** Module names come from directory names, so the layout is `app/sharedUi`, `app/clientShared`, `app/adminShared`, `app/clientWebApp`, `app/adminWebApp` (not nested `shared` folders). `clientShared` also targets `jvm`, which lets view-model and Compose UI tests run headlessly on the JVM.
- **Extra admin read routes.** `GET /admin/profile`, `/admin/links`, `/admin/projects` and `/admin/projects/{id}` return private content to the owner, and `GET /admin/session` lets an app check its stored token.
- **Image serving auth.** `GET /images/...` checks the bearer header itself instead of using the auth plugin's optional mode, because an invalid token must be treated as an anonymous visitor (the plugin answers 401). The owner can fetch any image, so admin apps can preview staged and private images.
- **Link addresses** accept only `http`, `https`, `mailto` and `tel`, since client apps open them.
- **CORS.** Browser apps call the API from another origin, so the server allows any origin by default (auth is a bearer token, not a cookie) and can be restricted with `PORTFOLIO_CORS_ORIGINS`.
- **Ports.** The server defaults to 8090 because the web dev server uses 8080.
- **One image codec for both admin platforms.** Decoding, cropping, scaling and encoding use Skia directly from common code (`ImageProcessing`), because the desktop and web admin both run on Skia, so no per-platform `expect/actual` was needed. Skia's decoder already applies EXIF orientation, so photos arrive upright.
- **File picking** uses FileKit (`rememberFilePickerLauncher`), which covers desktop and web.
- **Upload queue.** `UploadQueue` holds the gallery as an ordered list of items with their own state. Saving a project leaves the queue untouched so failed uploads can still be retried; only finished images are attached.
- **Session expiry.** A 401 from any admin call logs out. The selected section and any open editor live in view models, so the owner returns to the same unsaved form after logging in again.
- **Admin-to-server contract test.** The server's tests depend on `adminShared` and upload images produced by the admin's real crop and downscale code, so a change to ratios, formats or limits on either side fails a test. This is why the server enables the Compose catalog (for the native Skia runtime in tests only).
- **Versions.** Ktor client and server share one version in `libs.versions.toml`, matching the toolchain's bundled server version.

## Risks / Trade-offs

- [Kotlin Toolchain and wasm library support is young, so a chosen library may not support a target] → Verify each dependency compiles for all its targets in the first group of tasks before building features on it.
- [Image decode and crop on wasm is slower and more constrained than on JVM] → Keep the interface small, downscale early, and test with large photos on both platforms.
- [Large originals still load on mobile for covers and gallery viewing] → Admin-side downscale cap bounds this; extra variants can be added later.
- [Single token table and a single password mean a leaked password gives full control] → Strong password via config, login throttling, token expiry; HTTPS is required in deployment.
- [Server-side `ImageIO` decoding of hostile files] → Check magic bytes and dimensions before decoding, cap file size, reject others.
- [Two wasm web apps built together once failed with a file-exists error while extracting the Skiko runtime into a shared temp directory; a retry passed] → Treat as a toolchain race; rerun the build if it recurs.
- [Daily cleanup could race with an upload that is still being attached] → The 60-minute grace period covers normal attach flows; deletion re-checks references inside the transaction.
- [SQLite writes with concurrent uploads] → Use WAL mode and short transactions.

## Migration Plan

Greenfield: no data to migrate. Existing scaffold modules are renamed or split; the greeting helper and sample UI are deleted. Deployment: run the server with an images directory, SQLite path and password configured through the environment, behind HTTPS. Rollback is redeploying the previous build; the SQLite file and images directory are the only state.

## Open Questions

- Exact numeric limits (max upload bytes, max pixels, token lifetime, ratio tolerance, throttle thresholds) can be set during implementation without changing the specs.
- Which image-loading library to adopt, confirmed in the first dependency-verification task.
