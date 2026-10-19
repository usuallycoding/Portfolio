# Spec Delta

## ADDED Requirements

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
