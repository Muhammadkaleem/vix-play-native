# Cast / DLNA — PRD

**Route:** player/network sheet `/cast` · **Priority:** P1
**Entry:** Cast icon in player, or Network tab.

## Purpose
Send playback to Chromecast and DLNA/UPnP renderers.

## Layout
- Device picker (Chromecast + DLNA discovered devices).
- Cast controls: play/pause, seek, volume, subtitle track, disconnect.

## Capabilities
- Chromecast via Cast SDK (Default Media Receiver or custom).
- DLNA/UPnP renderer discovery + control (P2).
- Local file serving to renderer via embedded HTTP server for DLNA.

## States
Discovering · Connected (mirrored controls) · Casting · Disconnected · Incompatible media (transcode note).

## Edge cases
- Codec/container not supported by receiver → inform, offer local playback.
- Subtitle passthrough limits on Chromecast (side-loaded VTT only).
- Network drop mid-cast → attempt reconnect, then fall back to local.

## Acceptance criteria
- [ ] Discovers and casts a supported file to Chromecast with working transport + subtitle.
- [ ] Graceful messaging for unsupported media.
