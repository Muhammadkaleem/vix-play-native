# QA & Release

## Device matrix
- OS: Android 7 (min) → latest, focus 10/12/13/14+ for storage + PiP + Material You.
- SoC: low-end (software decode stress), mid, flagship; test HW/SW fallback per tier.
- Screens: phone (portrait/landscape), tablet, foldable, Android TV.
- Panels: SDR + HDR/OLED for AMOLED + HDR playback.
- Storage: internal, SD card, USB-OTG, SAF trees, SMB/NAS.

## Test areas
- **Codec/container matrix** — one sample per P0/P1 format; verify HW then forced SW.
- **Subtitles** — SRT/ASS/PGS render, sync, encoding override, online download.
- **Gestures** — every default + a full remap; tap↔drag handoff on low-end; precision seek; speed hold; lock.
- **Playback lifecycle** — resume accuracy, audio focus (call/headset/BT), background, PiP, rotation.
- **Streaming/cast** — HLS/DASH/SMB, Chromecast, network drop recovery.
- **Storage** — scoped-storage paths, removable mount/unmount, huge library perf.
- **Theming** — all themes, Material You, no hardcoded-color leaks, skin switch.
- **Billing** — purchase, restore, pending, refund downgrade.

## Automation
- Unit: `GestureDispatcher` resolution/conflict rules, repositories, config serialization/migrations.
- Instrumented: Room, scanning, playback smoke tests.
- E2E: Maestro flows (open → play → seek → subtitle → background → resume).
- Crash: Crashlytics; symbolicated native stacks for FFmpeg/libass.

## Performance budgets
- TTFF < 500ms local · scroll 60fps at 5k items · no ANR on unsupported files · memory stable across long playback.

## Release checklist
- [ ] Licensing items (engineering/licensing-legal.md) cleared.
- [ ] All-files declaration submitted if used.
- [ ] Crash-free > 99.5% in beta.
- [ ] OSS licenses screen present.
- [ ] Accessibility pass (TalkBack on controls, contrast, touch targets).
- [ ] Staged rollout (5% → 20% → 100%) with crash gate.
