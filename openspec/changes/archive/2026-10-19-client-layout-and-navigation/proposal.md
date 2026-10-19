# Proposal

## Why

On large screens (web, tablets) the client apps sit in a narrow column with empty margins, the profile lists projects one huge card per row, and the reader cannot use the whole screen. On phones the status bar and top bar keep taking space while reading a comic. The web app has no URLs, so reload, bookmarks and the browser back button do not work, and it shows a Back button that duplicates the browser's. The theme and back controls are plain text rather than standard icons.

## What Changes

- **Full width.** The client uses the full width of the screen on the profile, the project grid and the reader. The 720 dp and 800 dp maximum widths are removed.
- **Auto-hiding bars (Android and iOS).** Scrolling down on a project page hides the system status bar and the project's top bar; scrolling up shows them again. The web client keeps its top bar visible.
- **Compact project cards on the profile.** Projects are shown in an adaptive grid with several cards per row. Each card shows a thumbnail, title, date and a summary truncated to 3 lines, and every card is exactly the same size.
- **Standard icons.** The theme control and the back control use the default Material icons, with accessible labels.
- **Web URL navigation.** The web client uses hash URLs: `#/` for the profile and `#/projects/<id>` for a project. Browser back and forward, reload and shared links work on any static host. The project page on web has no Back button, since the browser's back is used. Android and iOS keep their Back button.
- Out of scope: any server or admin change, URLs for the full-screen image viewer or the grid/reader choice, path-style URLs, and hiding bars on the profile page.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: full-width layout; compact equal-size project cards; the reader's wide-window behavior changes from a capped width to full width; auto-hiding bars on Android and iOS; standard icons for theme and back; web hash URLs and no Back button on web.

## Impact

- `app/clientShared`: profile layout (LazyColumn becomes an adaptive grid), project card, removal of width caps, scroll-driven bar visibility, a route model with a platform hook for web navigation, icon controls.
- `app/sharedUi`: the theme picker becomes an icon button, so the admin app's picker changes appearance too. New dependency for Material icons.
- `app/androidApp`: hide and show the system bars on request.
- `app/iosApp`: hide and show the status bar from the SwiftUI host.
- `app/clientWebApp`: a hash-URL implementation of the route hook using the browser's location and history.
- No server, database or API changes.
