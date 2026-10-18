#!/usr/bin/env python3
"""tvctl: talk to an Android TV over network ADB without the adb binary.

    tvctl.py info                      # TV address: $TV_HOST or private/tv.env
    tvctl.py sh 'pm list packages'
    tvctl.py batch < cmds.txt          # one command per line, run separately
    tvctl.py push <local> <remote>     # base64 chunks + md5 check, safe for APKs
    tvctl.py pull <remote> <local>
    tvctl.py shot out.png              # screenshot from the TV
    tvctl.py key DPAD_DOWN HOME ...    # inject remote key presses
"""
import base64
import hashlib
import os
import sys
import time

from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.keygen import keygen
from adb_shell.auth.sign_pythonrsa import PythonRSASigner

def _private_env():
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "private", "tv.env")
    if os.path.exists(path):
        for line in open(path):
            k, _, v = line.strip().partition("=")
            if k and not k.startswith("#"):
                os.environ.setdefault(k, v)


_private_env()
HOST = os.environ.get("TV_HOST")
if not HOST:
    sys.exit("Set the TV's IP: put TV_HOST=<ip> in private/tv.env (git-ignored) or export TV_HOST")
PORT = int(os.environ.get("TV_PORT", "5555"))
KEY = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".keys", "adbkey")
CHUNK = 48 * 1024


def device():
    if not os.path.exists(KEY):
        os.makedirs(os.path.dirname(KEY), exist_ok=True)
        keygen(KEY)
    signer = PythonRSASigner(open(KEY + ".pub").read(), open(KEY).read())
    dev = AdbDeviceTcp(HOST, PORT, default_transport_timeout_s=120.0)
    dev.connect(rsa_keys=[signer], auth_timeout_s=120.0)
    return dev


def push(dev, local, remote):
    data = open(local, "rb").read()
    tmp = remote + ".b64"
    dev.shell(f"rm -f {tmp}")
    enc = base64.b64encode(data).decode()
    for i in range(0, len(enc), CHUNK):
        dev.shell(f"echo -n '{enc[i:i + CHUNK]}' >> {tmp}")
        print(f"\r{min(i + CHUNK, len(enc)) * 100 // len(enc)}%", end="", flush=True)
    dev.shell(f"base64 -d {tmp} > {remote} && rm -f {tmp}")
    want, got = hashlib.md5(data).hexdigest(), dev.shell(f"md5sum {remote}").split()[0]
    print(f"\n{remote}: md5 {'ok' if want == got else 'MISMATCH ' + got}")
    if want != got:
        sys.exit(1)


def main():
    cmd, args = (sys.argv[1], sys.argv[2:]) if len(sys.argv) > 1 else ("info", [])
    dev = device()
    if cmd == "info":
        for p in ("ro.product.manufacturer", "ro.product.model", "ro.product.device",
                  "ro.build.version.release", "ro.build.version.sdk", "ro.build.display.id"):
            print(f"{p:28} {dev.shell('getprop ' + p).strip()}")
    elif cmd == "sh":
        print(dev.shell(" ".join(args), timeout_s=300), end="")
    elif cmd == "batch":
        for line in sys.stdin:
            if line.strip() and not line.lstrip().startswith("#"):
                print(f"$ {line.strip()}\n{dev.shell(line.strip(), timeout_s=300)}", end="")
    elif cmd == "push":
        push(dev, *args)
    elif cmd == "pull":
        dev.pull(*args)
    elif cmd == "shot":
        dev.shell("screencap -p /sdcard/_shot.png")
        dev.pull("/sdcard/_shot.png", args[0] if args else "shot.png")
    elif cmd == "key":
        for k in args:
            dev.shell(f"input keyevent KEYCODE_{k}")
            time.sleep(0.35)
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
