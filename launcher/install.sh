#!/usr/bin/env bash
# Build, install and make FamTV Home the default home screen, then switch off the stock
# launcher (the #@launcher lines of the device list).
# Usage: launcher/install.sh private/devices/<tv>.txt
set -euo pipefail
cd "$(dirname "$0")/.."
LIST="${1:?usage: install.sh private/devices/<tv>.txt}"
TV="tools/venv/bin/python -I tools/tvctl.py"

launcher/build.sh
$TV push launcher/build/famtv-home.apk /data/local/tmp/famtv-home.apk
{
  echo "pm install -r /data/local/tmp/famtv-home.apk"
  echo "rm -f /data/local/tmp/famtv-home.apk"
  echo "cmd package set-home-activity --user 0 app.famtv.home/.HomeActivity"
  grep -E '^#@launcher' "$LIST" | awk '{print "pm disable-user --user 0 " $2}'
  echo "input keyevent KEYCODE_HOME"
} | $TV batch
