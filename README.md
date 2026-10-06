<div align="center">

# 📞 Portail Keo

**Open the company gate automatically when you arrive — no more calling it by hand every morning.**

[![Release APK](https://github.com/JulesD29/PortailKeo/actions/workflows/build.yml/badge.svg)](https://github.com/JulesD29/PortailKeo/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/JulesD29/PortailKeo?label=version)](https://github.com/JulesD29/PortailKeo/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)

English · [Français](README.fr.md)

</div>

---

Many company gates open when you call a phone number from an authorised mobile.
**Portail Keo** places that call for you as soon as you approach the gate, on the days
and at the times you choose. The app interface is in French.

## Table of contents
- [Features](#features)
- [Installation](#installation)
- [Setup](#setup)
- [Everyday use](#everyday-use)
- [How it works](#how-it-works)
- [Permissions](#permissions)
- [Privacy](#privacy)
- [Troubleshooting](#troubleshooting)
- [Building from source](#building-from-source)
- [Contributing](#contributing)

## Features

| | |
|---|---|
| 🗺️ **Map picker** | Place the gate on an OpenStreetMap map and see both zones as circles while adjusting them. |
| 🧭 **Setup wizard** | First launch walks you through number, permissions, map, battery and a test call. |
| 🔗 **Share by QR code** | Show your setup as a QR code; a colleague scans it and everything is filled in. |
| 📍 **Automatic call on arrival** | Two-stage detection: a large approach zone (2 km by default) switches on precise GPS, the gate zone triggers the call. |
| ⏱️ **Countdown** | "Calling the gate in 5 s" notification with **Cancel** and **Call now**. |
| 📅 **Schedule** | Active days, time window, at most one automatic call per day. |
| 🏖️ **Pause & public holidays** | Pause until a given date; no call on French public holidays. |
| 📲 **Manual call** | **Appeler Garage** Quick Settings tile, home-screen widget and a launcher shortcut. |
| 🎙️ **Voice command** | "Ok Google, ouvre Portail" calls the gate — handy while driving (optional). |
| 📳 **Pocket feedback** | Vibration when the call starts; double vibration and "Portail appelé" in your earbuds when it ends. |
| 📊 **History** | Calls this week, average arrival time and latest calls. |
| 🔄 **Automatic updates** | The app checks GitHub Releases and installs new versions itself. |
| 📝 **Log** | Every zone entry, call and reason for not calling is recorded in the app. |

## Installation

> Requires Android 8.0 or later. The app is not on the Play Store; you install the APK directly.

1. On your phone, download the app: **https://github.com/JulesD29/PortailKeo/releases/latest/download/PortailKeo.apk**
   (or open the [latest release](https://github.com/JulesD29/PortailKeo/releases/latest) page).
2. Open the downloaded file. If Android asks, allow your browser or file manager to
   *install unknown apps*.
3. If Play Protect shows a warning: **More details › Install anyway**.

> [!TIP]
> If the download stays stuck at 100 % in Chrome, pull down the notification shade and tap
> **Download anyway**.

After this first install, updates are offered **inside the app** (see [Automatic updates](#automatic-updates)).

## Setup

On first launch an **assistant** guides you step by step. A colleague already uses the app?
On their phone: *Partager ma configuration (QR code)*; on yours: **Scanner la configuration d'un
collègue** — only the permissions are left to grant.

The same settings are available afterwards on the main screen:

1. **Numéro du portail** — the number that opens the gate.
2. **Choisir sur la carte** — move the map until the crosshair is on the gate and adjust the two
   circles with the sliders: blue = gate zone (call, **200–400 m** recommended), orange = approach
   zone (precise GPS, **2 km** by default). Tap **Valider**.
   *Alternatives:* paste coordinates copied from Google Maps, or **Utiliser ma position actuelle** at the gate.
3. **GPS précis à l'approche** — leave it on.
4. *(the radii can also be typed in directly)*
5. **Compte à rebours** — seconds before the call (**0** = call immediately).
6. **Quand** — days and time window (Mon–Fri, 07:00–10:00 by default).
7. Turn on **Appel automatique à l'arrivée** in the status card at the top. Every change is saved
   immediately; the *Portail* card should read **✓ Zone active**.
8. **Accorder les autorisations** — phone calls, notifications, then location → **Allow all the time**.
9. **Désactiver l'optimisation batterie** — strongly recommended. On Samsung, Xiaomi, Huawei or
   Oppo, also set *Settings › Apps › Portail Keo › Battery* to **Unrestricted**.
10. **Ouvrir le portail maintenant** to check that the call goes through.

> [!IMPORTANT]
> The gate must recognise **your** phone number. If it is not registered with the gate
> system, the call will be placed but the gate will not open.

## Everyday use

### Automatic call
Nothing to do: on your way in, the **Approche du portail** notification shows the remaining
distance, then **Appel du portail dans 5 s**. Tap **Annuler** to cancel; no further automatic
call will be made that day.

### Manual call
- **Quick Settings tile**: pull down the shade twice › ✏️ › drag **Appeler Garage** into your tiles.
  On Android 16, long-press it in edit mode to enlarge it and show its label.
- **Shortcut**: long-press the app icon › **Ouvrir le portail** (you can drag it to your home screen).

The tile and the shortcut always work, even when paused or outside the time window.

### Voice command
Turn on **Commande vocale**: a **Portail** icon is added to your apps. Voice assistants open apps by
name, so **"Ok Google, ouvre Portail"** calls the gate straight away. For an exact phrase such as
*"ouvre le portail"*, create a Google routine (Google Home › Automations › Personal › *When I say…*
→ custom command *"ouvre Portail"*); the in-app **Dire exactement « ouvre le portail »** button explains it.

### Pause and public holidays
- **Mettre en pause…**: no automatic call until the chosen date (inclusive). **Reprendre** cancels it.
- **Pas d'appel les jours fériés** (on by default): French metropolitan public holidays,
  including Easter Monday, Ascension Day and Whit Monday.

### Automatic updates
The app checks for a new version when it opens and twice a day. A notification or dialog offers
to install it; your settings are kept. You can also tap **Rechercher une mise à jour**.
The first time, Android asks you to allow Portail Keo to install apps.

## How it works

```
      Approach zone (2 km)                 Gate zone (200–400 m)
 ─────────────●──────────────────────────────────●──────────────▶ Gate
              │                                  │
      rules OK? → precise GPS             countdown → call
      every 3 s (max 20 min)              (Cancel / Call now)
```

- Zones use Google Play Services **geofencing**, which is battery-friendly.
- In the approach zone a foreground service reads precise GPS until you arrive, leave, or 20 min pass.
- The gate zone stays active as a fallback if precise GPS cannot start.
- Rules checked before any automatic call: enabled, not paused, not a public holiday,
  active day, within the time window, not already called today.
- Zones are re-registered after a reboot, an app update, and every 6 hours.

## Permissions

| Permission | Why |
|---|---|
| Location — *Allow all the time* | Detect your arrival while the app is closed |
| Phone calls | Place the call directly, without opening the dialer |
| Full-screen notifications | Show the call screen over the lock screen when the phone is locked |
| Manage calls (phone state, end calls) | Check the call is ringing (retry if it dropped) and hang up automatically |
| Notifications | Approach, countdown, update and error notifications |
| Battery optimisation exemption | Keep Android from putting the app to sleep |
| Install apps | Install updates downloaded from GitHub |
| Internet | Check for and download updates, load the map |

## Privacy

- Your number, coordinates, settings and log **stay on your phone**; nothing is sent anywhere.
- Network access: the GitHub API (updates) and OpenStreetMap map tiles, only while the map is open.
  Your position is never sent; only the map area being displayed is downloaded.
- No accounts, no analytics, no ads.

## Troubleshooting

| Problem | Fix |
|---|---|
| No call on arrival | Check the **Journal** section in the app: it shows whether the zone was entered and why no call was made. |
| Call placed too late | Increase the gate zone **radius** or the **approach zone**. |
| "✗ Localisation « Toujours autoriser » manquante" | *Settings › Apps › Portail Keo › Permissions › Location › Allow all the time*. |
| Nothing happens in the background | Disable battery optimisation / set the app to **Unrestricted**. |
| Call placed but gate stays closed | Your number is not authorised by the gate system. |
| Call drops without ringing | The app retries automatically (up to 3 attempts); check the **Journal** for "nouvel essai". |
| Tile shows no label | Expand the full shade, or enlarge the tile (Android 16). |
| "Mise à jour refusée : signature différente" | Uninstall the app, then install the latest APK manually. |

## Building from source

Requirements: JDK 17+ and the Android SDK (or Android Studio).

```bash
git clone https://github.com/JulesD29/PortailKeo.git
cd PortailKeo
./gradlew assembleDebug      # → app/build/outputs/apk/debug/
```

Or open the folder in Android Studio and click ▶ **Run**.

<details>
<summary><b>Project structure</b></summary>

```
app/src/main/java/fr/julesdupont/portail/
├── MainActivity.kt        Settings screen
├── MapPickerActivity.kt   Map picker (OpenStreetMap / osmdroid)
├── Zones.kt               Zone size rules
├── SetupActivity.kt       First-launch wizard (steps in SetupWizard.kt)
├── ShareActivity.kt       QR code of the current setup
├── ConfigShare.kt         QR link format, import / export
├── Dashboard.kt           Status card logic (state, next time window)
├── GeofenceManager.kt     Registers the gate and approach zones
├── GeofenceReceiver.kt    Handles zone entry / exit
├── PortalService.kt       Precise GPS on approach + countdown (foreground service)
├── AutoCall.kt            Single entry point for automatic calls
├── Rules.kt               Days, time window, pause, holidays, once a day
├── Holidays.kt            French public holidays (Easter computed)
├── CallHelper.kt          Places the call (TelecomManager)
├── PortalTileService.kt   "Appeler Garage" Quick Settings tile
├── CallActivity.kt        Invisible screen used by the tile and the shortcut
├── Updater.kt             Updates from GitHub Releases
├── InstallReceiver.kt     Update install result
├── RegisterReceiver.kt    Reboot, app update, periodic refresh
├── Notifier.kt            Notifications
├── Prefs.kt / Perms.kt    Settings and permissions
```
</details>

<details>
<summary><b>Releases and signing</b></summary>

- Every merge into `main` runs the **Release APK** workflow: it builds, signs and publishes a
  GitHub Release `v<N>`, which installed apps pick up as an update.
- The APK is signed with a fixed key stored in the repository **secrets**
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Without these
  secrets, no Release is published.
- Local builds use `signing/keystore.properties` if present, otherwise the debug key.
  The `signing/` folder is git-ignored and must never be committed.
</details>

<details>
<summary><b>Test version</b></summary>

Every push to `develop` publishes **Portail Keo (test)** (orange icon) as the `test` pre-release:
[PortailKeo-test.apk](https://github.com/JulesD29/PortailKeo/releases/download/test/PortailKeo-test.apk).
It installs next to the real app, has its own settings, updates from `develop` on demand
(**Rechercher une mise à jour**), and a **test tools**
section (simulate arrival / approach, reset today's call). See [CONTRIBUTING.md](CONTRIBUTING.md).
</details>

## Contributing

Work happens on `develop`; merging into `main` publishes a new version.
See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch workflow.
