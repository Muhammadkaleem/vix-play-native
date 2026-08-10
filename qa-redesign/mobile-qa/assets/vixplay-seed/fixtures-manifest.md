# Fixtures manifest — VixPlay media corpus

Copy to `<repo>/qa/fixtures/manifest.md`. Build once (ffmpeg one-liners below), keep in
`qa/fixtures/media/` (or a git-LFS/ignored dir), push with the script at the bottom.
Every scenario's "preconditions" names fixtures from this list — no more hunting for a
test file mid-run.

| File | Exercises | Recipe |
|---|---|---|
| `f_short_5s.mp4` | ≤5 s "finished gate" (no resume chip) | `ffmpeg -f lavfi -i testsrc=d=5:s=640x360 -pix_fmt yuv420p f_short_5s.mp4` |
| `f_std_2min.mp4` | default happy path, resume positions | testsrc, d=120, 1280x720, H.264+AAC |
| `f_long_45min.mp4` | soak, long-seek, sleep timer, thumbnails across a long timeline | testsrc d=2700 (generate once) |
| `f_portrait.mp4` | 9:16 orientation logic | testsrc s=360x640 |
| `f_4k_hevc.mp4` | HW decode pressure, jank lens | `-c:v libx265 -s 3840x2160`, short |
| `f_av1.mkv` | AV1/FFmpeg-extension path | `-c:v libaom-av1`, short |
| `f_multiaudio.mkv` | audio-track sheet (≥2 tracks gate) | mux two AAC tracks with language tags eng/deu |
| `f_embedded_subs.mkv` | embedded subtitle selection + styling override | mux an .srt + an .ass stream into mkv |
| `f_sidecar.mp4` + `f_sidecar.srt` | external SAF subtitle load, sync offset | any short clip + generated srt |
| `f_zero_duration.mp4` | duration-badge hidden, resume-gate edges | truncate a header-only file |
| `f_corrupt.mp4` | codec-error overlay (message, no crash) | `head -c 100k f_std_2min.mp4 > f_corrupt.mp4` |
| `f_mistyped.avi` | actually H.264-in-mp4 named .avi — extension/MIME mismatch intent filters | copy f_std_2min.mp4 to .avi |
| `f_ünï🎬de long name … .mp4` | unicode/emoji/very-long filename in lists, rename, share | copy + rename |
| `a_track1.ogg` … `a_track5.ogg` (varied tags: unknown artist, embedded art, no art, `<unknown>`) | audio library grouping, metadata, queue | sine sources + `-metadata` flags |
| `a_long_podcast.ogg` | audio resume, background playback, notification seek | anullsrc 30 min |

Push script (`qa/fixtures/push.sh`):
```bash
#!/usr/bin/env bash
# usage: push.sh [serial]
A=${1:+-s $1}
adb $A shell mkdir -p /sdcard/Movies/QA /sdcard/Music/QA
adb $A push qa/fixtures/media/f_* /sdcard/Movies/QA/
adb $A push qa/fixtures/media/a_* /sdcard/Music/QA/
for f in /sdcard/Movies/QA/* /sdcard/Music/QA/*; do
  adb $A shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$f" >/dev/null
done
echo "fixtures pushed + scanned"
```
(On API 29+ the scan broadcast is best-effort; MediaStore usually indexes pushed files
within seconds. If a fixture doesn't appear, `pm clear com.android.providers.media.module`
is the nuclear option — note it in flakes.md if needed.)
