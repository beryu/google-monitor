#!/bin/sh
# Restore the two Google TV packages on this Xiaomi Google TV. No packages are removed.
set -eu
if [ "$#" -ne 1 ]; then
    echo "Usage: ADB=/path/to/adb sh scripts/restore-google-tv.sh TV_IP:PORT" >&2
    exit 2
fi
quiet_tv_adb=${ADB:-adb}
"$quiet_tv_adb" -s "$1" shell pm default-state --user 0 com.google.android.apps.tv.launcherx
"$quiet_tv_adb" -s "$1" shell pm default-state --user 0 com.google.android.tungsten.setupwraith
"$quiet_tv_adb" -s "$1" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
"$quiet_tv_adb" -s "$1" shell input keyevent KEYCODE_HOME
