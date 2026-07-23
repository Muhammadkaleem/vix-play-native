# File Info / Properties — PRD

**Route:** player/library sheet `/fileinfo` · **Priority:** P1
**Entry:** Long-press > properties; player overflow > info.

## Purpose
Show technical + file metadata to power users.

## Layout (sheet)
- File: name, path, size, date modified.
- Media: duration, container, resolution, frame rate, bitrate.
- Video track(s): codec, profile, HDR type, color space.
- Audio track(s): codec, channels, sample rate, bitrate, language.
- Subtitle track(s): format, language.

## Capabilities
- Pull from Media3 `Format` / `Tracks` + `MediaMetadataRetriever`.
- Copy path; open-with; share.

## States
Loaded · Partial (some fields unknown) · Error reading.

## Acceptance criteria
- [ ] Accurate track/codec/HDR info for the P0/P1 matrix.
- [ ] Never blocks UI while reading metadata.
