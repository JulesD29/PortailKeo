#!/usr/bin/env bash
# Lancé par le workflow Capture dans le téléphone virtuel.
set -x
PKG=fr.julesdupont.portail
OUT="$RUNNER_TEMP/captures"
mkdir -p "$OUT"
adb install -g app/build/outputs/apk/debug/app-debug.apk
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
adb shell wm size > "$OUT/ecran.txt"; adb shell wm density >> "$OUT/ecran.txt"
TODAY=$(date +%F)
for THEME in LIGHT DARK; do
  cat > /tmp/portail.xml <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="setupDone" value="true" />
    <string name="phone">0600000000</string>
    <string name="lat">48.39</string>
    <string name="lon">-4.48</string>
    <int name="lastSeenWhatsNew" value="99" />
    <string name="lastUpdateCheck">$TODAY</string>
    <string name="theme">$THEME</string>
</map>
XML
  adb shell am force-stop $PKG
  adb push /tmp/portail.xml /data/local/tmp/portail.xml
  adb shell chmod 644 /data/local/tmp/portail.xml
  adb shell run-as $PKG mkdir -p shared_prefs
  adb shell run-as $PKG cp /data/local/tmp/portail.xml shared_prefs/portail.xml
  adb shell input keyevent KEYCODE_HOME
  sleep 3
  adb logcat -c
  adb shell screenrecord --time-limit 14 --bit-rate 8000000 /sdcard/ouverture.mp4 &
  REC=$!
  sleep 1
  adb shell am start -W -n $PKG/.MainActivity
  wait $REC
  sleep 1
  adb pull /sdcard/ouverture.mp4 "$OUT/ouverture_$THEME.mp4"
  adb shell screencap -p /sdcard/fin.png && adb pull /sdcard/fin.png "$OUT/fin_$THEME.png"
  adb logcat -d -v time | grep -iE "portail|AndroidRuntime|MediaPlayer|VideoView" | tail -300 > "$OUT/logcat_$THEME.txt"
done
ls -la "$OUT"
