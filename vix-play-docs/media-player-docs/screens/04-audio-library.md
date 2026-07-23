# Audio Library — PRD

**Route:** `/library/audio` · **Priority:** P0
**Entry:** Audio tab.

## Purpose
Browse local audio by Tracks / Albums / Artists / Folders / Genres, with playlists and recently played.

## Layout
- Tabs: Tracks · Albums · Artists · Folders · Playlists.
- Rows: album art, title, artist/album, duration.
- Persistent **mini-player** at bottom when audio is active (art, title, play/pause, next).

## States
Loading (shimmer) · Empty (grant/choose folder) · Populated · Scanning.

## Interactions
- Tap track → Audio Player (adds context queue).
- Long-press → multi-select (queue, playlist, delete, share, properties).
- Sort/filter per tab; search.
- Swipe mini-player up → full Audio Player.

## Edge cases
- Missing/embedded art → fallback art; extract embedded where present.
- Compilation albums / various artists grouping.
- Same track in multiple folders.

## Acceptance criteria
- [ ] All five groupings populate correctly from a scanned library.
- [ ] Mini-player reflects live playback state across tabs.
