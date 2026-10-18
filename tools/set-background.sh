#!/usr/bin/env bash
# Put a photo behind the home screen tiles (any jpg/png/heic from the Mac).
# Usage: tools/set-background.sh <photo>     set it
#        tools/set-background.sh --clear     back to plain black
set -euo pipefail
cd "$(dirname "$0")/.."
DEST=/sdcard/Android/data/app.famtv.home/files/background.jpg
if [ "${1:-}" = "--clear" ]; then
  printf 'rm -f %s\nam force-stop app.famtv.home\ninput keyevent KEYCODE_HOME\n' "$DEST" | tools/tv batch
  exit
fi
SRC="${1:?usage: set-background.sh <photo> | --clear}"
TMP="$(mktemp -d)/background.jpg"
# Resize to cover 1920x1080 and re-encode as a ~300 KB JPEG (also converts HEIC from iPhones).
sips -s format jpeg -s formatOptions 80 --resampleHeightWidthMax 1920 "$SRC" --out "$TMP" >/dev/null
tools/tv sh "mkdir -p $(dirname $DEST)" >/dev/null
tools/tv push "$TMP" "$DEST"
printf 'am force-stop app.famtv.home\ninput keyevent KEYCODE_HOME\n' | tools/tv batch >/dev/null
echo "Background set. It shows on the TV now."
