# Tasks

## 1. Long text preview

- [x] 1.1 Add a `LongText` composable (10 rows, ellipsis, clickable only while truncated, driven by the real text layout) and use it for the profile description and the project summary in both the grid and reader views; verify UI tests that a text of more than 10 rows shows exactly 10 rows with an ellipsis, a short text shows in full and is not clickable, resizing the window across the 10-row boundary flips truncated and untruncated, and the project summary behaves the same in the grid and the reader
- [x] 1.2 Add the bottom sheet opened by tapping truncated text, with the full text in a scrollable area, a Material close (X) icon button and a Material copy icon button; choose the clipboard API that compiles and works on jvm, android, iOS and wasm, behind one function; verify UI tests that tapping opens the sheet with the full text (including the part past row 10), the X, a tap outside and the back key each close it leaving the page unchanged, and a very long text scrolls inside the sheet
- [x] 1.3 Make the copy button put exactly the full text on the clipboard and show a brief "Copied" confirmation that goes away by itself; verify UI tests with a fake clipboard for the exact text copied (including line breaks and non-ASCII characters), the confirmation appearing and disappearing, copying twice working, and the sheet staying open after copying

## 2. Collapsible project rows

- [x] 2.1 Add the shared collapse rule (first 5 rows) and an `ExpandedSections` holder saved with `rememberSaveable` in `ClientNavigation` and passed to `HomeContent` (with a local default); verify unit tests for the rule at 0, 1, 5, 6 and 20 rows and that the holder survives a save and restore
- [x] 2.2 Collapse the project card rows after 5 with a centered expand icon button ("Show more projects") and a "Show fewer projects" icon button when expanded, nothing when there are 5 rows or fewer; verify UI tests for 5 rows (no icon), 6 rows (icon, 5 rows shown, in owner order), expanding shows every row, collapsing returns to 5, resizing so the column count changes updates the rows and the icon while cards stay equal in size, and an expanded list staying expanded when a project is opened and closed

## 3. Collapsible social links

- [x] 3.1 On screens under 840 dp collapse the one-per-row links after 5 with a centered expand icon button ("Show more socials") and "Show fewer socials" when expanded; verify UI tests for 5 links (no icon), 6 links (icon, 5 shown), expanding, collapsing, and the state surviving opening and closing a project
- [x] 3.2 On wide screens collapse the wrapping row after 5 lines using the flow's overflow indicators (expand icon at the end of the fifth line, collapse icon only with at least 6 lines), falling back to a custom flow layout if the API does not behave; verify UI tests that 5 lines or fewer show no icon, more than 5 lines show the icon and exactly 5 lines, expanding shows every link, collapsing returns to 5 lines, resizing across the point where the line count passes 5 adds or removes the icon, and the lines stay centered
- [x] 3.3 Verify rendered screenshots of the profile collapsed and expanded, with long text and a sheet open, at phone and wide widths in light and dark

## 4. Integration

- [x] 4.1 Run `./kotlin build` for all targets and `./kotlin test`, and update the README; verify everything passes
- [x] 4.2 Check on a phone, a tablet or wide window, and the web: a long description and summary open the sheet and copy, 6 or more links and projects collapse and expand, and the state survives a visit to a project; verify nothing regresses

## 5. Adjustments after review

- [x] 5.1 Make the profile description and the project summary always tappable, opening the bottom sheet whether or not they are truncated (the ellipsis still shows only when text is cut); verify UI tests that text of 1, 5, 10 and 11+ rows all open the sheet with the full text, the close and copy icons work from short text too, and the ellipsis rules from 1.1 are unchanged; update the earlier tests that expected short text not to be tappable
- [x] 5.2 Collapse project card rows after 2 rows instead of 5 (the social links keep 5) by giving the two lists their own row limits; verify unit tests for each limit at 0, 1, 2, 3 and 20 rows, and UI tests that 2 rows show no icon, 3 rows show the first 2 rows and the icon, expanding and collapsing work, resizing recomputes the rows, the expanded state survives opening a project, and the socials still collapse at 5; update the tests that assumed 5 project rows
- [x] 5.3 Re-run the build and the whole test suite and refresh the README; verify everything passes
