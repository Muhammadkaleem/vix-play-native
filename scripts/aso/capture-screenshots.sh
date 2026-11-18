#!/usr/bin/env bash
# VixPlay — ASO authentic screenshot capture.
#
# Captures REAL screens from the running app into assets/store/raw/. Normalizes the
# status bar (clock 09:41, full signal, no notifications) via Android demo mode, then
# guides the operator through navigating to each of the six ASO screens and screencaps
# each one with the correct filename. (The app has no Compose test tags yet, so blind
# coordinate automation would be fragile — operator-guided capture is the reliable path.
# A Compose-test capture route is a documented follow-up once test tags are added.)
#
# Usage:
#   scripts/aso/capture-screenshots.sh [--serial <id>]            # guided full set
#   scripts/aso/capture-screenshots.sh --one 01_offline_video_player   # single capture now
#   scripts/aso/capture-screenshots.sh --demo-off                 # just restore the status bar
set -euo pipefail

PKG="com.devbytes.vixplayer.app"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
RAW_DIR="$ROOT/assets/store/raw"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
command -v "$ADB" >/dev/null 2>&1 || ADB="adb"   # fall back to PATH

SERIAL=""
MODE="all"
ONE=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial) SERIAL="$2"; shift 2 ;;
    --one) MODE="one"; ONE="$2"; shift 2 ;;
    --demo-off) MODE="demo-off"; shift ;;
    *) echo "Unknown arg: $1" >&2; exit 2 ;;
  esac
done

adbx() { if [[ -n "$SERIAL" ]]; then "$ADB" -s "$SERIAL" "$@"; else "$ADB" "$@"; fi; }

# Pick the only device if no serial was given and exactly one is attached.
if [[ -z "$SERIAL" ]]; then
  mapfile -t DEVS < <("$ADB" devices | awk 'NR>1 && $2=="device"{print $1}')
  if [[ ${#DEVS[@]} -eq 0 ]]; then echo "No device/emulator attached." >&2; exit 1; fi
  if [[ ${#DEVS[@]} -gt 1 ]]; then echo "Multiple devices; pass --serial. Found: ${DEVS[*]}" >&2; exit 1; fi
  SERIAL="${DEVS[0]}"
fi
echo "Device: $SERIAL"

demo_on() {
  adbx shell settings put global sysui_demo_allowed 1 >/dev/null
  adbx shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
  adbx shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
  adbx shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
  adbx shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4 >/dev/null
  adbx shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
  adbx shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
  echo "Status bar normalized (09:41, full signal, no notifications)."
}
demo_off() {
  adbx shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
  echo "Status bar restored."
}

cap() {  # cap <name>
  local name="$1"
  mkdir -p "$RAW_DIR"
  adbx exec-out screencap -p > "$RAW_DIR/$name.png"
  if [[ ! -s "$RAW_DIR/$name.png" ]]; then echo "  ! capture failed for $name" >&2; return 1; fi
  local dim; dim=$(python3 -c "from PIL import Image;import sys;print('%dx%d'%Image.open(sys.argv[1]).size)" "$RAW_DIR/$name.png" 2>/dev/null || echo "?")
  echo "  saved raw/$name.png ($dim)"
}

launch_home() { adbx shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; }

if [[ "$MODE" == "demo-off" ]]; then demo_off; exit 0; fi

demo_on
trap demo_off EXIT   # always restore the status bar on exit

if [[ "$MODE" == "one" ]]; then
  echo "Capturing the screen currently on display as: $ONE"
  cap "$ONE"
  exit 0
fi

# Guided full set. Operator navigates, presses Enter; the script captures + names correctly.
launch_home
sleep 2
SCREENS=(
  "01_offline_video_player|Open a SAMPLE video in the player; tap once so the HUD/controls are visible."
  "02_video_formats|Go to the Video tab (or a folder) showing a grid of sample clips."
  "03_subtitles|Open the subtitle selection sheet (player ⋮ → Subtitles) OR a clip with subtitles on."
  "04_continue_watching|Go Home; show the Continue Watching hero with real resume progress."
  "05_playback_controls|In the player, open the options panel OR trigger a brightness/volume/speed HUD."
  "06_video_folders|Open the folder browser showing sample folders."
)
echo
echo "Guided capture — navigate on the device, then press Enter at each prompt."
for entry in "${SCREENS[@]}"; do
  name="${entry%%|*}"; hint="${entry#*|}"
  echo
  echo ">>> $name"
  echo "    $hint"
  read -r -p "    Press Enter to capture (or type s to skip): " ans
  [[ "$ans" == "s" ]] && { echo "  skipped"; continue; }
  cap "$name" || true
done
echo
echo "Raw captures in: $RAW_DIR"
echo "Next: python3 scripts/aso/generate-assets.py   then   python3 scripts/aso/validate-assets.py"
