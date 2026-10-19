# client-app Specification

## Purpose

Gives visitors read-only Android, iOS and web apps to view the profile, public social links and public projects with their images.

## Requirements

### Requirement: Client platforms and read-only behavior
The client apps SHALL run on Android, iOS and web, and SHALL NOT offer any editing or login.

#### Scenario: No editing
- **WHEN** a visitor uses a client app
- **THEN** no control creates, edits, deletes or uploads content

### Requirement: Profile screen
The client SHALL show the cover image, profile picture, name, description and public social links in the owner's order, centered horizontally, with the picture on the cover's bottom edge and the description's text left-aligned. Links SHALL wrap in a centered row on wide screens and take one row each on smaller screens, and tapping a link SHALL open its address.

#### Scenario: Populated
- **WHEN** the profile has content
- **THEN** all of it is shown, with only public links

#### Scenario: Placeholders
- **WHEN** the profile is still placeholder
- **THEN** the screen renders without errors and shows the placeholder text and no images

#### Scenario: Centered profile
- **WHEN** the profile is shown
- **THEN** the profile picture straddles the bottom edge of the cover at its horizontal centre, and the name and the description block are centered horizontally

#### Scenario: Left-aligned description
- **WHEN** the description needs more than one line
- **THEN** its lines are left-aligned within the centered block, not centered line by line

#### Scenario: Socials title
- **WHEN** the profile has at least one public link
- **THEN** a "Socials" title, centered horizontally, is shown above the links, and when there are no public links neither the title nor the links are shown

#### Scenario: Links wrap on wide screens
- **WHEN** the screen is wide and the links do not fit on one line of the column
- **THEN** they wrap onto further lines, each line centered, with the links keeping their natural width

#### Scenario: One link per row on small screens
- **WHEN** the screen is narrower than 840 dp
- **THEN** each link is on its own full-width row, with its icon and text aligned to the left edge as before, not centered in the row

#### Scenario: Link shows its address
- **WHEN** a public link is shown
- **THEN** its name and its full address are both visible

#### Scenario: Larger icons
- **WHEN** a link has an icon
- **THEN** the icon is shown clearly larger than the text height, at least 40 dp, with a neutral placeholder of the same size while it loads or when there is none

#### Scenario: Long address
- **WHEN** a link address is too long for its space
- **THEN** it is shortened with an ellipsis rather than overflowing or hiding the name

#### Scenario: Open a link
- **WHEN** the visitor taps a link
- **THEN** the link's address is opened

### Requirement: Project list
The client SHALL list public projects in the owner's order under a centered "Projects" title, as compact cards in an adaptive grid with several cards per row inside the content column, each showing the thumbnail, title, date and a summary truncated to 3 lines. Every card SHALL have exactly the same size.

#### Scenario: List
- **WHEN** the visitor opens the projects section
- **THEN** public projects appear in owner order

#### Scenario: Empty
- **WHEN** there are no public projects
- **THEN** an empty state is shown

#### Scenario: Several per row
- **WHEN** the content column is wide enough for more than one card
- **THEN** the cards are arranged several to a row, filling the column, in owner order from left to right and top to bottom

#### Scenario: Same size
- **WHEN** projects have summaries of different lengths, or none, or no thumbnail
- **THEN** every card has the same width and the same height

#### Scenario: Long summary
- **WHEN** a summary needs more than 3 lines
- **THEN** it is cut after 3 lines with an ellipsis

#### Scenario: Centered title
- **WHEN** the projects section is shown
- **THEN** its "Projects" title is centered horizontally

### Requirement: Project detail
Selecting a project SHALL open a detail screen with its title, date, summary and all images in the owner's order, and SHALL allow viewing an image at full size.

#### Scenario: Open
- **WHEN** the visitor selects a project
- **THEN** its images are shown in order

#### Scenario: Back
- **WHEN** the visitor navigates back
- **THEN** the project list is shown again

### Requirement: Efficient image loading
The client SHALL use thumbnail variants for grids, icons and avatars and originals for the cover, the continuous reader and full-size viewing.

#### Scenario: Grid
- **WHEN** a list of projects is shown
- **THEN** thumbnail variants are requested

#### Scenario: Reader
- **WHEN** the continuous reader is shown
- **THEN** the original images are requested, loaded as they scroll into view

### Requirement: Failure handling
The client SHALL show a recoverable error state with a retry action when the server cannot be reached.

