# Portfolio

A personal portfolio: a Ktor server, admin apps for the owner to edit content, and read-only client apps for visitors. Built with Kotlin Multiplatform and the [Kotlin Toolchain](https://kotlin-toolchain.org/dev/).

## Modules

| Module | Purpose | Targets |
|---|---|---|
| [`core`](./core) | DTOs and API paths: the contract shared by the server and the apps | jvm, android, ios, wasmJs |
| [`app/sharedData`](./app/sharedData) | `PortfolioApi`, the HTTP client and its per-platform engines | jvm, android, ios, wasmJs |
| [`server`](./server) | Ktor server: public API, admin API, images, SQLite | jvm |
| [`app/sharedUi`](./app/sharedUi) | Theme and Compose pieces shared by admin and client | jvm, android, ios, wasmJs |
| [`app/clientShared`](./app/clientShared) | Client screens (read-only) | android, ios, wasmJs |
| [`app/adminShared`](./app/adminShared) | Admin screens (editing) | jvm, wasmJs |
| [`app/clientAndroidApp`](./app/clientAndroidApp), [`app/clientIosApp`](./app/clientIosApp), [`app/clientWebApp`](./app/clientWebApp) | Client shells | |
| [`app/adminDesktopApp`](./app/adminDesktopApp), [`app/adminWebApp`](./app/adminWebApp) | Admin shells | |

## Architecture

`clientShared` and `adminShared` are organized in layers, as packages:

```
presentation  screens and view models; depends only on domain
domain        repository interfaces (the DTOs from `core` double as the domain models)
data          repository implementations over `PortfolioApi` (`sharedData`)
di            Koin modules
```

Dependencies point inward, and nothing in `presentation` or `domain` imports Ktor or `PortfolioApi`. Dependency injection is [Koin](https://insert-koin.io/). `ClientApp` and `AdminApp` are the composition roots: the shells pass in what differs per device (server URL, theme store, platform kind) and the root creates an isolated Koin for that composition and closes it when it leaves. Do not use Koin's compose `KoinApplication` here: it shares one global instance across compositions. Tests swap definitions through the `overrides` argument of `AdminApp` or by building a Koin from `clientModules(...)` / `adminModules(...)`. Keep module definitions in functions, not top-level values, because a Koin `Module` owns its singleton instances.

### Server

`server` follows the same layers, as packages under `server/src`:

```
api       Ktor routes, plugins, error mapping; depends on services only
service   validation and business rules (profile, links, projects, images, auth, cleanup)
domain    repository interfaces, ports (image files, image processing, password check), Transactor, domain errors
data      SQLite repositories, file storage, AWT image processing, PBKDF2; the only place that sees java.sql
di        Koin modules
config    ServerConfig
```

Services run each unit of work through `Transactor.transaction { ... }`, so several repositories change together or not at all. Failures are `DomainError`s with no HTTP in them; `api/Errors.kt` maps them to status codes. `serverApplication` is the composition root: it installs Koin with the `KoinIsolated` plugin (the plain `Koin` plugin starts one global instance, so a second server in the same process would reuse the first one's database), and the isolated instance is closed, with the database, when the application stops. Tests start a server with `withServer { ... }`, fetch what they need with `koin.get<...>()`, and can swap any definition with the `overrides` argument. As in the apps, build Koin modules in functions, not top-level values.

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts are self-bootstrapping wrappers for the Kotlin Toolchain. Build everything with `./kotlin build`, and run tests with `./kotlin test`.

## Running

- Server: `PORTFOLIO_PASSWORD=<password> ./kotlin run -m server`
- Client apps: `./kotlin run -m clientAndroidApp`, `./kotlin run -m clientIosApp`, `./kotlin run -m clientWebApp`
- Admin apps: `./kotlin run -m adminDesktopApp` (set `PORTFOLIO_SERVER_URL` to use a server other than `http://localhost:8090`), `./kotlin run -m adminWebApp`

The web dev server uses port 8080, so run only one of the two web apps at a time. It expects the server on `localhost:8090`; served from any other address the web apps use their own address as the server's.

## Deployment

The server and both web apps run as containers from `docker-compose.yml`. The web apps call the server at their own address, so the same images work on localhost, on the local network and behind a tunnel.

```
internet -> Cloudflare tunnel (separate stack) -> client (nginx) -> server   public site: /api and /images only, /admin is 404
local network -> admin (nginx) -> server                                    owner site: the full API
```

The server publishes no port. Its database and uploaded images live in the `portfolio-data` volume.

**Images.** Tag a release (`git tag v1.0.0 && git push --tags`) and the *Images* workflow publishes `ghcr.io/<owner>/portfolio-server`, `-client` and `-admin` for amd64 and arm64 (including the Raspberry Pi), as the tag and as `latest`. You can also run the workflow by hand.

**Settings.** Set the variables from [`.env.example`](./.env.example) in a `.env` file next to the compose file, or in the Portainer stack's environment variables:

| Variable | Meaning |
|---|---|
| `PORTFOLIO_PASSWORD` | Owner's admin password (required) |
| `GHCR_OWNER` | GitHub account that owns the images, lower case (required) |
| `CLIENT_PORT`, `ADMIN_PORT` | Host ports of the public and the owner site (3000 and 8081) |
| `ADMIN_BIND` | Publish the admin only on this host address, e.g. the Pi's LAN address |
| `IMAGE_TAG` | `latest`, or a release tag to pin a version |
| `SERVER_HEAP`, `SERVER_MEMORY` | JVM heap and container memory limit (512m and 768m) |

**Portainer.**
1. Registries -> Add registry -> GitHub (or custom `ghcr.io`) with a token that has `read:packages`. Not needed once the images are public.
2. Stacks -> Add stack, paste `docker-compose.yml`, and fill in the environment variables.
3. The tunnel runs in its own stack. In Cloudflare, point the public hostname at the client's published port, `http://<host address>:<CLIENT_PORT>` (the Pi's LAN address; `localhost` would mean the tunnel's own container). Do not add the admin to the tunnel; open it from the local network at `http://<host>:<ADMIN_PORT>`.

**Local run.** `docker compose up` with a `.env` (set `PORTFOLIO_PASSWORD`, `GHCR_OWNER` and an `IMAGE_TAG` you have built). To build the images yourself: `docker build --target server -t ghcr.io/<owner>/portfolio-server:dev .`, and the same for `client` and `admin`.

**Updating.** Change `IMAGE_TAG` (or pull `latest`) and redeploy the stack; the data volume is untouched. Roll back by setting the previous tag.

**Backup.** `docker run --rm -v <stack>_portfolio-data:/data -v "$PWD":/backup alpine tar czf /backup/portfolio-data.tgz -C /data .` (the volume name is the stack name followed by `_portfolio-data`; `docker volume ls` shows it).

## App behavior

- **Theme.** Every app follows the device's light or dark setting. A "Theme" menu (top of the client profile, and in the admin navigation rail) lets you choose System, Light or Dark; the choice is remembered on that device.
- **Social links** are shown as rows with a large icon, the name and the address; tapping a row opens it.
- **Project gallery.** A project's images can be viewed as a grid or, with the Grid / Reader switch, as one continuous vertical strip for reading comics. Every project opens as the grid.
- **Pull to refresh.** On Android and iOS, pulling down on the profile or on a project reloads it. The web client does not offer this.
- **Layout.** The profile is centered: the avatar sits on the cover's bottom edge, with the name, a "Socials" title and the "Projects" title centered (the description is a centered block with left-aligned text). On screens at least 840 dp wide, the description, social links, projects and (on a project page) the date, description and image grid share a centered column half the screen wide (never under 560 dp), and the social links wrap in a centered row; on smaller screens everything is full width and each link has its own row. The cover and the reader's images always span the full width. Project cards are all the same size, with the summary cut after three lines.
- **Long text and long lists.** The profile description and a project's summary show at most 10 rows (ending with an ellipsis when more is hidden). Tapping or clicking either one opens a sheet with the full text, a copy button and a close button, whether or not anything is hidden. Social links fold behind a small expand arrow after 5 rows and project cards after 2 rows (with a "show less" arrow once expanded); the profile remembers what you expanded while you visit a project and come back.
- **Image viewer.** Opening an image from the grid shows it full screen. Swipe, or press the left and right arrow keys, to move between images (the keys stop at the first and last image), and use the close icon at the top right to leave.
- **Reading.** On Android and iOS, scrolling down a project hides the status bar and the top bar so the page is full screen, and scrolling up brings them back.
- **Web URLs.** The web client uses hash URLs: `#/` is the profile and `#/projects/<id>` is a project. Browser back and forward, reload and shared links all work on any static host with no server configuration. The project page has no Back button on the web; use the browser's.

## Server configuration

The server is configured with environment variables.

| Variable | Required | Default | Meaning |
|---|---|---|---|
| `PORTFOLIO_PASSWORD` | yes | | The owner's admin password. The server refuses to start without it. |
| `PORTFOLIO_DB` | no | `data/portfolio.db` | Path of the SQLite database file. |
| `PORTFOLIO_IMAGES` | no | `data/images` | Directory where uploaded images are stored. |
| `PORTFOLIO_PORT` | no | `8090` | Port to listen on. |
| `PORTFOLIO_CORS_ORIGINS` | no | any origin | Comma-separated browser origins allowed to call the API, e.g. `https://me.example,http://localhost:8080`. The web apps need their own origin listed when you restrict this. |

Run the server behind HTTPS in any real deployment: the admin password and bearer token are sent with every login and admin request.

State lives in two places only, the SQLite file and the images directory. Back up both together.

### API overview

- Public, no token: `GET /api/profile`, `/api/links`, `/api/projects`, `/api/projects/{id}`, and `GET /images/{fileName}` (add `?size=thumb` for the small variant).
- Admin, `Authorization: Bearer <token>`: everything under `/admin/*`. Get a token with `POST /admin/login` and body `{"password": "..."}`.
