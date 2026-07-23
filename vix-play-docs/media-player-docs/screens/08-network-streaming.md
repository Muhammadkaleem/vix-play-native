# Network & Streaming — PRD

**Route:** `/network` · **Priority:** P1
**Entry:** Network tab.

## Purpose
Play remote media: direct URLs, adaptive streams, and network shares.

## Layout
- **Stream by URL:** input + recent URLs; protocol auto-detect.
- **Network shares:** SMB/FTP server list; add server (host, credentials, path); browse like folders.
- Cast/DLNA entry.

## Capabilities
- Protocols: HTTP(S) progressive, HLS, DASH, RTSP (P1); RTMP, FTP (P2); SMB via SMBJ.
- Credential storage (encrypted); guest access.
- Adaptive bitrate for HLS/DASH.

## States
Idle · Connecting · Auth required · Auth failed · Browsing · Buffering · Unreachable/timeout.

## Edge cases
- Self-signed certs → explicit user opt-in.
- Auth expiry / wrong credentials → clear recoverable error.
- Slow network → buffering UX + quality/decoder hints; resumable where server supports range.
- SMBv1 legacy NAS → attempt negotiation, warn on insecure.

## Acceptance criteria
- [ ] Plays an HLS and a DASH stream from URL.
- [ ] Browses and plays from an SMB share with stored credentials.
- [ ] Network failures produce recoverable errors, never crashes.
