#!/usr/bin/env python3
"""VixPlay — ASO promotional composition pipeline.

Turns AUTHENTIC app screenshots (assets/store/raw/) into Google-Play-ready promo
screenshots (1080x1920) + a feature graphic (1024x500), using the app's real brand:
Inter font (res/font) and the indigo->violet tokens. It never invents UI — the device
screenshot is the only visual content; we add a dark background, a headline/subtitle,
and a subtle accent frame around the real screenshot.

Usage:  python3 scripts/aso/generate-assets.py [--raw DIR] [--out DIR]
Missing raw sources are skipped with a warning, so partial runs work before all six
captures exist.
"""
from __future__ import annotations
import argparse, os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")

# Brand tokens (from CLAUDE.md / theme/Color.kt)
BRAND_START = (0x6D, 0x5B, 0xFF)   # indigo
BRAND_END   = (0xB2, 0x4B, 0xFF)   # violet
BG_TOP      = (0x17, 0x15, 0x20)
BG_BOTTOM   = (0x0A, 0x09, 0x0F)
CONTENT     = (0xF2, 0xF3, 0xF5)   # headline
MUTED       = (0xA0, 0xA3, 0xAD)   # subtitle

PHONE = (1080, 1920)
FEATURE = (1024, 500)

# Six screens: (raw source, output, headline, subtitle)
SCREENS = [
    ("01_offline_video_player", "01_offline_video_player", "Watch videos offline", "Your favorite videos, anytime."),
    ("02_video_formats",        "02_video_formats",        "Play your favorite formats", "MP4, MKV, WebM and more."),
    ("03_subtitles",            "03_subtitles",            "Subtitles made simple", "Enjoy videos with subtitles."),
    ("04_continue_watching",    "04_continue_watching",    "Pick up where you left off", "Resume your videos effortlessly."),
    ("05_playback_controls",    "05_playback_controls",    "Control every moment", "Smart gestures. Effortless playback."),
    ("06_video_folders",        "06_video_folders",        "All your videos, organized", "Find what you want to watch."),
]


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    p = os.path.join(FONT_DIR, name)
    return ImageFont.truetype(p, size) if os.path.exists(p) else ImageFont.load_default()


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def vgradient(size, top, bottom):
    w, h = size
    base = Image.new("RGB", (1, h))
    for y in range(h):
        base.putpixel((0, y), lerp(top, bottom, y / max(1, h - 1)))
    return base.resize((w, h))


def hgradient_strip(w, h, c0, c1):
    strip = Image.new("RGB", (w, 1))
    for x in range(w):
        strip.putpixel((x, 0), lerp(c0, c1, x / max(1, w - 1)))
    return strip.resize((w, h))


def accent_glow(canvas, cx, cy, radius, color, alpha=90):
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    d.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=color + (alpha,))
    glow = glow.filter(ImageFilter.GaussianBlur(radius // 2))
    canvas.alpha_composite(glow)


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius, fill=255)
    return m


def wrap(draw, text, fnt, max_w):
    words, lines, cur = text.split(), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if draw.textlength(t, font=fnt) <= max_w:
            cur = t
        else:
            lines.append(cur); cur = w
    if cur:
        lines.append(cur)
    return lines


def place_screenshot(canvas, shot, box, radius=40, border=4):
    """Fit `shot` inside `box`=(x,y,w,h), rounded + shadow + gradient border, centered."""
    bx, by, bw, bh = box
    sw, sh = shot.size
    scale = min(bw / sw, bh / sh)
    nw, nh = int(sw * scale), int(sh * scale)
    shot = shot.resize((nw, nh), Image.LANCZOS).convert("RGBA")
    x = bx + (bw - nw) // 2
    y = by + (bh - nh) // 2
    # drop shadow
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle([x - 6, y + 10, x + nw + 6, y + nh + 22], radius + 8, fill=(0, 0, 0, 150))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(26)))
    # gradient border card (slightly larger, behind)
    bcard = hgradient_strip(nw + 2 * border, nh + 2 * border, BRAND_START, BRAND_END).convert("RGBA")
    bcard.putalpha(rounded_mask(bcard.size, radius + border))
    canvas.alpha_composite(bcard, (x - border, y - border))
    # the real screenshot, rounded
    shot.putalpha(rounded_mask((nw, nh), radius))
    canvas.alpha_composite(shot, (x, y))


