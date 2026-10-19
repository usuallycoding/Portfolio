# Tasks

## 1. Content column and centered profile header

- [x] 1.1 Add `contentColumnWidth(screenWidth)` (full width below 840 dp, otherwise half the screen but at least 560 dp and never more than the screen) as a pure function; verify unit tests at 320, 411, 839, 840, 1000, 1119, 1120, 1600 and 3000 dp
- [x] 1.2 Center the avatar on the cover's bottom edge, and center the name and the description (centered text, within the column width); keep the cover full width with the 240 dp cap and the theme button at its top end; verify UI tests at 411 and 1600 px that the avatar's center is the window center and straddles the cover's bottom edge, the name and description are centered, and the description is at most the column width

## 2. Socials

- [x] 2.1 Add a centered "Socials" title shown only when there are public links; verify UI tests that it is shown and centered with links, and absent (with no links) without them
- [x] 2.2 On wide screens lay the links out as a centered `FlowRow` in the column, each link at its natural width (icon, name, address with a single-line ellipsis at a maximum item width); verify UI tests that many links wrap onto further centered lines, a few links share one centered line, every link is inside the column, a very long address does not take a whole line, and tapping each link opens its own address
- [x] 2.3 On screens under 840 dp keep one link per row, each row's icon and text centered as a group; verify UI tests at 411 px for one row per link in owner order, and a screenshot check

## 3. Projects

- [x] 3.1 Center the "Projects" title and lay the project rows inside the content column (card count from the column width, rows centered); verify UI tests that on a wide window the cards are inside the column, centered, still equal in size, still in owner order, and that on a phone the cards use the full width
- [x] 3.2 Update the layout tests that assumed the previous rules (720 dp text and link width, full-width project cards, left-aligned items) to the new rules, and verify rendered screenshots of the profile at phone and wide widths in light and dark

## 4. Project page

- [x] 4.1 Switch to a centered top bar title (`CenterAlignedTopAppBar`, back control at the start) and center the date, summary and Grid/Reader switch within the column with centered text; verify UI tests that the title is centered with and without a back control, and the header is centered and at most the column width on a wide window
- [x] 4.2 Put the grid in the centered column while keeping its scroll container full width (horizontal content padding), and keep the reader's images at the full screen width with the reader's header (date, summary, switch) centered in the column; verify UI tests that grid cells are inside the column and centered, that in the reader every image spans the whole window width (edge to edge, proportional heights, no gaps) at 411, 1000 and 1600 px, that the reader's header is in the centered column, and that scrolling works with the pointer beside the column in both views
- [x] 4.3 Update the tests that assumed a full-width grid and left-aligned header, and verify screenshots of the project page at phone and wide widths in both views and both themes

## 5. Image viewer

- [x] 5.1 Replace the word "Close" with a Material close `IconButton` labelled "Close" (white on the black viewer) and keep the position counter; verify a UI test that it is an icon labelled "Close" at the top end and closes the viewer
- [x] 5.2 Add left and right arrow key handling that animates to the previous and next image, stops at the ends, consumes only the keys it acts on, and focuses the viewer when it opens; verify UI tests with key events: right goes forward and the counter updates, left goes back, left on the first and right on the last image do nothing, the keys work right after opening without a click, and swiping still works

## 6. Integration

- [x] 6.1 Run `./kotlin build` for all targets and `./kotlin test`, and update the README for the centered layout and the viewer keys; verify everything passes
- [x] 6.2 Check on a phone, a tablet or wide window, and the web in both themes: the centered profile, the Socials row and wrapping, the projects column, the centered project page, the grid column, the full-width reader images, and the viewer's arrow keys and close icon; verify nothing regresses

## 7. Adjustments after device feedback

- [x] 7.1 Keep the profile description and the project's date and summary as centered blocks but with left-aligned text; verify UI tests that their text alignment is start (not center), that the blocks are still centered and within the column, and update the earlier tests that asserted centered text
- [x] 7.2 Return the phone-width link rows to start-aligned icon and text inside full-width rows; verify UI tests at 411 px that each row's icon starts at the left gutter and its text follows the icon, and that rows are still one per link in owner order and full width
- [x] 7.3 Keep the project title exactly centered whatever the back control's width by giving the top bar a same-size slot on both sides; verify UI tests that long and short titles are centered at 360, 411 and 600 px with and without a back control, the same space is kept free on both sides, and a long title is cut with an ellipsis and never runs under the back control
