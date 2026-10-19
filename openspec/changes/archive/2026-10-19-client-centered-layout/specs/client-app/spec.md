# Spec Delta

## ADDED Requirements

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

## RENAMED Requirements

- FROM: `### Requirement: Full-width layout`
- TO: `### Requirement: Centered content column`

## MODIFIED Requirements

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
