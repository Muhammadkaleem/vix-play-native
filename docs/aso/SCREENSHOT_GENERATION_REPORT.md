# VixPlay — ASO Screenshot Generation Report

## Status

| Phase | Deliverable | State |
|---|---|---|
| 1 | `docs/aso/APP_SCREEN_AUDIT.md` | ✅ Done — production-readiness audit; all 6 ASO screens map to production-ready surfaces. |
| 2/6 | `scripts/aso/capture-screenshots.sh` | ✅ Done — demo-mode status-bar normalization (09:41, full signal, no notifications) + operator-guided capture into `assets/store/raw/`. |
| 4/5 | `scripts/aso/generate-assets.py` | ✅ Done & **pipeline-tested** — composes authentic screenshots → 6× `1080×1920` promos + `1024×500` feature graphic, using the app's Inter font + indigo→violet brand tokens. |
| 7 | `scripts/aso/validate-assets.py` | ✅ Done & **tested** — enforces exact dims, 9:16 aspect, PNG, <8 MB; non-zero exit on failure. |
| 3 | Authentic captures (`assets/store/raw/*.png`) | ⛔ **Blocked on a decision** — see below. |
| 5/9 | Final promos + feature graphic + ZIP (`assets/store/final/`) | ⛔ Waits on the authentic captures. |

**Pipeline self-test (scratchpad, not published):** fed 6 placeholder images through
`generate-assets.py` → 6 `1080×1920` + 1 `1024×500` PNGs; `validate-assets.py` → all 7 pass.
The composition (dark gradient, Inter headline, accent frame around the real screenshot)
is confirmed working end-to-end.

## ⛔ The block: capture source (privacy)

The only connected device (`R5CX33WER1P`) holds the **owner's personal video library**.
The library / folders / Continue-Watching screens render that content directly, so
capturing them would put **personal media into a public Play Store listing** — which the
brief forbids and which is effectively irreversible once published.

**Two safe ways forward (owner's choice):**
1. **Seed safe sample media** — place a handful of original/CC0 clips (+ an `.srt`) into a
   clean folder (or a fresh emulator) and capture the six screens from those. Clean,
   publishable, brand-representative.
2. **Owner hand-picks** which specific non-personal clips may appear, and we capture only those.

Either way the pipeline is one command each:

```bash
scripts/aso/capture-screenshots.sh            # guided capture → assets/store/raw/
python3 scripts/aso/generate-assets.py        # raw → assets/store/final/ (6 promos + feature graphic)
python3 scripts/aso/validate-assets.py        # gate dims / aspect / size
```

## Formats-copy caveat (carried from the audit)
Screenshot 02's suggested "AVI" is **not guaranteed** without the (not-yet-built) FFmpeg
extension. Default copy uses **"MP4, MKV, WebM and more."** Change to include AVI only after
confirming an `.avi` plays on a device.

## Follow-ups (not blocking)
- **Compose-test capture route** (`app/src/androidTest/.../aso/*ScreenshotTest.kt`): needs
  `Modifier.testTag(...)` added to the target composables for stable, coordinate-free
  navigation. The shell script (operator-guided) is the reliable path until those tags exist.
- `SCREENSHOT_COMPARISON.md` (Phase 8): only the **launcher icon** exists as a prior store
  asset (`config-vix-play/player-design-assets/launcher-icon*/playstore/`); there are **no
  prior Play Store screenshots** in the repo to compare against, so the comparison will be
  "baseline: none" once the new set is produced.
