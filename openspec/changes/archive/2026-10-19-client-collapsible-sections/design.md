# Design

## Context

See proposal.md for motivation. Current client code (`app/clientShared`):

- `HomeContent` is a `LazyColumn` of centered items. The description is a `Text` in a column item; on screens under 840 dp the links are one `LinkRow` item each, and on wide screens one `FlowRow` item; projects are lazy rows (`projectRows`, chunked by the column count) of equal-width cards.
- `ProjectHeader` (project page) shows the date and the summary, in both the grid and the reader views.
- `ClientNavigation` (in `ClientApp.kt`) switches between the profile and a project and already keeps navigation state (`projectId`, `galleryMode`) in `rememberSaveable`, outside the screens, so it survives opening a project and coming back. `HomeContent` itself leaves the composition while a project is open.
- Material3 1.9 and the Material icons library are already dependencies. Nothing here touches the clipboard yet.

## Goals / Non-Goals

**Goals:**
- A text preview that follows the real layout: whether text is truncated depends on the width, not on a character count.
- One shared mechanism for "first 5 rows, then expand" for both link rows and project rows, with state that survives a trip to a project page.
- A copy action that works on every target.

**Non-Goals:**
- Persisting the expanded state across app launches, or remembering the bottom sheet.
- Truncating the profile name or project titles.
- Changing the equal-size cards, the column width or the centered layout.

## Decisions

### Long text: measure, don't guess
A `LongText` composable wraps `Text` with `maxLines = 10`, `overflow = TextOverflow.Ellipsis` and `onTextLayout { truncated = it.hasVisualOverflow }`. The text is always clickable (and exposed as a button) so the full text is one tap away even when it all fits; `truncated` only decides whether an ellipsis is shown. Because it is driven by the layout result, resizing a window or changing the font size updates it immediately. It is used for the profile description and in `ProjectHeader`, so the project summary behaves the same in the grid and the reader.

Alternative considered: estimating rows from the character count. Rejected because rows depend on width, font and language.

### Bottom sheet
Tapping truncated text shows a Material3 `ModalBottomSheet` (partial expansion skipped so a long text gets the room it needs, capped by the sheet's own maximum width on wide screens). Its content is a header row with a copy icon button and a close (X) icon button at the top end, then the full text in a vertically scrollable area. It closes through the X, a tap outside, or the system back or Escape, via `onDismissRequest`.

Copy puts the full text on the clipboard and shows a brief confirmation: the copy icon switches to a check mark with the label "Copied" for about two seconds. The clipboard call is behind one small function so the API that compiles on all five targets (`LocalClipboard` with a plain-text entry, or the older `LocalClipboardManager`) can be chosen in the first task without touching the callers; tests substitute a fake clipboard.

### One collapse rule
Two constants, `CollapsedSocialRows = 5` and `CollapsedProjectRows = 2`, because projects are tall cards and collapse sooner than the compact links. Rows are counted the way they are laid out:
- Projects: the existing `projectRows` list. When `rows.size > 2` and collapsed, only `rows.take(2)` are emitted, followed by one centered icon item; expanded, all rows are emitted followed by the "show fewer" icon. Because rows come from the column count, a resize that changes the columns changes the row count and so the icon.
- Socials on narrow screens: each link is a row, so the same take(5) rule applies (with the social limit), with the icon as a centered item below.
- Socials on wide screens: rows are wrapped lines, which only layout knows. The wrapping row uses `FlowRow` with `maxLines = 5` while collapsed and `FlowRowOverflow.expandOrCollapseIndicator`, whose expand indicator is composed only when items did not fit and whose collapse indicator is composed only when expanded and there are at least 6 lines (`minRowsToShowCollapse = 6`). The indicators are small icon buttons placed by the flow itself, at the end of the fifth line (collapsed) or the last line (expanded). If this API does not behave on every target, the fallback is a custom flow layout that reports its line count.

The icons are the standard Material expand more and expand less icons as `IconButton`s labelled "Show more socials", "Show fewer socials", "Show more projects" and "Show fewer projects".

### State that survives the profile being left
`ClientNavigation` owns one small holder, `ExpandedSections` (two booleans), saved with `rememberSaveable`, and passes it to `HomeContent`. Since `HomeContent` leaves the composition while a project is open, state kept inside it would reset; state held in `ClientNavigation` survives opening a project and returning, and starts collapsed when the app is opened again. `HomeContent` takes the holder as a parameter with a locally remembered default, so it stays usable on its own in tests and previews.

### Adjustments made during implementation
- **Ellipsis when the cut falls at a line break.** Compose draws "…" only when the cut falls inside a line; `isLineEllipsized` is unreliable (always false on desktop). When the character at the cut is a newline, the component draws the "…" itself after the tenth row, without semantics of its own.
- **Clipboard resolved by the page.** The sheet is a separate window with its own platform locals, so the copy function is created in the page composition and passed to the sheet.
- **Sheet content split out** (`FullTextSheetContent`) so Escape and the buttons can be tested; the test harness sends keys only to the main window, not to a modal sheet's own window.
- **Wide socials are placed, not just composed.** The flow row composes the links and indicators it hides; checks must look at what is placed on screen.
- **Equal card widths in whole pixels.** Card width is computed in integer pixels (rounded down) so neighbouring cards are identical; at most `columns - 1` pixels stay unused at a row's right edge.

## Risks / Trade-offs

- [The `FlowRow` overflow API is experimental and may behave differently on some targets] → The first wide-screen task checks it on the JVM tests and builds all targets; a custom flow layout is the planned fallback.
- [On wide screens the expand icon takes the place of one link in the fifth line] → Accepted: it is the flow's own overflow behavior and keeps the icon next to the links it controls.
- [`hasVisualOverflow` is only known after layout, so one frame may show an untruncated tap state] → Harmless: the text is simply not clickable until the first layout result arrives.
- [A bottom sheet on a wide window is narrow and centered] → That is Material's default and keeps long text readable.
- [Clipboard APIs differ across Compose targets and some are deprecated] → Isolated behind one function and chosen by what compiles and works on all five targets.

## Migration Plan

No data or API changes. Rollback is reverting the app builds.

## Open Questions

- The confirmation's wording and duration ("Copied", about two seconds) can be tuned without changing the structure.
- Whether a "+N more" count should appear next to the expand icon; not required by the specs.
