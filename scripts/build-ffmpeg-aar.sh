#!/usr/bin/env bash
#
# Builds the Media3 FFmpeg *audio* decoder extension as a local .aar and drops it in app/libs.
#
#   ./scripts/build-ffmpeg-aar.sh            # full build (clone → ffmpeg → aar → copy)
#   ./scripts/build-ffmpeg-aar.sh --clean    # wipe build/ffmpeg-src first
#
# This is the licensing audit trail for the shipped binary: the Media3 tag, the FFmpeg
# branch, the NDK, and — most importantly — ENABLED_DECODERS are all pinned here. The
# decoder list is deliberately the single thing to edit if the AC-3/E-AC-3/DTS/TrueHD
# decision in CLAUDE.md is ever revisited (they are patent-encumbered, not a license
# problem; the build is plain LGPL-2.1 with no GPL components — see the linking note in
# the BUILD-INFO file this script writes).
#
# Requirements (all present on the dev Mac as of 2026-11): Android NDK (NDK_VERSION below),
# SDK cmake, JDK 17+, git, make, bash. No host ffmpeg needed. Network: github.com.
#
# The resulting .aar must match the app's media3 version EXACTLY (the extension uses
# internal ExoPlayer APIs). Bumping media3 in libs.versions.toml means re-running this
# script with MEDIA3_TAG updated.

set -euo pipefail

# ---- pinned inputs -----------------------------------------------------------------------
MEDIA3_TAG="1.6.1"                 # must equal `media3` in gradle/libs.versions.toml
NDK_VERSION="27.1.12297006"        # ~/Library/Android/sdk/ndk/<this>
# Must equal media3's OWN minSdk (constants.gradle: 21), not the app's 24: the module links
# libffmpegJNI.so at API 21, and FFmpeg objects built against a newer sysroot reference libc
# symbols (e.g. `stderr`) that only exist as macros at 21 → "undefined symbol: stderr".
ANDROID_API="21"
SHIPPED_ABIS=(arm64-v8a armeabi-v7a)   # x86/x86_64 are stripped from the .aar (emulator is arm64)

# LGPL-only, patent-free set. Platform MediaCodec is tried first (EXTENSION_RENDERER_MODE_ON);
# these only engage when the device has no decoder for the codec.
ENABLED_DECODERS=(
  # Google's default set — insurance for odd profiles the platform decoder rejects
  vorbis opus flac alac pcm_mulaw pcm_alaw mp3 aac
  # The genuine platform gaps this build exists for
  mp2 wmav1 wmav2 wmapro wmalossless ape wavpack
  # DELIBERATELY ABSENT (CLAUDE.md "AC3/DTS omitted — revisit licensing at 1.0"):
  #   ac3 eac3 dca truehd mlp
)

# ---- paths -------------------------------------------------------------------------------
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${REPO_ROOT}/build/ffmpeg-src"           # gitignored via /build
MEDIA_DIR="${WORK}/media"
MODULE_PATH="${MEDIA_DIR}/libraries/decoder_ffmpeg/src/main"
OUT_DIR="${REPO_ROOT}/app/libs"
OUT_AAR="${OUT_DIR}/media3-decoder-ffmpeg-${MEDIA3_TAG}.aar"
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
NDK_PATH="${SDK_DIR}/ndk/${NDK_VERSION}"
HOST_PLATFORM="darwin-x86_64"      # NDK ships only this prebuilt dir on macOS, arm64 hosts included

if [[ "${1:-}" == "--clean" ]]; then rm -rf "${WORK}"; fi
mkdir -p "${WORK}" "${OUT_DIR}"

[[ -d "${NDK_PATH}" ]] || { echo "NDK ${NDK_VERSION} not found at ${NDK_PATH}"; exit 1; }
grep -q "^media3 = \"${MEDIA3_TAG}\"" "${REPO_ROOT}/gradle/libs.versions.toml" \
  || { echo "libs.versions.toml media3 version != ${MEDIA3_TAG}; update MEDIA3_TAG"; exit 1; }

step() { printf '\n\033[1;35m==> %s\033[0m\n' "$*"; }

# ---- 1. media3 checkout ------------------------------------------------------------------
if [[ ! -d "${MEDIA_DIR}/.git" ]]; then
  step "Cloning androidx/media @ ${MEDIA3_TAG}"
  git clone --depth 1 --branch "${MEDIA3_TAG}" https://github.com/androidx/media.git "${MEDIA_DIR}"
