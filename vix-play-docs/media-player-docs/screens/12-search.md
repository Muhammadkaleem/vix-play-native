# Search — PRD

**Route:** `/search` · **Priority:** P0
**Entry:** Search icon (library screens).

## Purpose
Fast local search across media titles, folders, artists/albums.

## Layout
- Search field + debounced results grouped by type (Videos, Audio, Folders, Playlists).
- Recent searches; voice input (P2).

## States
Idle (recents) · Typing (debounced) · Results · No results · Searching large library (progressive).

## Interactions
- Tap result → play or open. Long-press → same actions as library.

## Edge cases
- Diacritics/case-insensitive matching; partial + fuzzy (P2).
- Very large library → indexed search (Room FTS) to stay responsive.

## Acceptance criteria
- [ ] Results appear < 200ms after typing stops on a 5k-item library.
- [ ] Handles accents/case correctly.
