# Folder Browser — PRD

**Route:** `/library/folders/{path}` · **Priority:** P0
**Entry:** Video Library "Folders" view, or Settings > storage locations.

## Purpose
Let users navigate their actual filesystem/tree (including SD card, USB OTG, SAF trees) to find and play media the way MX users expect.

## Layout
- Breadcrumb / up button.
- List: folder rows (name, item count) + media rows (thumbnail, name, duration, resume).
- Storage picker header (Internal, SD card, USB, SAF locations, Network shortcut).

## States
- **Loading:** shimmer rows.
- **Empty folder:** "No media here."
- **No access to location:** grant/SAF CTA.
- **Populated.**

## Interactions
- Tap folder → descend. Tap media → play.
- Long-press → multi-select (move, copy, delete, rename, share, add to playlist, hide via .nomedia).
- Sort + view options mirror Video Library.
- "Add as shortcut" to pin a folder to home.

## Edge cases
- Deep nesting / permission boundaries with SAF (`DocumentFile`) — slower than direct File; cache listings.
- Removable storage unmounted mid-browse → graceful "storage removed" state.
- Symlinks / recursive folders → guard against infinite descent.
- Mixed media + non-media files → optionally show/hide non-media.

## Acceptance criteria
- [ ] Works across internal, SD, USB-OTG, and SAF trees.
- [ ] Listing a 1k-file folder renders < 500ms (cached) / < 1.5s (cold SAF).
- [ ] Rename/move/delete reflect immediately and survive rescans.
