# Playback Engine

## Goal
Play virtually any file — MX Player's core promise — via Media3 (ExoPlayer) with an FFmpeg software-decode path and automatic HW→SW fallback.

## Architecture
- Base: `ExoPlayer` with `DefaultRenderersFactory` set to `EXTENSION_RENDERER_MODE_PREFER` so the **FFmpeg extension** wins when the platform decoder can't handle a codec.
- Build the FFmpeg extension with the full set of enabled decoders you're licensed for (see engineering/licensing-legal.md).
- Custom `RenderersFactory` to expose a **decoder toggle** (Auto / HW / SW) surfaced in player + settings.

## Codec / container matrix
| Layer | P0 | P1 |
|---|---|---|
| Containers | MP4, MKV, WebM, TS | AVI, FLV, MOV, WMV, 3GP, OGV, M2TS |
| Video | H.264, HEVC, VP9 | AV1, MPEG-4, DivX/Xvid |
| Audio | AAC, MP3, FLAC, Opus, Vorbis, PCM, ALAC | AC3/EAC3, DTS (licensed) |
| HDR | — | HDR10, HDR10+ (device-gated) |

## HW ↔ SW fallback
1. Attempt hardware decoder.
2. On `DecoderInitializationException` / decode error → transparently retry with FFmpeg SW renderer.
3. Persist last-good decoder per file to skip the retry next time.
4. Manual override always wins.

## Resume & positions
- Save position on pause/stop/background and periodically (every ~5s) to MMKV.
- Restore on open within ±1s; mark ≥95% as watched.

## Edge cases
- Variable frame rate files, B-frames, broken indexes (seek by re-scan).
- 10-bit / HDR on SDR panels → tone-mapping or clear limitation notice.
- Very high bitrate on weak SoC → suggest SW off / resolution note.
- Audio-only or video-only streams.

## Acceptance criteria
- [ ] Full P0 + P1 matrix plays.
- [ ] Automatic SW fallback works without user action.
- [ ] Decoder override persists per file.