else
  step "media3 checkout present (${MEDIA_DIR})"
fi

# ---- 2. FFmpeg checkout at the branch this media3 release expects ------------------------
FFMPEG_BRANCH="$(grep -o 'release/[0-9.]*' "${MEDIA_DIR}/libraries/decoder_ffmpeg/README.md" | head -1)"
[[ -n "${FFMPEG_BRANCH}" ]] || { echo "Could not detect FFmpeg branch from the media3 README"; exit 1; }
FFMPEG_DIR="${MODULE_PATH}/jni/ffmpeg"
if [[ ! -d "${FFMPEG_DIR}/.git" ]]; then
  step "Cloning FFmpeg @ ${FFMPEG_BRANCH}"
  git clone --depth 1 --branch "${FFMPEG_BRANCH}" https://github.com/FFmpeg/FFmpeg.git "${FFMPEG_DIR}"
else
  step "FFmpeg checkout present (${FFMPEG_BRANCH})"
fi

# ---- 3. Build libavcodec/libavutil/libswresample for Android ------------------------------
# media3's script builds all four ABIs into jni/ffmpeg/android-libs/<abi>; x86 ones are
# dropped from the .aar at the end rather than patching the upstream script.
if [[ ! -d "${FFMPEG_DIR}/android-libs/arm64-v8a" ]]; then
  step "Building FFmpeg (decoders: ${ENABLED_DECODERS[*]})"
  ( cd "${MODULE_PATH}/jni" && \
    ./build_ffmpeg.sh "${MODULE_PATH}" "${NDK_PATH}" "${HOST_PLATFORM}" "${ANDROID_API}" "${ENABLED_DECODERS[@]}" )
else
  step "FFmpeg android-libs present — skipping (use --clean to rebuild)"
fi

# ---- 4. Build the media3 module into an .aar ---------------------------------------------
step "Building lib-decoder-ffmpeg .aar"
[[ -f "${MEDIA_DIR}/local.properties" ]] || echo "sdk.dir=${SDK_DIR}" > "${MEDIA_DIR}/local.properties"
( cd "${MEDIA_DIR}" && ./gradlew --no-daemon -q :lib-decoder-ffmpeg:assembleRelease )
BUILT_AAR="$(find "${MEDIA_DIR}/libraries/decoder_ffmpeg" -name '*release*.aar' | head -1)"
[[ -f "${BUILT_AAR}" ]] || { echo "No .aar produced"; exit 1; }

# ---- 5. Strip non-shipped ABIs and install into app/libs ---------------------------------
step "Installing ${OUT_AAR}"
cp "${BUILT_AAR}" "${OUT_AAR}"
for abi in x86 x86_64; do
  zip -q -d "${OUT_AAR}" "jni/${abi}/*" 2>/dev/null || true
done
unzip -l "${OUT_AAR}" | grep 'jni/' || true

cat > "${OUT_DIR}/media3-decoder-ffmpeg-BUILD-INFO.txt" <<EOF
media3-decoder-ffmpeg ${MEDIA3_TAG} — built $(date -u +%Y-%m-%dT%H:%MZ) by scripts/build-ffmpeg-aar.sh
Media3 tag:       ${MEDIA3_TAG}
FFmpeg branch:    ${FFMPEG_BRANCH} ($(git -C "${FFMPEG_DIR}" rev-parse --short HEAD))
NDK:              ${NDK_VERSION}, min API ${ANDROID_API}
ABIs:             ${SHIPPED_ABIS[*]}
License:          FFmpeg LGPL-2.1+ (configure reports "LGPL version 2.1 or later"; no GPL flags).
                  NOTE: media3's build_ffmpeg.sh uses --enable-static, so libavcodec/libavutil/
                  libswresample are linked STATICALLY into libffmpegJNI.so. LGPL 2.1 §6 then
                  requires offering the JNI object code for relinking (or switching to shared).
Enabled decoders: ${ENABLED_DECODERS[*]}
Excluded:         ac3 eac3 dca truehd mlp (patent-encumbered — see CLAUDE.md)
EOF
step "Done. $(du -h "${OUT_AAR}" | cut -f1) → ${OUT_AAR}"
