# Design

## Context

See proposal.md for motivation. Current client code this change touches (all in `app/clientShared`):

- `HomeContent` is one `LazyColumn` inside a `BoxWithConstraints`. The cover is full width with a capped height; the avatar is `BottomStart`; the name, description and link rows are left-aligned with 16 dp gutters; link rows are capped at 720 dp; projects are lazy rows of equal-width cards sized by `projectColumns(width)`.
- `ProjectContent` has an overlay `TopAppBar` (left-aligned title), a `LazyVerticalGrid` and a `LazyColumn` reader, both full width, and `ProjectHeader` (date, summary, Grid/Reader switch) left-aligned. A constant top padding keeps content below the overlay bar.
- `ImageViewer` is a black overlay with a `HorizontalPager`, a position counter, and a `TextButton("Close")`. It has no keyboard handling.
- The apps already know their window width (`BoxWithConstraints`) and the platform (`ClientPlatform`).

## Goals / Non-Goals

**Goals:**
- One rule for the content column, used by every part of both pages, so they always line up.
- Scrolling still works over the empty margins on wide screens.
- Keyboard navigation in the viewer that works the moment it opens.

**Non-Goals:**
- Changing the cover's height cap, the card minimum width, or any phone layout beyond centering.
- Left/right keys on the project page itself; they are only for the viewer.
- Narrowing the reader's images: they stay full screen width by request.
- Escape to close the viewer, or arrow-key navigation anywhere else.

## Decisions

### One column rule
A pure function `contentColumnWidth(screenWidth: Dp): Dp` returns the full width below 840 dp and otherwise `max(screenWidth / 2, 560 dp)` (never more than the screen). Every wide-screen rule is expressed with it, so the description, links, projects, project header and grid always share the same column. The reader's images are deliberately not in it: a comic page should use the whole screen width. The 560 dp floor avoids content jumping from full width to a very narrow column when a window is resized across 840 dp: just above the threshold the column is 560 dp (two thirds of the screen) and reaches exactly half at 1120 dp.

Alternative considered: a fixed maximum width. Rejected because the requirement is "half the screen" and proportional.

### Keeping the scroll area full width
The `LazyColumn` and `LazyVerticalGrid` stay full width so the mouse wheel and touch scroll anywhere on the page; the column is made by centering each item at the column width (`horizontalAlignment = CenterHorizontally` plus `Modifier.width(columnWidth)` per item) or by horizontal content padding of `(screen - column) / 2` (the grid). A narrow scrolling container would stop scrolling when the pointer is beside it.

### Profile
- Avatar: `Alignment.BottomCenter` on the cover, same half-height overlap.
- Name: `TextAlign.Center`. Description: a block centered at the column width, but its text is start-aligned (centered text is hard to read over several lines). The theme button stays at the cover's top end.
- "Socials" title (shown only when there are public links) and "Projects" title: centered text.
- Links on wide screens (`screen >= 840 dp`): a `FlowRow` in the column with `Arrangement.spacedBy(.., CenterHorizontally)`, each link a compact clickable item at its natural width (icon, name, address with an ellipsis at a maximum item width so one long address cannot take a whole line). On smaller screens: one link per row at full width, with the icon and text aligned to the start as before.
- Projects: the existing lazy rows, but their width is the column width and `projectColumns` is computed from it; the rows are centered.

### Project page
- Top bar: Material3 `CenterAlignedTopAppBar` replaces `TopAppBar`; the navigation icon stays at the start. The bar centres the title between its navigation and action slots, so a long title sits off centre by half the back control's width; both slots are therefore always filled with a same-size slot (the back control or an empty 48 dp slot, and an empty 48 dp slot opposite), which keeps the title exactly centered with or without a back control. The overlay behavior and the constant content padding are unchanged.
- Header: the date and summary block and the Grid/Reader switch are centered in the column width; the date and summary text is start-aligned, and only the switch and the title are centered.
- Grid: the grid's horizontal content padding is `(screen - column) / 2 + 16 dp`, so the cells live in the column while the scroll container is full width. Reader: unchanged images, each at the full screen width, edge to edge, with its proportional height; only the header item above the images (date, summary, switch) is centered at the column width.

### Image viewer
- Close: `IconButton` with `Icons.Filled.Close` (white tint, content description "Close"); the Material icons dependency is already present.
- Keys: `Modifier.onPreviewKeyEvent` on the viewer handling `Key.DirectionRight` and `Key.DirectionLeft` on key-down, calling `pager.animateScrollToPage(current ± 1)` when that page exists. A `FocusRequester` focuses the viewer in a `LaunchedEffect` when it opens, so the keys work without a click, and the viewer's container is `focusable()`. At the ends the key does nothing (no wrapping), and the event is consumed only when it was acted on.

### Adjustments made during implementation
- **Card width.** Project cards get one explicit width (`(column - gutters - gaps) / columns`) instead of sharing the row with weights, because weights can leave neighbouring cards one pixel apart and the requirement is exactly equal sizes.
- **Viewer steps** are taken from the pager's target page, so quick repeated key presses keep advancing.
- **Link tags.** Link items carry a `link-<id>` test tag, since a link's name and address are merged into one semantics node.

## Risks / Trade-offs

- [The 840 dp breakpoint decides which layout a tablet gets: a tablet in portrait (about 800 dp) gets the full-width layout, in landscape the column] → Matches Material's "expanded" window size; the threshold is one constant.
- [Keyboard focus can be lost by clicking elsewhere, which stops the arrow keys] → Clicks inside the viewer keep focus because the viewer is the focus target; the viewer re-requests focus when it opens.
- [Existing tests assert the previous layout (720 dp text, full-width grid and reader, left-aligned items)] → Updated in the same tasks, so the suite always describes the current rules.
- [Centering a one-per-row link list on phones looked ragged] → Tried on a device and reverted: phone link rows are start-aligned as before.
- [The browser may handle arrow keys itself (scrolling) when the canvas is focused] → The handler consumes the keys it acts on.

## Migration Plan

No data or API changes. Rollback is reverting the app builds.

## Open Questions

- The breakpoint (840 dp), the column share (50%) and the floor (560 dp) are constants that can be tuned without changing the structure.
