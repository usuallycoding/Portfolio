# Proposal

## Why

A very long description or summary, or a long list of social links or projects, pushes everything else far down the page. Visitors should see a tidy preview and be able to open or expand the rest only if they want it.

## What Changes

- **Long text preview.** If the profile description or a project's summary needs more than 10 rows at its current width, only the first 10 rows are shown, ending with an ellipsis. Tapping or clicking the description or summary, truncated or not, opens a Material bottom sheet with the full text, a close (X) icon in its corner, and a copy icon that copies the full text to the clipboard and confirms briefly. Text that fits in 10 rows is shown in full (no ellipsis) and opens the same sheet when tapped. This applies to the profile description and to the project summary in both the grid and reader views.
- **Collapsible social links.** If the links take more than 5 rows (one link per row on small screens, wrapped lines of the wrapping row on wide screens), only the first 5 rows are shown, with a small centered expand icon button below. Expanded, all links are shown with a "show less" icon button. With 5 rows or fewer, no icon appears.
- **Collapsible projects.** Rows of project cards behave the same way, but sooner: more than 2 rows shows the first 2 plus an expand icon, and expanded shows all rows plus a "show less" icon.
- Expanded or collapsed state is kept while the visitor stays on the profile (including after opening a project and coming back) and starts collapsed again when the app is reopened.
- The centered layout, the column width and the equal-size project cards are unchanged.
- Out of scope: any server or admin change, truncating the profile name or project titles, and persisting the expanded state between launches.

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-app`: preview and bottom sheet for long text, and collapsible social links and project rows.

## Impact

- `app/clientShared`: line-limited text with overflow detection and a tap target, a bottom sheet with close and copy controls, a collapse rule for social link rows and project card rows, and the expand/collapse icon buttons.
- Uses the Material icons library already in the project (expand, close, copy); no new dependency is expected beyond a clipboard API from Compose.
- No server, database, API or admin changes.