#### Scenario: Offline
- **WHEN** the server is unreachable
- **THEN** an error with retry is shown instead of a crash

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
- **THEN** the strip spans the full width of the screen, like on a phone

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

### Requirement: Centered content column
The profile's cover SHALL span the full screen width, at most 240 dp tall. On screens at least 840 dp wide, the description, social links and projects, and a project page's date, summary, Grid / Reader switch and grid SHALL share a centered column half the screen wide (at least 560 dp); on narrower screens they SHALL use the full width. The reader's images always span the full screen width.

#### Scenario: Large screen
- **WHEN** the screen is 840 dp wide or more
- **THEN** the description, the social links, the projects, and on a project page the date, summary, switch and grid are each in a column half the screen wide, centered horizontally

#### Scenario: Cover height
- **WHEN** the window is so wide that a 3:1 cover would be taller than 240 dp
- **THEN** the cover is shown 240 dp tall at full width, cropped at the top and bottom around its centre rather than stretched

#### Scenario: Readable text width
- **WHEN** the screen is wider than the column
- **THEN** the description and the social links never run wider than the content column, so lines of text stay short

#### Scenario: Phone
- **WHEN** the screen is narrower than 840 dp
- **THEN** the cover keeps its 3:1 shape and the content uses the full width

#### Scenario: Minimum column width
- **WHEN** the screen is between 840 dp and 1120 dp wide
- **THEN** the column is 560 dp wide, which is more than half the screen, and is centered

#### Scenario: Reader images stay full width
- **WHEN** the reader view is shown on a screen of any width
- **THEN** every image spans the full width of the screen, edge to edge, while the date, summary and switch above the images stay in the centered column

#### Scenario: Content outside the column scrolls too
- **WHEN** the visitor scrolls with the pointer over the empty space beside the column
- **THEN** the page scrolls as it does over the column

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

### Requirement: Project page layout
On the project page the title in the top bar SHALL be centered horizontally, with the back control (where shown) at the start. The date, the summary and the Grid / Reader switch SHALL sit centered in the content column, with the date and the summary's text left-aligned (not centered) inside it.

#### Scenario: Centered title
- **WHEN** a project page is shown
- **THEN** the project title is centered on the screen's width, and the back control, where the platform shows one, stays at the start edge

#### Scenario: Back control width
- **WHEN** the project title is long, with or without a back control
- **THEN** it stays exactly centered on the screen, the same space is kept free on both sides, and it is shortened with an ellipsis rather than moving off center or running under the back control

#### Scenario: Centered header
- **WHEN** a project page is shown on a wide screen, in the grid or the reader view
- **THEN** the date, summary and switch sit centered in the content column, which is half the screen width and centered horizontally, while the date and summary text itself is left-aligned

#### Scenario: Phone
- **WHEN** a project page is shown on a phone
- **THEN** the switch is centered, the date and summary use the full width, and their text is left-aligned

### Requirement: Image viewer controls
In the full-screen image viewer the left and right arrow keys SHALL show the previous and next image, stopping at the first and last image, and the viewer SHALL be closed with the standard Material close icon button, labelled "Close", instead of the word "Close".

#### Scenario: Next image
- **WHEN** the viewer is open and the visitor presses the right arrow key
- **THEN** the next image is shown, and the position indicator updates

#### Scenario: Previous image
- **WHEN** the viewer is open and the visitor presses the left arrow key
- **THEN** the previous image is shown

#### Scenario: Ends
- **WHEN** the first image is shown and the visitor presses the left arrow key, or the last image is shown and the visitor presses the right arrow key
- **THEN** the image does not change and nothing wraps around

#### Scenario: Keys work as soon as it opens
- **WHEN** the viewer opens
- **THEN** the arrow keys work immediately, without the visitor first clicking in it

#### Scenario: Close control
- **WHEN** the viewer is shown
- **THEN** a Material close icon button labelled "Close" is shown at the top end, and activating it closes the viewer

#### Scenario: Swipe still works
- **WHEN** the visitor swipes horizontally in the viewer
- **THEN** the previous or next image is shown, as before

### Requirement: Long text preview
The client SHALL show at most 10 rows of the profile description and of a project's summary, ending with an ellipsis when more is hidden, and tapping or clicking the description or summary, whether or not it is truncated, SHALL open a bottom sheet with the full text, a close icon button and a copy icon button.

