# Design

## Context

- Build system is the Kotlin Toolchain (Amper), not Gradle. `./kotlin package -m server -f executable-jar` makes the server jar; `buildWasmJsAppWasmJsRelease` makes each web app's static directory. There is no native-installer or web-bundle package format, so the Dockerfile calls the build tasks directly.
- Both web apps hardcode `SERVER_URL = "http://localhost:8090"` as a `const`.
- The server reads `PORTFOLIO_PASSWORD` (required), `PORTFOLIO_PORT`, `PORTFOLIO_DB`, `PORTFOLIO_IMAGES`, `PORTFOLIO_CORS_ORIGINS`. Admin routes live under `/admin/*`, public ones under `/api/*` and `/images/*` (`core/ApiPaths`).
- Target host: Raspberry Pi 8 GB (arm64), Portainer, and a Cloudflare tunnel that already runs in its own stack. See proposal.md.

## Goals / Non-Goals

**Goals:**
- One image set that runs unchanged on localhost, LAN and behind the tunnel.
- The public container structurally unable to reach `/admin`.
- Heavy compilation happens once, natively on amd64, never under emulation.

**Non-Goals:**
- TLS inside the stack, authentication in front of the public site, automated backups, a settings screen for the server URL.

## Decisions

**1. Same-origin requests, with a dev fallback.** `main.kt` in each web app computes the server URL at startup: if `window.location` is `localhost:8080`, use `http://localhost:8090`; otherwise use `window.location.origin`. Alternatives: a runtime `config.json` (async startup, CORS to keep in sync with ports) and build-time args (a rebuild per environment, defeating a prebuilt image). The proxy approach also removes CORS from the deployed path entirely.

**2. Two nginx images with different proxy rules.** `client` proxies `/api/` and `/images/` and returns 404 for `/admin`; `admin` proxies `/api/`, `/images/` and `/admin/`. Config is copied into the image, because a Portainer web-editor stack cannot bind-mount repository files. Static files are served with a long cache on hashed assets and `no-cache` on `index.html`. Alternative considered: Caddy for automatic HTTPS, rejected since Cloudflare terminates TLS.

**3. Server unpublished.** Only `client` and `admin` publish ports. The web containers reach the server by its compose service name, so nginx upstream is `server:8090`.

**4. One Dockerfile, build once, assemble per architecture.** A build stage pinned with `--platform=$BUILDPLATFORM` produces the jar and both wasm directories (both are architecture-independent; `sqlite-jdbc` bundles every native, and the Skiko natives are test-only for the server). Final stages `server`, `client`, `admin` are plain `COPY`s onto arm64/amd64 base images, so emulation only touches cheap steps. The server base is a full Temurin JRE (the server uses AWT/ImageIO) run with `-Djava.awt.headless=true`. Alternative: GitHub arm64 runners, rejected since private repos may not get them for free.

**5. Compose written for Portainer.** Image names use `ghcr.io/${GHCR_OWNER}/portfolio-<target>:${IMAGE_TAG:-latest}`. The password uses `${PORTFOLIO_PASSWORD:?...}` so a missing value fails loudly. Server settings fixed inside the compose file: `PORTFOLIO_DB=/data/portfolio.db`, `PORTFOLIO_IMAGES=/data/images`, volume `portfolio-data:/data`. The server gets `JAVA_TOOL_OPTIONS=-Xmx${SERVER_HEAP:-512m}` and a container memory limit, because 40-megapixel uploads decode to a few hundred MB. Ports: `"${ADMIN_BIND:-0.0.0.0}:${ADMIN_PORT:-8081}:80"` and `"${CLIENT_PORT:-3000}:80"`. The tunnel is not part of this stack: it lives in its own stack, so its public hostname targets the client's published port on the host (`http://<host address>:<CLIENT_PORT>`). Alternative considered: a shared external Docker network so the tunnel can use `http://client:80`; not needed, and it would couple the two stacks.

**6. Image workflow.** On tags (and manual dispatch) with Buildx and QEMU, build the three targets for `linux/amd64,linux/arm64` and push to GHCR with the tag and `latest`, using the repository's `GITHUB_TOKEN`. Images start private; making them public later needs no change.

## Risks / Trade-offs

- [Dev fallback is a special case in `main.kt`] → Keep it to one small function with a comment, shared by both apps' pattern.
- [The Amper build in Docker downloads its own JRE and npm packages] → First build is slow and needs network; use BuildKit cache mounts for the Kotlin cache and `~/.npm`.
- [Admin served over plain HTTP on the LAN sends the password unencrypted] → Accepted for a trusted home network; bind to the LAN address and never add it to the tunnel.
- [All admin traffic reaches the server from the proxy's address, so `LoginThrottle` sees one client] → Acceptable: only the owner can reach `/admin/login`, so no extra forwarding-header handling.
- [Pi memory pressure from large image uploads] → Heap cap and container memory limit, both overridable.
- [`/data` volume is the only copy of content] → Document a backup command in the README; automation is out of scope.
- [Compose and Dockerfile cannot be fully verified without Docker on the Pi] → Verify locally with `docker compose` on amd64 and run an arm64 image under emulation once before the first release.

## Migration Plan

1. Push the repository to GitHub (private); add the workflow; tag a release to publish images.
2. In Portainer add a GHCR registry with a `read:packages` token; create the stack from the compose file and set the environment values.
3. In Cloudflare map the tunnel's public hostname to the client's host port; do not map the admin.
4. Rollback: redeploy the stack with the previous `IMAGE_TAG`; the data volume is untouched by image changes.
