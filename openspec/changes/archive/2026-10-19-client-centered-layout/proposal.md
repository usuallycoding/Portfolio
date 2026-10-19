# Proposal

## Why

On wide screens the profile and project pages stretch to the left edge and across the whole window: text runs in very long lines, the social links stack in a single narrow column, and everything hugs the left. The content should sit in a balanced, centered column. The full-screen image viewer also lacks keyboard navigation on web and desktop-class devices, and its "Close" is plain text.

## What Changes

- **Centered content column.** On wide screens (at least 840 dp) the profile's description, social links and projects, and the project page's date, description, switch and image grid, share one centered column that is half the screen wide (never narrower than 560 dp). Below 840 dp everything stays full width. This replaces the earlier 720 dp text width and the full-width grid. The reader's images stay full width.
- **Centered profile.** The avatar is centered on the cover's bottom edge, and the name, description, "Socials" and "Projects" titles and their content are centered, with text centered too. The cover keeps its full width and 240 dp height cap.
- **Social links.** A new centered "Socials" title appears above them. On wide screens the links form a centered wrapping row (FlowRow) at their natural width; on smaller screens each link stays on its own row.
- **Project page.** The top bar title is centered (the back control stays at the start). The date, description and Grid / Reader switch are centered text in the column, and the image grid sits in the same centered column. The reader's images are the exception: they keep spanning the full width of the screen.
- **Image viewer.** Left and right arrow keys go to the previous and next image (stopping at the ends), and the text "Close" becomes the standard Material close icon button.
- Out of scope: server or admin changes, new routes, the cover's size, and any change to phone layouts other than centering.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: centered content column on wide screens, centered profile and project page, social links title and wrapping row, and keyboard and icon controls in the image viewer.

## Impact

- `app/clientShared`: a shared content-column width rule, profile layout (avatar, text, titles, links, project rows), project page layout (top bar, header, grid, reader), and the image viewer.
- No new dependencies beyond the Material icons already in use; no server, database, API or admin changes.
- Existing layout tests that assert the previous widths (720 dp text, full-width grid and reader, left-aligned items) are updated to the new rules.
