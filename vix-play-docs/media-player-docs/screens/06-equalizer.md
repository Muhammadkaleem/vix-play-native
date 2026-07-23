# Equalizer — PRD

**Route:** `/equalizer` · **Priority:** P1
**Entry:** Audio/Video player > equalizer.

## Purpose
Per-output audio shaping: multiband EQ, bass boost, virtualizer, presets, preamp.

## Layout
- Enable toggle · preset dropdown (Flat, Rock, Pop, Jazz, Classical, Custom...) · N-band vertical sliders · Bass boost + Virtualizer knobs · Preamp.

## Capabilities
- Android `Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer` effects bound to the audio session.
- Save custom presets; apply globally or per-output (speaker/headset/BT).

## States
On/Off · Preset selected · Custom (unsaved) · Unsupported device (graceful disable).

## Edge cases
- Some devices/ROMs restrict audio effects → detect and disable with message.
- Effect must rebind when audio session id changes (track change).

## Acceptance criteria
- [ ] EQ audibly affects output; persists across tracks and app restarts.
- [ ] Gracefully disabled on unsupported hardware.