#### Scenario: Long text is truncated
- **WHEN** the description or summary needs more than 10 rows at the current width
- **THEN** only the first 10 rows are shown, the last ending with an ellipsis

#### Scenario: Short text is shown in full
- **WHEN** the text fits in 10 rows or fewer
- **THEN** all of it is shown, with no ellipsis

#### Scenario: Short text opens the sheet too
- **WHEN** the visitor taps or clicks text that fits in 10 rows
- **THEN** the bottom sheet opens with the same text, the close icon and the copy icon

#### Scenario: Width decides
- **WHEN** the window is resized so the same text takes more or fewer than 10 rows
- **THEN** it becomes truncated, or is shown in full, accordingly, and is tappable either way

#### Scenario: Open the full text
- **WHEN** the visitor taps or clicks truncated text
- **THEN** a bottom sheet opens showing the full text, which scrolls if it is longer than the sheet

#### Scenario: Close the sheet
- **WHEN** the visitor activates the close (X) icon button, taps outside the sheet, or uses the system back
- **THEN** the sheet closes and the page behind it is unchanged

#### Scenario: Copy the text
- **WHEN** the visitor activates the copy icon button
- **THEN** the full text is placed on the clipboard and a brief confirmation is shown

#### Scenario: Both places
- **WHEN** a project summary is shown on the project page
- **THEN** it follows the same rule in the grid view and in the reader view, as the profile description does

### Requirement: Collapsible social links
When the social links take more than 5 rows, the client SHALL show only the first 5 rows and a small expand icon button, and expanding SHALL show all links with a "show less" icon button. With 5 rows or fewer no icon SHALL be shown. A row is one link on a screen narrower than 840 dp and one wrapped line of links on a wide screen.

#### Scenario: Collapsed by default
- **WHEN** the profile opens and the links take more than 5 rows
- **THEN** the first 5 rows are shown and the expand icon, labelled "Show more socials", is shown (centered below the links on a narrow screen, at the end of the fifth line on a wide one)

#### Scenario: Icon placement
- **WHEN** the icon is shown
- **THEN** it is centered below the links on a screen narrower than 840 dp, and at the end of the fifth line of the wrapping row on a wide screen

#### Scenario: Expand
- **WHEN** the visitor activates the expand icon
- **THEN** all links are shown, and the icon becomes "Show fewer socials"

#### Scenario: Collapse
- **WHEN** the visitor activates the "Show fewer socials" icon
- **THEN** only the first 5 rows are shown again

#### Scenario: Five rows or fewer
- **WHEN** the links take 5 rows or fewer
- **THEN** all links are shown and there is no expand or collapse icon

#### Scenario: Rows depend on width
- **WHEN** the window is resized so the wrapped links take more or fewer than 5 rows
- **THEN** the icon appears or disappears accordingly, and an expanded list stays expanded

#### Scenario: State is kept on the profile
- **WHEN** the visitor expands the links, opens a project and comes back
- **THEN** the links are still expanded; after the app is reopened they start collapsed

### Requirement: Collapsible project list
When the project cards take more than 2 rows, the client SHALL show only the first 2 rows of cards and a small centered expand icon button, and expanding SHALL show all rows with a "show less" icon button that collapses them again. With 2 rows or fewer no icon SHALL be shown.

#### Scenario: Collapsed by default
- **WHEN** the profile opens and the projects take more than 2 rows of cards
- **THEN** the first 2 rows are shown in owner order and the expand icon, labelled "Show more projects", is shown centered below them

#### Scenario: Expand
- **WHEN** the visitor activates the expand icon
- **THEN** all rows are shown, and the icon becomes "Show fewer projects"

#### Scenario: Collapse
- **WHEN** the visitor activates the "Show fewer projects" icon
- **THEN** only the first 2 rows are shown again

#### Scenario: Two rows or fewer
- **WHEN** the projects take 2 rows or fewer
- **THEN** all cards are shown and there is no icon

#### Scenario: Rows depend on width
- **WHEN** the window is resized so the number of card columns changes
- **THEN** the number of rows, and so whether the icon is shown, changes accordingly, and the cards stay the same size

#### Scenario: State is kept on the profile
- **WHEN** the visitor expands the projects, opens a project and comes back
- **THEN** the projects are still expanded; after the app is reopened they start collapsed
