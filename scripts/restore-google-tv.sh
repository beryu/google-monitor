#!/bin/sh
# Restore the two Google TV packages on this Xiaomi Google TV. No packages are removed.
set -eu
if [ "$#" -ne 1 ]; then
    echo "Usage: ADB=/path/to/adb sh scripts/restore-google-tv.sh TV_IP:PORT" >&2
    exit 2
fi
quiet_tv_adb=${ADB:-adb}
current_services=$("$quiet_tv_adb" -s "$1" shell settings get secure enabled_accessibility_services | tr -d '\r')
remaining_services=$(printf '%s\n' "$current_services" | awk -F ':' '
    {
        result = ""
        for (i = 1; i <= NF; i++) {
            if ($i == "null" || $i == "" ||
                $i == "com.beryu.tvhome/.HomeRedirectService" ||
                $i == "com.beryu.tvhome/com.beryu.tvhome.HomeRedirectService" ||
                $i == "com.beryu.tvhome/.HomeKeyService" ||
                $i == "com.beryu.tvhome/com.beryu.tvhome.HomeKeyService") continue
            result = result (result == "" ? "" : ":") $i
        }
        print result
    }')
if [ "$remaining_services" != "$current_services" ]; then
    if [ -n "$remaining_services" ]; then
        "$quiet_tv_adb" -s "$1" shell settings put secure enabled_accessibility_services "$remaining_services"
    else
        "$quiet_tv_adb" -s "$1" shell settings delete secure enabled_accessibility_services
    fi
fi
"$quiet_tv_adb" -s "$1" shell pm default-state --user 0 com.google.android.apps.tv.launcherx
"$quiet_tv_adb" -s "$1" shell pm default-state --user 0 com.google.android.tungsten.setupwraith
"$quiet_tv_adb" -s "$1" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
"$quiet_tv_adb" -s "$1" shell input keyevent KEYCODE_HOME