def build_phone(shot, headline, subtitle):
    canvas = vgradient(PHONE, BG_TOP, BG_BOTTOM).convert("RGBA")
    accent_glow(canvas, PHONE[0] // 2, 120, 520, BRAND_START, alpha=70)
    d = ImageDraw.Draw(canvas)
    M = 72
    maxw = PHONE[0] - 2 * M
    hf = font("inter_bold.ttf", 74)
    sf = font("inter_medium.ttf", 38)
    y = 104
    for line in wrap(d, headline, hf, maxw)[:2]:
        d.text((M, y), line, font=hf, fill=CONTENT)
        y += 88
    y += 10
    d.text((M, y), subtitle, font=sf, fill=MUTED)
    y += 64
    canvas.alpha_composite(
        _bar(110, 8), (M, y)
    )
    y += 40
    place_screenshot(canvas, shot, (M, y, maxw, PHONE[1] - y - 80))
    return canvas.convert("RGB")


def _bar(w, h):
    bar = hgradient_strip(w, h, BRAND_START, BRAND_END).convert("RGBA")
    bar.putalpha(rounded_mask((w, h), h // 2))
    return bar


def build_feature(shot):
    canvas = vgradient(FEATURE, (0x23, 0x1B, 0x3A), BG_BOTTOM).convert("RGBA")
    accent_glow(canvas, 180, 120, 300, BRAND_START, alpha=90)
    d = ImageDraw.Draw(canvas)
    M = 56
    # wordmark: "Vix" white + "Play" accent-ish
    wf = font("inter_bold.ttf", 76)
    d.text((M, 70), "Vix", font=wf, fill=CONTENT)
    vw = d.textlength("Vix", font=wf)
    d.text((M + vw, 70), "Play", font=wf, fill=BRAND_END)
    d.text((M, 182), "YOUR VIDEOS. YOUR WAY.", font=font("inter_bold.ttf", 46), fill=CONTENT)
    d.text((M, 250), "Offline Video Player for Android", font=font("inter_medium.ttf", 28), fill=MUTED)
    canvas.alpha_composite(_bar(120, 7), (M, 316))
    if shot is not None:
        place_screenshot(canvas, shot, (FEATURE[0] - 300, 40, 240, FEATURE[1] - 80), radius=28, border=3)
    return canvas.convert("RGB")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", default=os.path.join(ROOT, "assets", "store", "raw"))
    ap.add_argument("--out", default=os.path.join(ROOT, "assets", "store", "final"))
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    made = 0
    first_shot = None
    for src, out, head, sub in SCREENS:
        p = os.path.join(a.raw, src + ".png")
        if not os.path.exists(p):
            print(f"  skip {out}: missing raw/{src}.png"); continue
        shot = Image.open(p).convert("RGB")
        if first_shot is None:
            first_shot = shot
        img = build_phone(shot, head, sub)
        dst = os.path.join(a.out, out + ".png")
        img.save(dst, "PNG", optimize=True)
        print(f"  wrote final/{out}.png  {img.size[0]}x{img.size[1]}")
        made += 1
    # feature graphic (uses screen 01 if present)
    fg_src = os.path.join(a.raw, "01_offline_video_player.png")
    fg_shot = Image.open(fg_src).convert("RGB") if os.path.exists(fg_src) else first_shot
    fg = build_feature(fg_shot)
    fg.save(os.path.join(a.out, "feature_graphic.png"), "PNG", optimize=True)
    print(f"  wrote final/feature_graphic.png  {fg.size[0]}x{fg.size[1]}")
    print(f"Done: {made}/6 screenshots + feature graphic. Out: {a.out}")
    if made < 6:
        print("  (run the capture script to produce the remaining raw screenshots)")


if __name__ == "__main__":
    main()
