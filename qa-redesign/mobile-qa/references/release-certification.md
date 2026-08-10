# Release certification — T4 (RC validation) and T5 (production sign-off)

The organizing question changes at T4. T1–T3 ask "does the new thing work?";
T4–T5 ask "will the thing we ship harm anyone who already uses it?" — which is why
upgrade-path and release-build testing dominate here, and why debug-on-emulator evidence
stops being sufficient.

## T4 — RC validation

Run in this order (cheap failures first):

### 1. Static release audit (parallel, qa-static-sweeper)
- Version bump sanity (versionCode strictly increasing; versionName coherent).
- Manifest diff vs last release: new permissions? new exported components? Justify each.
- `allowBackup`/`dataExtractionRules` vs any sensitive local data (vaults, credentials).
- Dependency & license diff; R8 mapping file generated and archived; native libs
  alignment (16 KB pages on modern Android).

### 2. Full regression (T3) on the primary target
Per quality-gates.md. Debug build is acceptable *here only*.

### 3. Signed-release-artifact smoke — test what ships
```bash
./gradlew bundleRelease
bundletool build-apks --bundle app-release.aab --output rc.apks --mode=universal \
  --ks <keystore> --ks-key-alias <alias>
bundletool install-apks --apks rc.apks
```
Then the full smoke suite on this install. This is where R8/ProGuard breaks reflection
users (serialization libs, network stacks, media codec factories), where real ad units
replace test ads, and where debug-only scaffolding disappears. A debug-only test cycle
has literally never exercised the binary users get.

### 4. Upgrade-path (mandatory whenever any schema/prefs/storage format changed — and
cheap enough to always run)
1. Install the **previous released** artifact (keep every shipped AAB/APK in an archive).
2. Create real data: play media partway (resume positions), build playlists, set
   preferences/profiles, set up any PIN/vault, store any credentials.
3. Install the RC **over it** (`adb install -r` the universal APK).
4. Verify: app launches (migrations ran), every category of data intact, no
   re-onboarding, no permission regression.
Data loss here is P0. This is the single most likely catastrophic-regression vector in
any app that ships database migrations.

### 5. Device-matrix smoke (qa/device-matrix.md)
Full regression on the primary target; smoke + the release's API-sensitive scenarios on
each secondary image (chosen to hit `SDK_INT` fault lines); one physical device for
touch-hardware, haptics, real ads, Bluetooth.

### 6. Soak & abuse
- 30-minute continuous core-usage session (for a media app: long playback with periodic
  seeks, background/foreground cycles, route changes). Zero crashes; `meminfo` sampled
  every 5 min must not climb monotonically.
- `monkey` **is** appropriate here (crash-fuzzing, not navigation):
  `adb shell monkey -p <pkg> --throttle 200 --pct-syskeys 0 -s 42 50000` — seed pinned
  for reproducibility. Any crash is a real crash.

### 7. Perf baselines
Cold start, key latencies, jank, size — recorded against the previous release's numbers.

Output: T4 report per quality-gates.md, verdict computed.

## T5 — Certification (qa-release-auditor)

Independence rule: the auditor must be a fresh context (subagent or new session) that
did **not** execute T4. Its job is distrust:

1. Re-derive the verdict from the gate table using only evidence **on file** — a claimed
   PASS with no screenshot/log is treated as not run and sends the RC back.
2. Walk qa/ledgers/deferred-verification.md: every open item gets an explicit decision —
   *accept* (with why + revisit-by date) or *block the release*. Silence is not acceptance.
3. Walk qa/ledgers/bugs.md for open P1/P2: confirm none violates the gate table.
4. Confirm operational readiness: crash reporting live in release build (symbols/mapping
   uploaded), staged rollout plan with an abort threshold (e.g. halt below 99.5%
   crash-free sessions), rollback path (previous artifact re-promotable).
5. Write the sign-off document (template in quality-gates.md) into qa/reports/.

CERTIFIED-WITH-RISKS is an honorable verdict — shipping with named, accepted risks is
engineering; shipping with unnamed ones is negligence. REJECTED must cite the exact
failed gate(s).
