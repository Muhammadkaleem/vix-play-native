# History / Recently Played — PRD

**Route:** `/history` · **Priority:** P0
**Entry:** Library overflow, continue-watching "see all".

## Purpose
Show recently played media with resume positions; power the continue-watching rail.

## Layout
- Chronological list: thumbnail, title, last-played time, resume %.
- Filters: video/audio/all; clear-all.

## Capabilities
- Persist last position + timestamp per file (Room).
- Privacy: exclude private-folder items; incognito playback toggle (P2).

## States
Empty · Populated · Cleared.

## Edge cases
- Deleted source → keep entry greyed or purge (setting).
- Finished items (≥ ~95%) → mark watched, drop from continue-watching.

## Acceptance criteria
- [ ] Resume positions accurate and match continue-watching rail.
- [ ] Clear history removes entries and rail items.
- [ ] Private items never appear in history.
