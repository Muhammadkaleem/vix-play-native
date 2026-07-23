# Storage, Scanning & Thumbnails

## Storage access
See engineering/permissions-storage.md for the scoped-storage decision (SAF vs `MANAGE_EXTERNAL_STORAGE`). Support internal, SD card, USB-OTG, and SAF trees.

## Media scanning
- WorkManager job scans included folders; incremental (only changed) via `last_modified` + size.
- Build Room index: media items, folders, tracks, metadata.
- FTS index for search.
- Live updates to UI via Flow; background rescans with subtle progress.

## Thumbnails
- Grid thumbnails via Coil with a video frame fetcher.
- Scrub sprite-sheets via `thumbnail/ThumbnailProvider.kt` (local files only; skip for streams).
- Disk cache keyed by file+duration; memory LRU.

## Edge cases
- Removable storage mount/unmount events.
- Huge libraries (10k+): paginate, defer thumbnailing, avoid main-thread work.
- `.nomedia` respected; hidden-folder toggle.
- Permission revoked mid-session.

## Acceptance criteria
- [ ] Incremental scan doesn't reprocess unchanged files.
- [ ] 10k-item library scans + browses without jank.
- [ ] Thumbnails cached and reused across sessions.
