#!/usr/bin/env bash
# Put the TV back to stock: re-enable everything in a device list (launcher lines included)
# and make the stock launcher home again.
# Usage: tools/restore.sh private/devices/<tv>.txt [stock launcher pkg]
set -euo pipefail
cd "$(dirname "$0")/.."
LIST="${1:?usage: restore.sh <list> [stock launcher]}"
STOCK="${2:-com.google.android.tvlauncher}"
{
  sed -E 's/^#@launcher[[:space:]]+//' "$LIST" | grep -vE '^\s*(#|$)' | sed -E 's/[[:space:]]+#.*$//' \
    | sed 's/^/pm enable /'
  echo "cmd package set-home-activity --user 0 $STOCK"
  echo "pm disable-user --user 0 app.famtv.home"
  echo "input keyevent KEYCODE_HOME"
} | tools/venv/bin/python -I tools/tvctl.py batch
