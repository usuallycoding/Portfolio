# Proposal

## Why

The server and the two web apps can only be run from source today, and both web apps hardcode `http://localhost:8090` as the server address, so a built copy cannot be pointed anywhere else. The portfolio needs to run on a Raspberry Pi at home: the client public through an existing Cloudflare tunnel, the admin reachable only from the LAN, all managed from Portainer with ports and the owner password set in an environment file.

## What Changes

- The client and admin web apps talk to the server at their own origin instead of a compile-time URL. When served by the dev server (port 8080) they keep using `http://localhost:8090`, so `./kotlin run` still works.
- A multi-target Dockerfile builds the server jar and both wasm bundles once, then assembles three small images: `server`, `client` (nginx) and `admin` (nginx). Nginx configuration is baked into the images.
- The `client` image proxies only the public read API (`/api`, `/images`) to the server and answers `/admin` with 404. The `admin` image proxies the full API.
- A `docker-compose.yml` (pull-only, no bind mounts, so it works pasted into a Portainer stack) runs `server`, `client` and `admin`; the existing Cloudflare tunnel stays in its own stack and points at the client's published port. The server publishes no port and keeps its data in a named volume.
- An `.env.example` documents the settings: owner password, host ports for client and admin, optional LAN bind address for the admin, image tag and JVM memory cap.
- A GitHub Actions workflow builds multi-arch (amd64 and arm64) images and pushes them to GitHub Container Registry, private for now.
- README deployment notes: tunnel hostname mapping (to the client's host port), registry login in Portainer, data backup.

## Capabilities

### New Capabilities
- `container-deployment`: running the server, client web app and admin web app as containers from a compose file configured by environment variables, with the public client unable to reach admin endpoints.

### Modified Capabilities

None. The existing client and admin behavior does not change.

## Impact

- Code: `app/clientWebApp/src/main.kt` and `app/adminWebApp/src/main.kt` (server address resolution).
- New files: `Dockerfile`, nginx configs, `docker-compose.yml`, `.env.example`, `.dockerignore`, `.github/workflows/` image workflow; README additions.
- Systems: GitHub repository and GHCR (the repo has no remote yet), Cloudflare tunnel, Portainer on the Raspberry Pi (arm64, 8 GB).
- Out of scope: native desktop installers, HTTPS inside the stack (Cloudflare terminates it), Cloudflare Access, automatic backups.
