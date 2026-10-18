#!/usr/bin/env bash
# Build the launcher APK with the bare Android SDK tools (no Gradle).
# Output: launcher/build/famtv-home.apk
set -euo pipefail
cd "$(dirname "$0")"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
SDK="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
OUT=build
rm -rf "$OUT" && mkdir -p "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir res -o "$OUT/res/res.zip"
# Personal settings (family name, favourites...) live in git-ignored private/launcher.xml and are
# layered over the defaults, so they never end up in the repo.
OVERLAY=()
if [ -f ../private/launcher.xml ]; then
  mkdir -p "$OUT/private/values" && cp ../private/launcher.xml "$OUT/private/values/private.xml"
  "$BT/aapt2" compile --dir "$OUT/private" -o "$OUT/res/private.zip"
  OVERLAY=(-R "$OUT/res/private.zip")
fi
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml --java "$OUT/gen" \
  -o "$OUT/unsigned.apk" "$OUT/res/res.zip" "${OVERLAY[@]}"

"$JAVA_HOME/bin/javac" -nowarn --release 8 -classpath "$JAR" \
  -d "$OUT/classes" $(find src "$OUT/gen" -name '*.java')
"$BT/d8" --min-api 24 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)

KS="$HOME/.android/famtv-debug.keystore"
[ -f "$KS" ] || "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$KS" -storepass famtv123 \
  -keypass famtv123 -alias famtv -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=FamTV" >/dev/null 2>&1
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:famtv123 --out "$OUT/famtv-home.apk" "$OUT/aligned.apk"
echo "built $OUT/famtv-home.apk ($(du -h "$OUT/famtv-home.apk" | cut -f1))"
