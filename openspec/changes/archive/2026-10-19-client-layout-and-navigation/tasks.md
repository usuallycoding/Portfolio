# Tasks

## 1. Platform behavior and full width

- [x] 1.1 Add `ClientPlatform { Mobile, Web }` to replace `pullToRefresh: Boolean` in `ClientApp`, and pass `Mobile` from the Android and iOS shells and `Web` from the web shell; verify the existing pull-to-refresh tests pass using the new parameter and that nothing refreshes on `Web`
- [x] 1.2 Remove the 720 dp and 800 dp width caps on the profile, the project grid and the reader; verify UI tests in a 1400 px wide window show the profile content, the grid and the reader spanning the whole width, and update the existing wide-window reader test (it asserted the 800 dp cap)

## 2. Compact project grid on the profile

- [x] 2.1 Rebuild `HomeContent` as an adaptive `LazyVerticalGrid` (minimum cell 220 dp) with full-span header, links and "Projects" title and type-prefixed keys; verify tests for several cards per row in a wide window, one per row in a narrow one, owner order left to right then top to bottom, and ids overlapping between a link and a project not crashing
- [x] 2.2 Make project cards uniform: 16:9 thumbnail or placeholder, one-line title, date, and a summary reserved to exactly 3 lines with an ellipsis; verify tests that cards with a long summary, a short summary, no summary and no thumbnail all have identical width and height, and that a long summary is cut after 3 lines
- [x] 2.3 Verify rendered screenshots of the profile in a phone-width and a tablet/web-width window, in light and dark, and that the empty-projects state and the link rows still look right

## 3. Standard icons

- [x] 3.1 Add the Material icons dependency and verify `./kotlin build` for jvm, android, iOS and wasmJs; measure the web bundle size before and after, and if the increase is significant replace the dependency with the few needed icon vectors copied from the Material icon paths and note it in design.md
- [x] 3.2 Turn `ThemePicker` into an icon button (BrightnessAuto, LightMode or DarkMode for the current choice, content description "Theme: <choice>") that opens the existing menu, and turn the project page's back control into an `IconButton` with `Icons.AutoMirrored.Filled.ArrowBack` labelled "Back"; verify updated tests for the icon button labels, selecting each theme, the back control calling back, and the admin rail still working, and look at screenshots in both themes

## 4. Auto-hiding bars (Android and iOS)

- [x] 4.1 Add a `BarsVisibility` state holder and a `NestedScrollConnection` (hide after scrolling down past a threshold, show on any upward scroll, always shown at the top); verify unit tests for the thresholds, direction changes, the top, and that small jitters do not toggle it
- [x] 4.2 Draw the project top bar as an animated overlay with constant content top padding, attach the scroll connection only on `Mobile`, hide and show the bar with it in both grid and reader, and report the state through `onSystemBarsVisibleChanged` (also restoring visible when leaving the project and on dispose); verify UI tests: a swipe up hides the bar and reports false, a swipe down shows it and reports true, the content position does not change when the bar hides, leaving the project reports true, and on `Web` the bar never hides
- [x] 4.3 Implement the Android side in `MainActivity` (hide and show the status bar with `WindowInsetsControllerCompat`, transient-by-swipe behavior, short-edge cutout mode) and the iOS side (callback into `ViewController`, a `@State` flag and `.statusBarHidden` in `iosApp.swift`); verify both build for their targets
- [x] 4.4 Check on an Android device and an iOS device: scrolling down a project in the reader hides the status bar and the top bar, scrolling up brings them back, the content does not jump, a notch device is not letterboxed, and going back to the profile restores the status bar

## 5. Web URL navigation

- [x] 5.1 Add `Route` with pure `parse` and `format` for `#/` and `#/projects/<id>` and a `RouteHost` interface; verify unit tests for valid routes, an empty hash, a missing or non-numeric id, extra slashes, trailing paths and other junk all parsing to the profile, and that `parse(format(route))` round-trips
- [x] 5.2 Drive `ClientNavigation` from the route (initial route from the host, `push` when a project opens, state updated when the host reports a change, in-memory behavior when there is no host); verify UI tests with a fake host: opening a project pushes the right route, an external change to Home or to a project switches the page, a deep link to a project opens it directly, and an unknown route shows the profile
- [x] 5.3 Hide the project page's back control and disable `BackHandler` when the platform is `Web` and keep them on `Mobile`; verify UI tests for both platforms
- [x] 5.4 Implement the hash `RouteHost` in the web shell with `window.location.hash` and a `hashchange` listener (replacing an unrecognised hash with `#/`) and pass it from `main.kt`; verify the web app builds
- [x] 5.5 Check in a browser: opening a project changes the URL to `#/projects/<id>`, browser back and forward move between the pages, reload keeps the page, a link pasted into a new tab opens that project, an unknown hash shows the profile, there is no Back button on the project page, and a missing project id shows the retry error

## 6. Integration

- [x] 6.1 Run `./kotlin build` for all targets and `./kotlin test`, update the README for the new behavior, and verify everything passes
- [x] 6.2 Walk through the whole flow on a phone, a tablet or wide window, and the web: full-width layout, compact profile cards, hiding bars while reading, icons, and web URLs in both themes; verify nothing regresses

## 7. Profile proportions on wide screens

- [x] 7.1 Cap the profile cover's height at 240 dp (full width, centre-cropped, unchanged on phones) and limit the description and each link row to 720 dp, left-aligned; verify UI tests in a 1600 px window for cover width 1600 and height 240, description and link rows at most 720 dp wide and starting at the page gutter, tap areas no wider than the rows, phone width unchanged, and the avatar still overlapping the cover's bottom edge
- [x] 7.2 Verify rendered screenshots at phone and wide widths in light and dark, and that the project cards below still fill the full width
