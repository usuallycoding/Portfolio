# Spec Delta

## ADDED Requirements

### Requirement: Full-width layout
The client SHALL use the full width of the screen for the profile's cover image and project cards, the project grid and the reader, on every screen size. To stay comfortable to look at and read on wide screens, the profile's cover image SHALL be no taller than 240 dp, and the profile's description and social link rows SHALL be no wider than 720 dp.

#### Scenario: Large screen
- **WHEN** the client runs in a wide window, such as a desktop browser or a tablet
- **THEN** the cover image, the project cards, the project grid and the reader extend across the full width of the window

#### Scenario: Cover height
- **WHEN** the window is so wide that a 3:1 cover would be taller than 240 dp
- **THEN** the cover is shown 240 dp tall at full width, cropped at the top and bottom around its centre rather than stretched

#### Scenario: Readable text width
- **WHEN** the window is wider than 720 dp
- **THEN** the description and each social link row are at most 720 dp wide, aligned with the page's left edge, and a link row's tap area is no wider than its row

#### Scenario: Phone
- **WHEN** the client runs on a phone
- **THEN** the cover keeps its 3:1 shape and the description and links use the full width, as before

### Requirement: Auto-hiding bars
On Android and iOS, scrolling down on a project page SHALL hide the system status bar and the project's top bar, and scrolling up SHALL show them again. The bars SHALL always be shown when the page is at its top, and SHALL be restored when the visitor leaves the project page. The web client SHALL keep its top bar visible.

#### Scenario: Scroll down
- **WHEN** a visitor on Android or iOS scrolls down a project page, in the grid or the reader
- **THEN** the status bar and the top bar hide, and the content uses the full screen

#### Scenario: Scroll up
- **WHEN** the visitor scrolls up
- **THEN** the status bar and the top bar are shown again

#### Scenario: At the top
- **WHEN** the project page is scrolled to its very top
- **THEN** the bars are shown

#### Scenario: Leave the project
- **WHEN** the visitor goes back to the profile while the bars are hidden
- **THEN** the status bar and the top bar are shown

#### Scenario: Content does not jump
- **WHEN** the bars hide or show
- **THEN** the content on screen does not shift position because of the status bar

#### Scenario: Web
- **WHEN** the client runs in a web browser
- **THEN** scrolling does not hide the top bar

### Requirement: Standard icons
The theme control and the back control SHALL use the standard Material icons, each with an accessible label, and the theme control's icon SHALL reflect the current choice.

#### Scenario: Theme control
- **WHEN** the theme control is shown
- **THEN** it is an icon button whose icon matches the current choice (System, Light or Dark) and whose label names the choice

#### Scenario: Back control
- **WHEN** the back control is shown
- **THEN** it is the standard back arrow icon labelled "Back"

### Requirement: Back control by platform
On Android and iOS the project page SHALL show a back control that returns to the profile. On the web the project page SHALL NOT show a back control, and navigation back SHALL be done with the browser's own back.

#### Scenario: Mobile
- **WHEN** a project page is shown on Android or iOS
- **THEN** a back control is shown and returns to the profile

#### Scenario: Web
- **WHEN** a project page is shown in a web browser
- **THEN** there is no back control, and the browser's back button returns to the profile

### Requirement: Web URL navigation
The web client SHALL reflect the current page in the URL using hash URLs: `#/` for the profile and `#/projects/<id>` for a project. Opening a project SHALL add a history entry, the browser's back and forward buttons SHALL move between pages, and loading a URL directly SHALL show that page.

#### Scenario: Open a project
- **WHEN** a visitor opens a project on the web
- **THEN** the URL becomes `#/projects/<id>` and a history entry is added

#### Scenario: Browser back and forward
- **WHEN** the visitor presses the browser's back button and then forward
- **THEN** the profile and then the project are shown, matching the URL

#### Scenario: Reload
- **WHEN** the visitor reloads the page while on `#/projects/<id>`
- **THEN** the same project is shown again

#### Scenario: Shared link
- **WHEN** a visitor opens a link containing `#/projects/<id>` in a new tab
- **THEN** that project is shown directly, and going back from there leaves the app or shows the profile

#### Scenario: Unknown address
- **WHEN** the URL is empty or does not match a known page
- **THEN** the profile is shown

#### Scenario: Missing project
- **WHEN** the URL names a project that does not exist or is not public
- **THEN** the project page shows the same recoverable error as any failed load, and the visitor can reach the profile with the browser's back or by editing the URL

## MODIFIED Requirements

### Requirement: Project list
The client SHALL list public projects in the owner's order as compact cards in an adaptive grid with several cards per row, each showing the thumbnail, title, date and a summary truncated to 3 lines. Every card SHALL have exactly the same size.

#### Scenario: List
- **WHEN** the visitor opens the projects section
- **THEN** public projects appear in owner order

#### Scenario: Empty
- **WHEN** there are no public projects
- **THEN** an empty state is shown

#### Scenario: Several per row
- **WHEN** the screen is wide enough for more than one card
- **THEN** the cards are arranged several to a row, filling the width, in owner order from left to right and top to bottom

#### Scenario: Same size
- **WHEN** projects have summaries of different lengths, or none, or no thumbnail
- **THEN** every card has the same width and the same height

#### Scenario: Long summary
- **WHEN** a summary needs more than 3 lines
- **THEN** it is cut after 3 lines with an ellipsis

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
- **THEN** the strip spans the full width of the screen, like on a phone

#### Scenario: No images
- **WHEN** the project has no images
- **THEN** the reader shows the same "no images" message as the grid
