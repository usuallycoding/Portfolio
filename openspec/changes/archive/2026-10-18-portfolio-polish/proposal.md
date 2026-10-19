# Proposal

## Why

The first release works end to end, but the apps are light-only, the client shows social links as small unlabeled chips, a project's images can only be browsed as a grid, and visitors on phones have no way to refresh content without restarting the app. These four improvements make the apps comfortable to use day to day and better suited to image-heavy projects such as comics.

## What Changes

- **Themes.** Every app (client and admin) gets a light and a dark theme and follows the device setting by default. The user can override it with a Light, Dark or System choice, which is remembered on that device.
- **Social links in the client.** Each link shows its address beneath its name, and link icons are larger. Links become full-width rows that open the address when tapped.
- **Continuous reader.** The client's project page can show the gallery either as the existing grid or as a vertical, continuous strip of full-width images in the owner's order, for reading comics and manga. The visitor switches between the two views on the project page.
- **Pull to refresh.** On Android and iOS only, pulling down on the profile page or the project page reloads its content. The web client does not offer it.
- Out of scope: any server or API change, new admin editing features, pinch-zoom in the reader, and remembering reading position.

## Capabilities

### New Capabilities
- `app-theme`: light and dark themes for all apps, following the device by default, with a remembered Light / Dark / System choice.

### Modified Capabilities
- `client-app`: the profile screen shows link addresses and larger icons; the project detail screen offers a continuous vertical reader besides the grid; Android and iOS support pull to refresh on the profile and project screens.

## Impact

- `app/sharedUi`: theme definitions (light and dark color schemes), a theme preference holder, and a small per-device storage abstraction. New dependency for storing a setting on jvm, android, ios and wasmJs.
- `app/clientShared`: profile links layout, project gallery view modes and reader, pull-to-refresh container gated to Android and iOS, a theme picker.
- `app/adminShared`: apply the theme and offer the theme picker.
- App shells (`androidApp`, `iosApp`, `clientWebApp`, `desktopApp`, `adminWebApp`): provide device-specific theme detection and storage where the shared code needs it.
- No server, database or API changes.
