# Licensing & Legal (read before shipping)

This is what kills video-player projects commercially. Plan from day one.

## FFmpeg
- FFmpeg is **LGPL/GPL**. To keep the app closed-source, build the **LGPL** configuration and **dynamically link**; keep any patches separate and provide the required notices/source offer.
- Enabling GPL-only components makes the whole app GPL. Audit your build flags.

## Codec patents (separate from software license)
- **HEVC (H.265):** multiple patent pools (MPEG-LA/Access Advance/Velos) — per-unit royalties possible at scale. Budget or gate.
- **AV1:** royalty-free (AOMedia) — prefer where possible.
- **AC3 / E-AC3 (Dolby)** and **DTS:** require **paid commercial licenses**. MX Player removed DTS/AC3 decoding for a period over exactly this. Options: license it, or ship these decoders as a **user-initiated separate "custom codec" download** to shift responsibility (still consult a lawyer).
- **Dolby Vision / Atmos:** licensed + device-gated.

## Third-party libraries
- **libass** (subtitles): ISC — permissive, include notice.
- Cast SDK, SMBJ, OkHttp, etc.: include OSS attributions (Settings > About > Licenses).

## Play Store policy
- All-files access declaration (see permissions-storage.md).
- Don't facilitate access to infringing content; be careful with built-in "find streams" features.

## Action items
- [ ] Legal review of the codec set you ship by default.
- [ ] LGPL FFmpeg build + notices + source offer.
- [ ] Decide DTS/AC3 strategy (license vs optional download vs omit).
- [ ] OSS license screen present.
- [ ] Play Console all-files declaration prepared.

*This is engineering guidance, not legal advice — get a lawyer to review the codec/licensing plan before release.*
