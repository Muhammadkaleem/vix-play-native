#!/usr/bin/env python3
"""VixPlay — ASO asset validation.

Checks the final Google Play assets for exact dimensions, aspect ratio, PNG format,
file size, and presence/ordering. Exits non-zero if any check fails, so it can gate CI
or an upload step.

Usage:  python3 scripts/aso/validate-assets.py [--dir assets/store/final]
"""
from __future__ import annotations
import argparse, os, sys
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MAX_BYTES = 8 * 1024 * 1024  # Play screenshot cap

# (filename, expected_w, expected_h)
EXPECTED = [
    ("01_offline_video_player.png", 1080, 1920),
    ("02_video_formats.png",        1080, 1920),
    ("03_subtitles.png",            1080, 1920),
    ("04_continue_watching.png",    1080, 1920),
    ("05_playback_controls.png",    1080, 1920),
    ("06_video_folders.png",        1080, 1920),
    ("feature_graphic.png",         1024, 500),
]


def check(path, ew, eh):
    errs = []
    if not os.path.exists(path):
        return ["MISSING"]
    size = os.path.getsize(path)
    if size > MAX_BYTES:
        errs.append(f"too big ({size/1_048_576:.1f} MB > 8 MB)")
    try:
        im = Image.open(path)
    except Exception as e:
        return [f"not an image: {e}"]
    if im.format != "PNG":
        errs.append(f"format {im.format} != PNG")
    if im.size != (ew, eh):
        errs.append(f"dims {im.size[0]}x{im.size[1]} != {ew}x{eh}")
    # aspect (phone shots must be 9:16)
    if (ew, eh) == (1080, 1920):
        ar = im.size[0] / im.size[1]
        if abs(ar - 9 / 16) > 0.001:
            errs.append(f"aspect {ar:.4f} != 0.5625 (9:16)")
    return errs


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=os.path.join(ROOT, "assets", "store", "final"))
    a = ap.parse_args()
    print(f"Validating: {a.dir}\n")
    failed = 0
    for i, (name, ew, eh) in enumerate(EXPECTED, 1):
        errs = check(os.path.join(a.dir, name), ew, eh)
        if errs:
            failed += 1
            print(f"  [{i}] FAIL  {name:32s}  {'; '.join(errs)}")
        else:
            sz = os.path.getsize(os.path.join(a.dir, name)) / 1024
            print(f"  [{i}] ok    {name:32s}  {ew}x{eh}  {sz:.0f} KB")
    print()
    if failed:
        print(f"{failed}/{len(EXPECTED)} asset(s) failed validation.")
        sys.exit(1)
    print(f"All {len(EXPECTED)} assets valid (dimensions, aspect, PNG, <8 MB).")


if __name__ == "__main__":
    main()
