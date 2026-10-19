# Tasks

## 1. Theme foundation (sharedUi)

- [x] 1.1 Add the `multiplatform-settings` no-arg dependency to `sharedUi` and verify `./kotlin build` compiles it for jvm, android, iOS and wasmJs; if a target fails, replace it with small hand-written storage for that target and note it in design.md
- [x] 1.2 Add `ThemePreference`, light and dark color schemes, `PortfolioTheme(preference, content)` and a `ThemeStore` whose reads and writes never throw (a failure means System); verify sharedUi unit tests cover Light, Dark and System with a faked system setting, a stored value round trip, and a corrupt or unreadable stored value falling back to System
- [x] 1.3 Add `ThemeController` (current preference as Compose state, saved on change) and a shared `ThemePicker` menu offering System, Light and Dark and marking the current one; verify a Compose UI test selects each option and the controller and the store both update

## 2. Apply themes in the apps

- [x] 2.1 Use the controller in `ClientApp` and place the picker at the top end of the profile screen; verify rendered screenshots of the profile and project screens in light and dark show no leftover white surfaces or unreadable text
- [x] 2.2 Use the controller in `AdminApp` and put the picker in the navigation rail above "Log out"; verify rendered screenshots of login, profile, links, projects and the crop dialog in both themes, and the existing admin tests still pass
- [x] 2.3 Report the resolved dark or light value from `ClientApp` and re-apply `enableEdgeToEdge` with a matching style in `MainActivity`; add a `prefers-color-scheme: dark` page background to both web `styles.css` files; verify the Android build and that a web page loads without a white flash in a dark browser
- [x] 2.4 Check on devices and the web: dark device shows dark on first launch, switching the device setting while the app is open switches the app, choosing Light on a dark device sticks after restart, and the iOS status bar text stays readable; record any iOS limitation in design.md

## 3. Client social links

- [x] 3.1 Replace the link chips with full-width clickable rows: 48 dp icon (same-size neutral placeholder when missing), name, and address beneath with single-line ellipsis; verify UI tests for name and address both visible, long address ellipsized, row tap opens that link's address, no links shows no section, and icon bounds are at least 40 dp
- [x] 3.2 Verify a rendered screenshot of the profile with several links (with and without icons, one very long address) in light and dark looks right, and the existing client tests still pass

## 4. Gallery views and reader

- [x] 4.1 Add `GalleryMode` (Grid default, kept across refresh and rotation) and a segmented Grid/Reader control on the project screen; verify a UI test switches to the reader and back and the grid is shown first
- [x] 4.2 Build the reader: full-width images from the originals in the owner's order, no gaps, no cropping, each sized from its `width`/`height` before loading, header scrolling with the strip, content capped at 800 dp and centered, and the "No images yet." message when empty; verify UI tests for order, zero spacing between items, item heights matching proportions at a given width, wide window cap, and the empty message
- [x] 4.3 Verify the reader against a seeded server: a project with many tall and wide images scrolls continuously on desktop-sized and phone-sized windows, and only nearby images are requested (check the server access pattern or an image-request count in a test)

## 5. Pull to refresh

- [x] 5.1 Add `refresh()`, `isRefreshing` and a one-shot refresh error to `HomeViewModel` and `ProjectViewModel` that keep the current content while loading; verify view-model tests: content stays during a refresh, new data replaces it on success, failure keeps the old content and reports an error, and `load()` still shows the full-screen loading and retry states
- [x] 5.2 Add `pullToRefresh: Boolean` to `ClientApp`, wrap the loaded profile and project content in `PullToRefreshBox` when enabled, show refresh failures in a Snackbar with Retry, and pass `true` from the Android and iOS shells and not from the web shell; verify a UI test that content stays visible and the gallery mode is unchanged after a refresh, and a test that nothing refreshes when the flag is off
- [x] 5.3 Check on an Android device and an iOS device: pulling down on the profile and on a project shows the indicator and updates content changed on the server, a refresh with the server stopped keeps the screen and shows the retry message, the reader and grid scroll normally without accidental refreshes, and the web client does nothing on pull

## 6. Integration

- [x] 6.1 Run `./kotlin build` for all targets and `./kotlin test`, update the README with the theme and reader behavior, and verify everything passes
- [x] 6.2 Walk through the whole flow on a device against a seeded server in both themes: profile with link rows, a project in grid and reader, pull to refresh on both screens, and the admin app in both themes; verify nothing regresses
