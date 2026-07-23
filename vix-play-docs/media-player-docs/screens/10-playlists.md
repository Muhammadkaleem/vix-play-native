# Playlists — PRD

**Route:** `/playlists`, `/playlists/{id}` · **Priority:** P1
**Entry:** Audio/Video library, add-to-playlist actions.

## Purpose
Create and manage ordered collections of audio/video for continuous play.

## Layout
- Playlist list (name, item count, art collage).
- Detail: reorderable items, play-all/shuffle, edit, remove.

## Capabilities
- Create/rename/delete; add from library, folders, search, now-playing.
- Reorder (drag), remove; smart playlists (recently added, most played) P2.
- Import/export M3U (P2).

## States
Empty · Populated · Editing · Missing files (flag items whose source is gone).

## Edge cases
- Source file moved/deleted → mark unavailable, offer relink/remove.
- Mixed audio+video playlist behavior (route each to correct player).

## Acceptance criteria
- [ ] Create, reorder, and play a playlist end-to-end.
- [ ] Missing items flagged, not silently skipped without notice.
