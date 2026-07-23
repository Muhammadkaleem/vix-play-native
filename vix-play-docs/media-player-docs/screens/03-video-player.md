# Video Player — PRD

**Route:** `/player/video?uri=...` · **Priority:** P0
**Entry:** Any media tap, external intent (ACTION_VIEW), network stream, cast source selection.

## Purpose
The core surface. Frame-accurate playback of virtually any format, with the full gesture system, subtitle rendering, and a clean auto-hiding control layer.

## Layout
- Full-bleed video Surface + gesture layer + HUD overlay (see features/gestures.md, ui/PlayerHudOverlay).
- **Control bar (auto-hide):** play/pause, seek bar with buffered range + chapter ticks, time, speed, subtitle, audio-track, aspect/zoom, rotate-lock, PiP, cast, more.
- Top bar: title, back, more (file info, sleep timer, decoder toggle, equalizer, screenshot).
- Skins reflow this layout (Minimal/Classic/Cinema/Pro).

## Playback capabilities
- HW decode with automatic SW (FFmpeg) fallback per file.
- Multi audio-track + subtitle-track switching.
- Speed 0.25–4× with pitch correction; A-B repeat; frame-step (Pro skin).
- Zoom modes: fit / crop / stretch / 100%; pinch zoom + pan.
- HDR passthrough where supported.
- Resume position on open; save on pause/exit/background.

## States
- **Loading/buffering:** spinner + first-frame target < 500ms local.
- **Playing / Paused.**
- **Seeking (precision):** thumbnail preview + delta HUD.
- **Error (unsupported/corrupt):** actionable message (try SW decoder / report) — never a bare crash.
- **Ended:** autoplay next (if in a queue) or show replay/next.
- **Locked:** gesture-lock overlay; only unlock gesture active.
- **Background / PiP.**

## Interactions
- All gestures per user's remap config (default set in features/gestures.md).
- Single tap toggles controls; controls auto-hide after ~3s.
- Rotation follows sensor unless locked.

## Edge cases
- Codec unsupported by HW → auto-switch to SW; if still unsupported, clear error + track/codec info.
- Audio present, no video (or vice-versa) → play what's available; offer audio-only mode.
- Headset unplug → pause (respect audio focus).
- Incoming call / audio focus loss → pause/duck; resume after.
- Very high bitrate / 4K on weak device → allow decoder + resolution downshift hint.
- External subtitle auto-detect (same basename in folder).

## Acceptance criteria
- [ ] Plays the full P0/P1 codec/container matrix (features/playback-engine.md).
- [ ] Time-to-first-frame < 500ms for local H.264/HEVC.
- [ ] Resume within ±1s of last position.
- [ ] Audio focus handled correctly (pause on call/headset unplug).
- [ ] No ANR/crash on unsupported files — always a graceful error.
