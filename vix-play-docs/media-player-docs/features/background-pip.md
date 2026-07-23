# Background Playback & Picture-in-Picture

## Background audio
- `MediaSessionService` (Media3) hosts the player; foreground service type `mediaPlayback`.
- Rich media notification + lock-screen controls + Bluetooth/media buttons.
- Audio focus handling: pause on transient loss/call, duck where appropriate, resume after.

## Picture-in-Picture (P1)
- Enter PiP on gesture/home-press during video; `PictureInPictureParams` with play/pause/seek actions.
- Handle aspect ratio, config changes, and resize.
- Restore full player on tap.

## Edge cases
- Android 12+ PiP auto-enter setting.
- Background audio for video (audio-only continue) toggle.
- Notification dismissal → stop service correctly.
- Battery optimization prompts for reliable background playback.

## Acceptance criteria
- [ ] Audio continues with app swiped away; notification controls work.
- [ ] PiP enters/exits cleanly with working controls.
- [ ] Audio focus behavior matches Android guidelines.
