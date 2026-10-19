# Spec Delta

## ADDED Requirements

### Requirement: Gallery views
The client project detail screen SHALL offer two views of a project's images, a grid and a continuous vertical reader, and SHALL let the visitor switch between them on that screen. The grid SHALL be shown first.

#### Scenario: Default view
- **WHEN** the visitor opens a project
- **THEN** the images are shown in the grid

#### Scenario: Switch to the reader
- **WHEN** the visitor chooses the reader view
- **THEN** the images are shown as one continuous vertical strip, in the owner's order

#### Scenario: Switch back
- **WHEN** the visitor chooses the grid view again
- **THEN** the grid is shown

### Requirement: Continuous reader
In the reader view the client SHALL show every image at the full width of the screen, one directly below the next with no gaps or cropping, keeping each image's proportions, and the whole strip SHALL scroll as one continuous page.

#### Scenario: Seamless strip
- **WHEN** the visitor scrolls the reader
- **THEN** consecutive images touch each other and nothing is cropped

#### Scenario: Layout does not jump
- **WHEN** images are still loading while the visitor scrolls
- **THEN** each image already occupies the height its proportions require, so the page does not shift as images appear

#### Scenario: Wide window
- **WHEN** the screen is wider than a phone
- **THEN** the strip is centred at a comfortable maximum width rather than stretched edge to edge

#### Scenario: No images
- **WHEN** the project has no images
- **THEN** the reader shows the same "no images" message as the grid

### Requirement: Pull to refresh
On Android and iOS, pulling down on the profile screen or the project detail screen SHALL reload that screen's content from the server and show a progress indicator while it loads. The web client SHALL NOT offer it.

#### Scenario: Refresh the profile
- **WHEN** a visitor on Android or iOS pulls down on the profile screen while it is at the top
- **THEN** the profile, links and projects are reloaded and updated content is shown

#### Scenario: Refresh a project
- **WHEN** a visitor on Android or iOS pulls down on a project screen
- **THEN** that project is reloaded, and its images appear in the owner's current order

#### Scenario: Refresh fails
- **WHEN** the reload fails because the server is unreachable
- **THEN** the content already on screen stays visible and a brief error with a retry action is shown

#### Scenario: Web client
- **WHEN** the client runs in a web browser
- **THEN** pulling down does not trigger a refresh

#### Scenario: View and position are kept
- **WHEN** a refresh completes
- **THEN** the selected gallery view stays the same

## MODIFIED Requirements

### Requirement: Profile screen
The client SHALL show the cover image, profile picture, name, description and public social links in the owner's order. Each link SHALL be shown as a full-width row with a large icon, the link's name and its address beneath the name, and tapping the row SHALL open the address.

#### Scenario: Populated
- **WHEN** the profile has content
- **THEN** all of it is shown, with only public links

#### Scenario: Placeholders
- **WHEN** the profile is still placeholder
- **THEN** the screen renders without errors and shows the placeholder text and no images

#### Scenario: Link shows its address
- **WHEN** a public link is shown
- **THEN** its name and its full address are both visible in the row

#### Scenario: Larger icons
- **WHEN** a link has an icon
- **THEN** the icon is shown clearly larger than the text height, at least 40 dp, with a neutral placeholder of the same size while it loads or when there is none

#### Scenario: Long address
- **WHEN** a link address is too long for the row
- **THEN** it is shortened with an ellipsis rather than overflowing or hiding the name

#### Scenario: Open a link
- **WHEN** the visitor taps a link row
- **THEN** the link's address is opened

### Requirement: Efficient image loading
The client SHALL use thumbnail variants for grids, icons and avatars and originals for the cover, the continuous reader and full-size viewing.

#### Scenario: Grid
- **WHEN** a list of projects is shown
- **THEN** thumbnail variants are requested

#### Scenario: Reader
- **WHEN** the continuous reader is shown
- **THEN** the original images are requested, loaded as they scroll into view
