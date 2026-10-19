# Design

## Context

See proposal.md for motivation. Current client code this change touches:

- `HomeContent` (`HomeScreen.kt`) is a `LazyColumn` capped at 720 dp holding the header, link rows, a "Projects" title and one full-width `ProjectCard` per project. `ProjectContent` (`ProjectScreen.kt`) uses a `Scaffold` with a `TopAppBar` (a text "Back" button), a grid capped at 720 dp and a reader capped at 800 dp.
- `ClientNavigation` (`ClientApp.kt`) keeps `projectId`, `imageIndex` and `galleryMode` in `rememberSaveable` and uses a `BackHandler`. `ClientApp` already takes a `pullToRefresh` flag that the shells set (Android and iOS true, web false).
- `ThemePicker` (`sharedUi`) is a `TextButton` ("Theme: System") with a dropdown, shared with the admin app.
- The Android shell already calls `enableEdgeToEdge` and reports the theme through `onDarkThemeChanged`. The iOS shell is a SwiftUI host (`iosApp.swift`) around `ViewControllerKt.ViewController()`.
- The web dev server answers unknown paths such as `/projects/7` with 404 (verified), which is why hash URLs were chosen.

## Goals / Non-Goals

**Goals:**
- One place that states which behavior belongs to which platform.
- Bars that hide and show without moving the content under the visitor's finger.
- Navigation state that can be driven by the browser without the common code knowing about browsers.

**Non-Goals:**
- Path-style URLs, or URLs for the image viewer or the grid/reader choice.
- Hiding bars on the profile page, or on web.
- A router library; the app has two pages.

## Decisions

### Platform behavior in one place
Replace the `pullToRefresh: Boolean` parameter with `platform: ClientPlatform { Mobile, Web }`. Each value answers the platform questions in one place: pull to refresh (Mobile), auto-hiding bars (Mobile), a visible back control (Mobile) and BackHandler use (Mobile). The Android and iOS shells pass `Mobile`, the web shell passes `Web`. This keeps the Android/iOS-only rules visible and testable instead of scattered booleans.

### Full width
Delete the `widthIn(max = …)` caps. Content keeps its 16 dp side padding. On very wide screens the reader's originals are scaled up to the window width; the admin already caps uploads at 2000 px, so on screens wider than that images are upscaled slightly. This is accepted because full width was requested.

### Profile project grid
`HomeContent` stays one `LazyColumn`. The cover and avatar header, the link rows and the "Projects" title are ordinary items, and the projects are laid out as lazy *rows* of equal-width cards: the column count is computed from the available width (`floor((width - 2*16 + 12) / (160 + 12))`, at least 1), so a typical phone gets two columns and wide screens get many. A short last row keeps the same card width. Keys are type-prefixed (`link-…`, `project-row-…`) because items of different kinds share one lazy container and their ids overlap. A `LazyVerticalGrid` was tried first in the design but rows in a column give exact 16 dp gutters and a full-bleed cover without fighting grid content padding.

Equal-size cards: every cell in an adaptive grid has the same width; the height is made constant by a fixed structure, a 16:9 thumbnail (a placeholder when there is none), a title limited to one line, the date, and a summary set to `minLines = 3, maxLines = 3` so a short or empty summary still reserves three lines. The card height therefore depends only on the cell width and the font, not on the content.

Alternative considered: a fixed card height in dp. Rejected because it would break with larger system font sizes.

### Standard icons
Add `org.jetbrains.compose.material:material-icons-extended` and use `Icons.AutoMirrored.Filled.ArrowBack` for back and `BrightnessAuto`, `LightMode` and `DarkMode` for the theme control, each as an `IconButton` with a content description ("Back", "Theme: System"…). The dropdown keeps text labels so the choice is explicit. Because `ThemePicker` is shared, the admin app's picker becomes an icon button too, which keeps both apps consistent.

Measured: with four icons used, the debug web bundle grew from 27.1 MB to 29.8 MB (about 10%); the toolchain only builds debug wasm today, and release builds remove unused code, so the official library was kept. If a release build shows a problem, the fallback below applies.

Risk: the extended icon set is large. Unused icons are removed by the Android shrinker and the wasm optimizer; the first task measures the web bundle and, if the growth is significant, replaces the dependency with the few needed `ImageVector`s copied from the Material icon paths.

