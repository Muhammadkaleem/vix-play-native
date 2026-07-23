# Gestures

## Capability
Fully remappable gesture system: any action to any zone/gesture, precision seek, variable-speed hold, multi-finger, haptics, gesture lock.

## Shipped implementation
This feature is already scaffolded in code:
- `gesture/GestureModels.kt` — zones, gestures, actions, bindings, defaults, `zonesFor()`.
- `gesture/GestureDispatcher.kt` — resolution (specificity), conflict rules, `assign()` + undo.
- `gesture/PlayerCommandSink.kt` — intent boundary + HUD state.
- `gesture/PlayerGestureModifier.kt` — pointer detection (axis lock, precision seek, speed hold, multi-finger).
- `playback/PlayerController.kt` — applies intents to ExoPlayer/AudioManager/window + haptics.
- `thumbnail/ThumbnailProvider.kt` — sprite-sheet scrub previews.
- `ui/PlayerHudOverlay.kt` — brightness/volume/seek HUD + thumbnail.
- `settings/GestureRemapScreen.kt` — visual grid remap editor.

## Default mapping
See the gesture spec: left-swipe brightness, right-swipe volume, horizontal precision seek, center double-tap play/pause, side double-tap ±10s, pinch zoom, long-press-drag speed hold, two-finger next/prev, two-finger double-tap PiP, three-finger screenshot.

## Precision seek
Finger's vertical position sets granularity (coarse/fine/frame); thumbnail preview + delta HUD; commit on release.

## Edge cases
- Tap↔drag handoff (verify no double-fire on low-end devices).
- Edge exclusion for system back-gesture.
- Conflict resolution: one action per (zone, gesture); no two continuous drags on same zone+axis.

## Acceptance criteria
- [ ] Remapping persists and takes effect immediately.
- [ ] Precision seek + speed hold feel smooth at 60fps.
- [ ] Gesture lock disables all but unlock.
