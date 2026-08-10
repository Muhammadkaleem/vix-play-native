# Deferred-verification ledger — VixPlay (seeded 2026-08-07 from the grill-tracker backlog)

Copy to `<repo>/qa/ledgers/deferred-verification.md`. Status: open until verified with a
report reference, or accepted-risk with a name and a revisit-by. Reviewed at every T4;
every open item forced to a decision at T5.

| ID | What's unverified | Why | Escape plan | Status |
|---|---|---|---|---|
| DV-001 | Pinch-to-zoom feel + detector on real fingers | Emulator can't inject multi-touch (no adb root, SELinux blocks sendevent, input is single-pointer) | Physical-device pass: pinch in/out, zoom clamp 1–4× | open |
| DV-002 | Pan-while-zoomed (bounds clamp, 1:1 mapping, back-gesture-gutter collision) | Reaching the zoomed state needs a real pinch (DV-001) | Same physical pass as DV-001 | open |
| DV-003 | SMB browsing + playback end-to-end | No SMB server was available (FTP verified vs local pyftpdlib) | Dockerized samba on the host LAN, or a NAS; verify browse + play + credential persistence | open |
| DV-004 | Library Pulse: actual notification delivery, quiet-hours suppression, 3-day min-gap, Turn-off action, deep-link tap → player with home beneath | Triggers need multi-day idle/new-video conditions | Time-shift device clock where logic allows; else multi-day soak on hardware; tap-path testable by lowering gates via temporary instrumentation (revert!) | open |
| DV-005 | Play in-app review prompt | No-op unless installed from Play | Play internal-track build; complete 3 playbacks; observe prompt | open |
| DV-006 | API ≤ 28 legacy branches: rename via DATA path, legacy WRITE_EXTERNAL delete confirm, legacy copy | Needs an API 24–28 image; can't run on API 35/36 | Boot API 26 emulator from device-matrix; run STORAGE cases there | open |
| DV-007 | Cancel-mid-copy cleanup (no partial file left) + multi-file move batches | Not exercised in the file-ops device pass | Targeted T1 on a large fixture (multi-GB) so cancel lands mid-file | open |
| DV-008 | Private folder: biometric unlock; unhide + delete-from-vault paths | No enrolled fingerprint on emulator; paths unexercised | Physical device with fingerprint; short scripted pass | open |
| DV-009 | Chromecast playback (design locked, HELD) | No Chromecast on the test network | Hardware pass when a Cast target exists | open |
| DV-010 | Native ad fill on hardware | Native ads return internal error 0 on emulator (banners fine) | Real-device check with test ad units | open |
| DV-011 | Player top-bar title shows resolved display name (visual re-confirm) | Fix verified in dumpsys, not visually (controls auto-hide; test clip ambiguous) | 30-second check during next device run with a distinct-title fixture | open |
| DV-012 | Bluetooth media buttons + audio routing (EQ per-output switch on real BT) | Emulator has no BT audio | Physical device + BT headset: play/pause/next from headset; EQ profile swaps on route change | open |
