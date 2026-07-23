# Feature Matrix

Full feature inventory with priority (P0=MVP, P1=fast-follow, P2=differentiator).

## Playback core
| Feature | Priority | Notes |
|---|---|---|
| Media3/ExoPlayer local playback | P0 | Base engine |
| FFmpeg software decode fallback | P0 | Auto HW→SW per file; MX's core advantage |
| Hardware decoding | P0 | Default; toggle in settings |
| Format containers: MP4, MKV, WebM, TS | P0 | |
| Containers: AVI, FLV, MOV, WMV, 3GP, OGV, M2TS | P1 | Via FFmpeg |
| H.264, HEVC, VP9 | P0 | |
| AV1, MPEG-4, DivX/Xvid | P1 | |
| HDR10 / HDR10+ | P1 | Device-dependent |
| Dolby Vision | P2 | Licensing + device gated |
| Audio: AAC, MP3, FLAC, Opus, Vorbis, PCM, ALAC | P0 | |
| Audio: AC3/EAC3, DTS | P1 | **Requires paid license — see licensing doc** |
| Multiple audio tracks + switching | P0 | |
| Playback speed 0.25–4× + pitch correction | P0 | |
| A-B repeat | P1 | |
| Frame-step | P2 | |
| Resume playback position | P0 | |
| Zoom modes (fit/crop/stretch/100%) | P0 | |

## Subtitles
| Feature | Priority | Notes |
|---|---|---|
| SRT, VTT | P0 | |
| ASS/SSA styled (libass) | P0 | |
| PGS / VobSub (image) | P1 | |
| External subtitle load | P0 | |
| Online subtitle search (OpenSubtitles) | P1 | |
| Timing offset / sync | P0 | |
| Style presets + custom styling | P1 | |
| Secondary subtitle track | P2 | |
| AI auto-generated subtitles (Whisper) | P2 | On-device |

## Gestures & controls
| Feature | Priority | Notes |
|---|---|---|
| Brightness / volume vertical swipe | P0 | |
| Horizontal seek | P0 | |
| Precision seek (vertical granularity) | P1 | Differentiator |
| Double-tap seek, pinch zoom | P0 | |
| Variable-speed hold | P1 | Differentiator |
| Fully remappable zones | P1 | Differentiator |
| Multi-finger gestures | P2 | |
| Haptic feedback | P1 | |
| Gesture (child) lock | P0 | |

## Theming
| Feature | Priority | Notes |
|---|---|---|
| Light / Dark | P0 | |
| AMOLED pure black | P1 | |
| Material You dynamic color | P1 | Differentiator |
| Player skins (Minimal/Classic/Cinema/Pro) | P2 | |
| Ambient backglow | P2 | Differentiator |
| Custom accent | P1 | |
| Adaptive fullscreen dim | P2 | |

## Library & files
| Feature | Priority | Notes |
|---|---|---|
| Folder browser | P0 | |
| Media library (auto-scan) | P0 | |
| Thumbnails + generation | P0 | |
| Sort / filter / search | P0 | |
| Play history / recently played | P0 | |
| Playlists | P1 | |
| Hidden folders (.nomedia) | P1 | |
| Private/locked folder | P1 | |
| Cloud (Drive/Dropbox) | P2 | |

## Streaming & casting
| Feature | Priority | Notes |
|---|---|---|
| Network stream by URL (HLS/DASH/RTSP) | P1 | |
| SMB / network shares | P1 | |
| FTP | P2 | |
| Chromecast | P1 | |
| DLNA / UPnP | P2 | |

## System integration
| Feature | Priority | Notes |
|---|---|---|
| Picture-in-Picture | P1 | |
| Background audio + media notification | P0 | |
| Lock-screen / media-session controls | P0 | |
| Sleep timer | P1 | |
| Screenshot / GIF capture | P2 | |
| Video trim | P2 | |
| Android TV (Leanback) | P1 | |
| Equalizer + bass boost | P1 | |

## Monetization
| Feature | Priority | Notes |
|---|---|---|
| Free tier with calm ads | P0 | |
| Pro one-time unlock (ad-free + extras) | P0 | |
| Cloud/sync subscription | P2 | |
