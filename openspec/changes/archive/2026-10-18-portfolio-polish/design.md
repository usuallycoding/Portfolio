# Design

## Context

See proposal.md for motivation. Current state of the code this change touches:

- `app/sharedUi` has `PortfolioTheme`, which wraps `MaterialTheme` with the default light colors only. Both client and admin apps use it, and the Android, iOS, desktop and web shells call the shared apps directly.
- `app/clientShared` shows links as `AssistChip`s with a 24 dp icon and only the name (`HomeScreen.kt`). The project page (`ProjectScreen.kt`) is a `LazyVerticalGrid` of square thumbnails that opens a full-screen pager. `HomeViewModel` and `ProjectViewModel` load once, and `load()` resets the state to `Loading`, which replaces the whole screen.
- Image DTOs already carry `width` and `height`, so the client knows each image's proportions before it loads.
- Material3 1.9.0 is already a dependency, which includes `PullToRefreshBox`.
- Nothing is stored on the device today.

## Goals / Non-Goals

**Goals:**
- One theme implementation shared by all five app shells, following the device by default with a remembered override.
- A refresh that keeps what is on screen (content, scroll position, gallery view) instead of replacing it with a spinner.
- A reader whose layout never jumps while images load.

**Non-Goals:**
- Custom brand colors or per-app palettes beyond a coherent light and dark Material color scheme.
- Dynamic (wallpaper) colors on Android.
- Pinch-zoom, tap zones, chapter navigation or remembered position in the reader.
- Syncing the theme choice across devices.

## Decisions

### Theme model
`ThemePreference { System, Light, Dark }` and `PortfolioTheme(preference, content)` in `sharedUi`. Dark is resolved as `Dark`, or `System` and `isSystemInDarkTheme()`. `isSystemInDarkTheme()` is a common Compose function that recomposes when the device setting changes, which covers the "switch while open" scenario on Android, iOS, desktop and web. The schemes are Material `lightColorScheme()` and `darkColorScheme()` with the existing seed colors, so no screen needs per-color changes; the few hard-coded colors (the black image viewer) are intentionally theme-independent.

Alternative considered: a two-state toggle (light/dark) without System. Rejected because the request is to follow the device by default.

### Remembering the choice
A small `ThemeStore` interface (`load(): ThemePreference`, `save(ThemePreference)`) backed by the `multiplatform-settings` library with its no-argument artifact, which stores in `SharedPreferences` on Android, `NSUserDefaults` on iOS, `java.util.prefs` on desktop and `localStorage` in the browser. Every read and write is wrapped so a failure (private browsing, blocked storage) falls back to System and the app keeps working. A `ThemeController` holds the current preference as Compose state and saves on change.

Alternatives considered: hand-written `expect/actual` per platform (more code and an Android `Context` plumbing problem for no benefit), and DataStore (not available on web).

### Where the picker lives
A shared `ThemePicker` composable (a button opening a menu with System, Light and Dark, marking the current one) in `sharedUi`. The client places it at the top end of the profile screen; the admin app places it in the navigation rail above "Log out".

### Platform details
- Android: `enableEdgeToEdge` chooses system bar icon colors from the system theme, which is wrong when the user overrides it. `ClientApp` reports the resolved dark/light value through a callback, and `MainActivity` re-applies `enableEdgeToEdge` with a matching `SystemBarStyle`.
- iOS: the status bar style follows the system; when the user overrides the theme the status bar text may not match. Mitigation: the top inset area is painted with the theme's surface color so content stays readable; verified on a device in the tasks.
- Web: both web pages start with a white background before Compose paints, which flashes in dark mode. `styles.css` gets a `prefers-color-scheme: dark` rule for the page background.

### Link rows
Replace the chip flow with a column of full-width, clickable rows: a 48 dp rounded icon (a neutral square of the same size when absent), the name (`titleMedium`) and the address below it (`bodySmall`, single line with ellipsis). The icon uses the thumbnail variant, which is 400 px and sharp at this size. Whole row is the tap target.

### Gallery views and the reader
`GalleryMode { Grid, Reader }` is held by the client navigation state (`rememberSaveable`) so it survives refresh and rotation, and defaults to Grid. A segmented control under the project header switches modes. The reader is a `LazyColumn` in the same scroll as the header: each item is `PortfolioImage` at full width with `Modifier.aspectRatio(width/height)` and `ContentScale.FillWidth`, no spacing and no cropping, loading the original image. Because the aspect ratio comes from the DTO, each item has its final height before its bitmap arrives, so the strip does not jump. The content column is capped at 800 dp and centered on wide screens. Lazy composition means only visible images (plus a small buffer) are fetched, so a long chapter does not download everything at once.

Alternative considered: reusing the grid's `LazyVerticalGrid` with one column. Rejected because the grid's header and spacing model fights a seamless strip.

### Refresh without losing the screen
View models get `refresh()` next to `load()`. `load()` keeps its current meaning (initial load and the full-screen Retry). `refresh()` leaves the existing `Loaded` value in place, exposes `isRefreshing`, replaces the value on success, and on failure emits a one-shot error message. The screen shows that message in a `Snackbar` with a Retry action. Because the screen is never swapped for a loading view, scroll position and the gallery mode are kept.

### Pull to refresh gating
`ClientApp` takes `pullToRefresh: Boolean = false`; the Android and iOS shells pass `true` and the web shell does not. When enabled, the loaded profile and project content is wrapped in `PullToRefreshBox`. Passing the flag from the shells states the Android/iOS-only rule in one visible place and avoids `expect/actual` for a boolean.

### Adjustments made during implementation
- **Image loader through the composition.** `ConfigureImageLoading` (Coil's global singleton) was replaced by `ProvideImageLoading`, which provides the loader through a composition local. The singleton can be configured only once per process and throws on the next composition, which would crash Android when the activity is recreated (for example on rotation). A regression test composes the app repeatedly in one process.
- **Image placeholders keep their content description** while loading, not only on error, so screen readers and tests can identify images that are still loading.
- **Injectable dependencies for tests.** `ClientApp` and `AdminApp` accept a `ThemeStore` (and `ClientApp` an `HttpClient`), so tests never touch real preferences or the network.
- **Reader mode resets per project.** The mode is held at navigation level so a refresh keeps it, and is set back to Grid whenever a project is opened, as the spec requires.

## Risks / Trade-offs

- [`multiplatform-settings` may not support every target combination in this toolchain] → First task adds the dependency and compiles all five targets before any feature depends on it; fall back to hand-written actuals if it does not.
- [iOS status bar text can mismatch when the theme is overridden] → Themed top inset background; checked on device; accepted if only the status bar text is affected.
- [Original images in the reader are large on mobile data] → Lazy loading, and the admin already caps uploads at 2000 px; no extra variant is added.
- [`isSystemInDarkTheme()` on the wasm target depends on browser support for `prefers-color-scheme`] → Verified in a browser in the tasks; the picker still lets users force a theme.
- [Pull-to-refresh gesture conflicts with the image grid or reader scrolling] → The container only triggers when the list is at the top, which is how `PullToRefreshBox` behaves; checked on devices.
- [Compose UI tests cannot reproduce a real touch pull on a device] → Refresh logic is covered by view-model tests, the gesture by a UI test where the desktop test harness allows it, and by the on-device checks.

## Migration Plan

No data or API migration. Existing installs start on System theme because nothing is stored yet. Rollback is reverting the app builds.

## Open Questions

- Exact color seed for the light and dark schemes: start from the Material defaults already in use; change later without affecting the specs.
- Whether the reader's maximum width of 800 dp suits the owner's comics; adjustable without changing the specs.
