# Tasks

## 1. Runtime server address in the web apps

- [x] 1.1 Replace the `SERVER_URL` const in `app/clientWebApp/src/main.kt` and `app/adminWebApp/src/main.kt` with a function that returns `http://localhost:8090` on `localhost:8080` and `window.location.origin` otherwise. Verify: `./kotlin build` succeeds and `./kotlin run -m clientWebApp` with the server running still loads the profile.

## 2. Images

- [x] 2.1 Add `.dockerignore` (build output, `data/`, `.git`, IDE files). Verify: `docker build` context stays small.
- [x] 2.2 Write the Dockerfile build stage (`--platform=$BUILDPLATFORM`) producing the server jar and both release wasm directories, with cache mounts. Verify: `docker build --target build .` completes and the artifacts exist in the stage.
- [x] 2.3 Add the `server` target (full Temurin JRE, headless, non-root user, `/data` volume, exposes 8090). Verify: the container starts with `PORTFOLIO_PASSWORD` set and refuses to start without it.
- [x] 2.4 Add nginx configs and the `client` and `admin` targets: `client` proxies `/api/` and `/images/` and returns 404 for `/admin`; `admin` also proxies `/admin/`; SPA caching headers. Verify: with a running server, `curl` on the client container returns 200 for `/api/profile` and 404 for `/admin/login`, while the admin container's `/admin/login` reaches the server.

## 3. Compose and configuration

- [x] 3.1 Write `docker-compose.yml` with `server` (no published port, named volume, memory cap), `client` and `admin`, using only `${VAR}` substitutions and no bind mounts. Verify: `docker compose config` renders, and fails naming `PORTFOLIO_PASSWORD` when it is unset.
- [x] 3.2 Write `.env.example` documenting every variable and add `.env` to `.gitignore`. Verify: copying it to `.env` with a password set lets `docker compose up` start the stack locally.
- [x] 3.3 Run the stack locally on custom `CLIENT_PORT` and `ADMIN_PORT`. Verify: login through the admin, create content with an image, see it on the client, restart with `down` and `up`, and confirm the content persists and the server port is not reachable from the host.

## 4. Registry and CI

- [ ] 4.1 Add `.github/workflows` image workflow (Buildx, QEMU, GHCR login with `GITHUB_TOKEN`) for linux/amd64 and linux/arm64, triggered by version tags and manual dispatch. Verify: `actionlint` or a workflow dry run is clean, and a manual run publishes all three images.
- [x] 4.2 Verify the arm64 images: pull the arm64 server image under emulation (or on the Pi) and confirm it starts and serves `/api/profile`.

## 5. Documentation

- [x] 5.1 Add a README "Deployment" section: image names, the environment variables, GHCR registry setup in Portainer, mapping the tunnel hostname to the client's host port, keeping the admin out of the tunnel, and a `/data` volume backup command. Verify: following it on a clean Portainer stack reaches a working site.
