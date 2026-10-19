# Portfolio

A personal portfolio: a Ktor server, admin apps for the owner to edit content, and read-only client apps for visitors. Built with Kotlin Multiplatform and the [Kotlin Toolchain](https://kotlin-toolchain.org/dev/).

## Modules

| Module | Purpose | Targets |
|---|---|---|
| [`core`](./core) | DTOs, API paths and the `PortfolioApi` client shared by everything | jvm, android, ios, wasmJs |
| [`server`](./server) | Ktor server: public API, admin API, images, SQLite | jvm |
| [`app/sharedUi`](./app/sharedUi) | Theme and Compose pieces shared by admin and client | jvm, android, ios, wasmJs |
| [`app/clientShared`](./app/clientShared) | Client screens (read-only) | android, ios, wasmJs |
| [`app/adminShared`](./app/adminShared) | Admin screens (editing) | jvm, wasmJs |
| [`app/clientAndroidApp`](./app/clientAndroidApp), [`app/clientIosApp`](./app/clientIosApp), [`app/clientWebApp`](./app/clientWebApp) | Client shells | |
| [`app/adminDesktopApp`](./app/adminDesktopApp), [`app/adminWebApp`](./app/adminWebApp) | Admin shells | |

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts are self-bootstrapping wrappers for the Kotlin Toolchain. Build everything with `./kotlin build`, and run tests with `./kotlin test`.

## Running

- Server: `PORTFOLIO_PASSWORD=<password> ./kotlin run -m server`
- Client apps: `./kotlin run -m clientAndroidApp`, `./kotlin run -m clientIosApp`, `./kotlin run -m clientWebApp`
- Admin apps: `./kotlin run -m adminDesktopApp` (set `PORTFOLIO_SERVER_URL` to use a server other than `http://localhost:8090`), `./kotlin run -m adminWebApp`

The web dev server uses port 8080, so run only one of the two web apps at a time.

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