### Auto-hiding bars
A small `BarsVisibility` state holder is fed by a `NestedScrollConnection` on the project content: scrolling down by more than a threshold sets hidden, any upward scroll sets visible, and being at the top forces visible. It is only attached when `platform == Mobile`.
- The top bar is drawn as an overlay animated in and out, not in a `Scaffold` top-bar slot, and the content has a constant top padding equal to the bar plus status bar height. Hiding the bar therefore does not move the content.
- `ClientApp` reports the state through a new callback `onSystemBarsVisibleChanged(visible: Boolean)`, called with `false` when hidden and `true` when shown, when leaving the project page and when the viewer or the page is disposed, so bars are never left hidden.
- Android: `MainActivity` uses `WindowInsetsControllerCompat` to hide and show the status bars with transient-by-swipe behavior, and sets the display cutout mode to short edges so notch devices do not letterbox.
- iOS: `ViewController` accepts the callback. `iosApp.swift` holds a `@State` flag set from that callback and applies `.statusBarHidden(…)` to the Compose host.
- The project page's top padding uses the status bar inset *ignoring visibility* where the platform supports it, so hiding the status bar does not shift content.

Alternative considered: Material's built-in `TopAppBarScrollBehavior`. Rejected because it only handles the top bar, and the status bar and the top bar must move together.

### Route model and web navigation
Common code gets `Route { Home, Project(id) }` with pure `parse(hash: String): Route` and `format(route): String` (`#/` and `#/projects/<id>`; anything unrecognised parses to Home). A small `RouteHost` interface (`current(): Route`, `push(route)`, `replace(route)` and a listener for external changes) is passed into `ClientApp` as an optional parameter. `ClientNavigation` holds the current route as state; when a host is present it initializes from `host.current()`, calls `host.push` when the visitor opens a project, and updates its state when the host reports a change. With no host (Android, iOS) it keeps the current in-memory behavior.

The web shell implements `RouteHost` with `window.location.hash` and a `hashchange` listener: pushing sets the hash (which creates a history entry), the listener turns browser back, forward, manual edits and loaded links into route changes, and an unrecognised hash is replaced with `#/`. Echoes of the app's own pushes are ignored because the route is unchanged.

The browser back button is the only back control on web: the back button is not drawn, and `BackHandler` is only enabled on Mobile.

Alternative considered: `navigation-compose` with its browser binding. Rejected because it adds a dependency and a larger refactor for two pages, and its URL format would not be the stable hash format specified.

### Profile proportions on wide screens
Full width suits the cover's edges and the project cards, but a 3:1 cover at 1600 px is over 530 px tall and lines of text 1600 px wide are hard to read. The cover keeps its full width but its height is `min(width / 3, 240 dp)`, cropped around the centre (so phones, where it is under 240 dp tall, are unchanged). The description and each link row are limited to 720 dp, left-aligned to the page gutter so they line up with the project cards below.

### Adjustments made during implementation
- **Card width.** The minimum card width is 160 dp, so a typical phone gets two columns (220 dp would have kept phones at one per row, which contradicts "not only one per row").
- **Route parsing** accepts ASCII digits only, because Kotlin parses other Unicode digits differently on different platforms.
- **Wasm bundle.** The platform builds debug wasm only, so the icon cost was measured there (+2.7 MB of 27 MB).
- **Status bar inset.** The page keeps the largest status bar inset it has seen as its top padding, which avoids shifting content when the system hides the status bar without relying on a platform-specific "ignoring visibility" API.

## Risks / Trade-offs

- [Hiding the status bar changes the safe-area insets, which can shift content or letterbox a notch] → Overlay top bar with constant padding, insets ignoring visibility, short-edge cutout mode on Android, checked on devices.
- [iOS status bar control from SwiftUI may behave differently on notch and non-notch devices] → Verified on a device; if it misbehaves, only the status bar part falls back to staying visible.
- [Hash `RouteHost` is browser-only code that cannot run in JVM tests] → The route parser and navigation logic are tested in JVM tests with a fake `RouteHost`; the browser adapter is small and verified manually in a browser.
- [Full width makes the reader's images blurry on very wide screens] → Accepted by request; originals are capped at 2000 px.
- [Material icons bundle size on web] → Measured in the first icon task, with a fallback to copied vectors.
- [A project URL for a deleted or private project] → Shows the normal recoverable error with Retry; the visitor goes back with the browser or edits the URL.

## Migration Plan

No data or API change. Existing installs keep working; the web app gains URLs on the next deploy. Hash URLs need no host configuration. Rollback is reverting the app builds.

## Open Questions

- Minimum card width (160 dp) and card spacing can be tuned without changing the specs.
- Whether to set the web page title per route (a nice-to-have, not required by the specs).
