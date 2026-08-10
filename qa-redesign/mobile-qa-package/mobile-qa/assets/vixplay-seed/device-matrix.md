# Device matrix — VixPlay (minSdk 24 · targetSdk 36)

Copy to `<repo>/qa/device-matrix.md`. Images chosen at the codebase's real `SDK_INT`
fault lines — each level below is where a behavior branch actually changes in this app.

| Target | Why this level | Duties |
|---|---|---|
| **API 36 emulator (primary)** | Current target; where all new work runs | T0 daily, T1, T3 full suite, T4 primary |
| API 34 emulator | Partial media access (`READ_MEDIA_VISUAL_USER_SELECTED`), fg-service types | Partial-grant scenarios, Pulse permission paths, T4 smoke |
| API 33 emulator | Granular media permissions split (`READ_MEDIA_VIDEO/AUDIO`), POST_NOTIFICATIONS | Permission matrix scenarios, T4 smoke |
| API 30 emulator | `createWriteRequest`/`createTrashRequest` consent flows; scoped-storage enforcement | STORAGE cases (rename/trash/move consent), T4 smoke |
| API 29 emulator | `RecoverableSecurityException` recovery path; IS_PENDING inserts | STORAGE consent-recovery cases, screenshot save path, T4 smoke |
| API 26 emulator | Legacy branch coverage: DATA-path rename, legacy delete confirm, pre-O PiP absence, `getFrameAtTime` fallback | DV-006 cases; T4 smoke |
| API 24 emulator (occasional) | Absolute floor: no PiP, legacy everything | Pre-release sanity only |
| **Physical device (any modern Android)** | Multi-touch, haptics, real thermal/battery, BT audio, Play-track installs, real ad fill | DV-001/002/005/008/010/012; T4 soak; certification smoke |

Rules: T4 = full regression on primary + smoke + level-specific scenarios on each
secondary + the physical-device duty list. Keep every shipped release AAB/APK archived —
the upgrade-path test installs the previous one first.
