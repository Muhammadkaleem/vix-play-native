# Screens Index & Navigation Map

Every screen has a PRD using the same template: **Purpose · Entry points · Layout · States · Interactions · Edge cases · Acceptance criteria.**

## Navigation map
```
Splash
  └─> (first run) Onboarding ─> Permissions ─> Home
  └─> (returning) Home

Home (bottom nav: Video · Audio · Network · Settings)
  ├─ Video Library ─┬─> Folder Browser ─> Video Player
  │                 ├─> Video Player (direct)
  │                 ├─> Search
  │                 └─> History
  ├─ Audio Library ─┬─> Audio Player ─> Equalizer
  │                 └─> Playlists
  ├─ Network ───────┬─> Stream by URL ─> Video Player
  │                 ├─> SMB/FTP Browser ─> Video Player
  │                 └─> Cast/DLNA picker
  └─ Settings ──────┬─> Gesture Remap
                    ├─> Theme & Skins
                    ├─> Subtitle defaults
                    ├─> Playback defaults
                    ├─> Private Folder (PIN)
                    └─> About / Pro

Video Player ─> Subtitle Manager (sheet), Cast picker (sheet), File Info (sheet), PiP
Audio Player ─> Equalizer, Sleep timer, Queue
```

## Screen list
| # | Screen | Priority | File |
|---|---|---|---|
| 00 | Splash / Onboarding / Permissions | P0 | `00-splash-onboarding.md` |
| 01 | Video Library (Home) | P0 | `01-video-library.md` |
| 02 | Folder Browser | P0 | `02-folder-browser.md` |
| 03 | Video Player | P0 | `03-video-player.md` |
| 04 | Audio Library | P0 | `04-audio-library.md` |
| 05 | Audio Player | P0 | `05-audio-player.md` |
| 06 | Equalizer | P1 | `06-equalizer.md` |
| 07 | Subtitle Manager | P0 | `07-subtitle-manager.md` |
| 08 | Network & Streaming | P1 | `08-network-streaming.md` |
| 09 | Cast / DLNA | P1 | `09-cast-dlna.md` |
| 10 | Playlists | P1 | `10-playlists.md` |
| 11 | History / Recent | P0 | `11-history.md` |
| 12 | Search | P0 | `12-search.md` |
| 13 | Private Folder | P1 | `13-private-folder.md` |
| 14 | Settings | P0 | `14-settings.md` |
| 15 | File Info | P1 | `15-file-info.md` |
