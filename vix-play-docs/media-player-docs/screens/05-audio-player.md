# Audio Player — PRD

**Route:** `/player/audio` · **Priority:** P0
**Entry:** Audio library tap, mini-player expand, notification, external audio intent.

## Purpose
Full-screen audio playback with queue, equalizer entry, background service, and lock-screen/notification controls.

## Layout
- Large album art, title/artist/album, seek bar, transport (prev/play/next), shuffle/repeat.
- Actions: equalizer, sleep timer, queue, favorite, share, speed, audio-track (for multi-track files).

## Capabilities
- Gapless playback (P1), crossfade (P2).
- Background playback via `MediaSessionService`; notification + lock-screen controls; Android Auto (P2).
- Speed + pitch; audio delay/sync for video-derived audio.

## States
Playing/Paused · Buffering (stream) · Ended (advance queue) · Background · Error.

## Interactions
- Swipe art for prev/next; drag seek; tap queue to reorder.
- Media buttons / Bluetooth controls.

## Edge cases
- Audio focus (calls, other apps, ducking).
- Bluetooth device switch mid-playback.
- Sleep timer at track end vs hard stop.

## Acceptance criteria
- [ ] Background playback continues with app swiped away (foreground service).
- [ ] Notification + lock-screen controls fully functional.
- [ ] Audio focus + Bluetooth handled per Android guidelines.
