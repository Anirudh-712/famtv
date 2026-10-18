# FamTV

A clean, ad-free home screen for an Android TV, plus the tools to debloat the TV over Wi-Fi.

- **Launcher** (`launcher/`): a ~28 KB native home screen. Greeting and clock, a big
  favourites row, every other app in a grid, an optional background photo, an ambient glow that
  takes the colour of the focused app, and Inputs / Settings buttons. No ads, no
  recommendations, no network access.
- **Debloat**: a list of tracking, promo and unused packages for your TV. Everything is
  *disabled*, never uninstalled, so it is fully reversible.
- **tvctl** (`tools/`): talks to the TV over network ADB from Python, no `adb` binary needed
  (macOS Local Network privacy blocks the stock one).

## Your details stay local

Everything specific to your household goes in `private/`, which is git-ignored and never pushed:

| File | What | Start from |
|---|---|---|
| `private/tv.env` | `TV_HOST=<the TV's IP>` | — |
| `private/launcher.xml` | greeting name, favourites, hidden apps | `examples/launcher.xml` |
| `private/devices/<tv>.txt` | what to switch off on your TV | `examples/debloat.txt` |

Backups (`backups/`), the ADB key (`tools/.keys/`), screenshots and photos are git-ignored too.

## Setup (once)

```bash
brew install openjdk@17 android-commandlinetools
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sdkmanager "platforms;android-34" "build-tools;34.0.0"
python3 -m venv tools/venv && tools/venv/bin/pip install adb-shell rsa
mkdir -p private/devices && echo "TV_HOST=<tv ip>" > private/tv.env
cp examples/launcher.xml private/launcher.xml
cp examples/debloat.txt private/devices/mytv.txt
```

On the TV: Settings → About → press **Build** 7 times, then Developer options → **USB / ADB
debugging** on. Accept the "Allow debugging?" prompt the first time you connect.

## Use

```bash
tools/tv info                                     # model / Android version
tools/tv shot home.png                            # screenshot
tools/tv key DPAD_DOWN DPAD_CENTER                # press remote buttons

tools/debloat.sh private/devices/mytv.txt         # switch off the bloat
launcher/install.sh private/devices/mytv.txt      # build + install + make it home
tools/restore.sh private/devices/mytv.txt         # back to 100% stock

tools/set-background.sh ~/Pictures/photo.jpg      # photo behind the tiles (jpg/png/heic)
tools/set-background.sh --clear                   # back to plain black
```

After editing `private/launcher.xml`, run `launcher/install.sh` again.

To bring back a single app: `tools/tv sh 'pm enable <package>'`, or on the TV go to
Settings → Apps → (app) → Enable.
