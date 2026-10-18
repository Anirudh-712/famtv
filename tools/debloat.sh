#!/usr/bin/env bash
# Disable every package in a device list (one per line, # comments). Reversible with restore.sh.
# Usage: tools/debloat.sh private/devices/<tv>.txt
set -euo pipefail
cd "$(dirname "$0")/.."
LIST="${1:?usage: debloat.sh <list>}"
pkgs() { grep -vE '^\s*(#|$)' "$LIST" | sed -E 's/[[:space:]]+#.*$//'; }
{ pkgs | sed 's/^/pm disable-user --user 0 /'; echo "pm list packages -d"; } \
  | tools/venv/bin/python -I tools/tvctl.py batch
