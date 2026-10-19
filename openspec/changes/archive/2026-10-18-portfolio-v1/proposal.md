# Proposal

## Why

The repo is a fresh Kotlin Multiplatform scaffold (one shared Compose UI, four app shells, a Ktor server with a single hello route). There is no portfolio functionality yet. The owner needs a way to publish and maintain a personal portfolio (profile, social links, projects with images) from admin apps, and have visitors browse it from read-only client apps on Android, iOS and the web.

## What Changes

- Add a Ktor server with a SQLite database, a public read-only API for clients and an authenticated admin API for the single owner.
- Add single-owner password login; admin routes require a bearer token.
- Add a profile (name, description, 1:1 profile picture, 3:1 cover image) that is always public and shows placeholders until filled in.
- Add ordered social links (name, address, isPublic, 1:1 icon) with manual ordering.
- Add projects (title, creation date, isPublic, summary, 16:9 thumbnail, images) with manual project ordering and manual image ordering.
- Add image storage: files on disk under unique server-generated names, served by the server, with a server-generated ~400px thumbnail variant (`?size=thumb`) that keeps the original's format. Uploads are one file per request and staged until attached.
- Add a daily cleanup job that deletes orphan images (row, original and thumbnail) uploaded more than 60 minutes ago.
- Add admin apps (desktop and web) for editing everything above, with in-app crop (zoom and positioning) to the required ratio, downscale before upload, and multi-file upload with per-file progress and manual reordering.
- Add client apps (Android, iOS, web) that view the profile, social links, public projects and each project's images.
- Restructure modules: separate admin and client apps (two web apps), and shared models and API client in `core`.

## Capabilities

### New Capabilities
- `owner-auth`: single-owner password login and bearer-token protection of admin routes.
- `profile`: the public profile (name, description, profile picture, cover image) with placeholders.
- `social-links`: ordered social links with visibility and icon.
- `projects`: projects with visibility, manual ordering, thumbnail and ordered images.
- `image-storage`: image upload, validation, unique naming, thumbnail variants, serving and orphan cleanup.
- `admin-app`: desktop and web editing apps, including cropping and multi-upload.
- `client-app`: read-only Android, iOS and web apps for browsing the portfolio.

### Modified Capabilities

## Impact

- `server/`: new routes, SQLite persistence, image handling, scheduled job, auth. New dependencies for SQLite, JSON, multipart, auth and a scheduler.
- `core/`: shared DTOs and an API client usable from jvm, android, ios and wasmJs.
- `app/`: `shared` UI split into client and admin UI; `webApp` split into client and admin web apps; the Android and iOS shells become client-only; the desktop shell becomes admin-only.
- `project.yaml` and `module.yaml` files: new modules and dependencies.
- Disk: an images directory and a SQLite file on the server host.
