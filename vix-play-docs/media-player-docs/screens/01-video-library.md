# Video Library (Home) — PRD

**Route:** `/library/video` · **Priority:** P0
**Entry:** Default tab after launch.

## Purpose
The player-first home. Surface the user's videos immediately — continue-watching first, then folders/all — with zero content-portal clutter.

## Layout
- Top app bar: title, search icon, overflow (sort, view mode, refresh, settings).
- **Continue Watching** rail (horizontal, resume %) — hidden if empty.
- Toggle: **Folders** view vs **All videos** grid/list.
- Grid cards: thumbnail, duration badge, title, resume progress bar, new/unwatched dot.
- Bottom navigation: Video · Audio · Network · Settings.

## States
- **Loading:** skeleton cards while first scan runs.
- **Empty (no media):** friendly illustration + "Choose folder" (SAF) / "Grant access".
- **Empty (permission):** grant-access CTA.
- **Populated:** rails + grid.
- **Scanning in background:** subtle top progress line; content updates live.

## Interactions
- Tap card → Video Player (resumes if position saved).
- Long-press → multi-select (share, delete, add to playlist, rename, properties, hide).
- Pull to refresh → rescan.
- Sort: name, date added, date modified, duration, size, most recent. Persist choice.
- View: grid / list / compact.

## Edge cases
- Corrupt/zero-byte file → show with warning badge; tapping surfaces a clear error, not a crash.
- File deleted outside app → remove on next scan; handle stale thumbnail.
- Very large library (10k+ items) → paged/lazy loading, no jank (FlashList-equivalent lazy list).
- Duplicate filenames across folders → disambiguate by parent folder.

## Acceptance criteria
- [ ] First meaningful content within 1s of a warm scan.
- [ ] Continue-watching reflects accurate resume positions.
- [ ] Multi-select operations are undoable where destructive (delete → snackbar undo when possible).
- [ ] Scrolling a 5k-item library stays at 60fps.
