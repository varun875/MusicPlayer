# Quick Build & Run Guide — Pulse Music v1.7.0

A short, practical guide to getting Pulse Music built and running. For the full
project overview, see [README.md](README.md).

## Prerequisites

- Android Studio (latest stable recommended)
- **JDK 21** (source/target and Kotlin JVM target are both 21)
- Android SDK with **platform 37** and **build-tools 36.0.0** installed
- A physical device or emulator on **Android 8.0+** (minSdk 26)

> The Gradle wrapper pins Gradle 9.4.1, so the first build downloads ~130 MB of
> Gradle plus AGP/Kotlin artifacts. Expect the first run to be slow; later builds
> are incremental.

## Building the App

### Method 1: Android Studio GUI

1. **Open the project**
   ```
   File → Open → select the MusicPlayer folder
   ```
2. **Sync Gradle** — wait for sync, click "Sync Now" if prompted
3. **Build** — `Build → Make Project` (`Ctrl+F9` / `Cmd+F9`)
4. **Run** — `Run → Run 'app'` (`Shift+F10` / `Ctrl+R`)

### Method 2: Command Line (Gradle)

```powershell
cd C:\Users\Varun\Documents\GitHub\MusicPlayer

# Debug APK
.\gradlew assembleDebug
# → app/build/outputs/apk/debug/pulse-music-v1.7.0.apk

# Build + install on a connected device
.\gradlew installDebug

# Release APK (minified + resource-shrunk)
.\gradlew assembleRelease
# → app/build/outputs/apk/release/pulse-music-v1.7.0.apk
```

The APK name is derived from `versionName` in `app/build.gradle.kts`
(currently `1.7.0`, `versionCode 7`), so it changes with the version.

> **Release builds are unsigned** — there is no `signingConfig` in
> `build.gradle.kts`. `assembleRelease` produces an APK you must sign before it
> will install. For local performance testing, install a debug build or add a
> signing config yourself.

### Method 3: Clean build (if something is stale)

```powershell
.\gradlew clean
.\gradlew assembleDebug
# or, lighter: refresh just the dependencies
.\gradlew assembleDebug --refresh-dependencies
```

## Running the App

### On a physical device

1. Enable **Developer options** — Settings → About phone → tap *Build number* 7×
2. Enable **USB debugging** in Developer options
3. Connect via USB and accept the *Allow USB debugging* prompt
4. Verify: `adb devices` should list your device
5. Run from Android Studio, or `.\gradlew installDebug`

**Wireless (no cable):** enable *Wireless debugging*, then
`adb pair <ip>:<pair-port> <code>` followed by
`adb connect <ip>:<port>` using the port shown on the Wireless debugging screen.

### On an emulator

1. Device Manager → Create Device — Pixel-class phone, **API 35**, 2 GB RAM+
2. Start the emulator and wait for boot
3. `Run 'app'` — it installs and launches automatically

## First Launch Setup

### Permissions

1. **Media access** (required)
   - Android 13+: `READ_MEDIA_AUDIO`
   - Android 11–12: the app opens the system **"All files access"** screen so a
     full library scan and tag editing can work
   - Android 10 and below: `READ_EXTERNAL_STORAGE`
2. **Notifications** (optional) — needed for playback controls in the shade

### Adding test music

```powershell
adb push "C:\path\to\song.mp3" /sdcard/Music/
adb push "C:\path\to\music\folder" /sdcard/Music/
```

You can also copy files over USB or download them on-device. Keep them in
`Music/` or `Download/`.

### Initial scan

Scans run through WorkManager, so they survive leaving the app. Progress is
observable, and songs appear in the Songs tab as they're indexed. Larger
libraries take longer — a few thousand tracks is typically well under a minute.

## Verifying 1.7.0

### Listen Now (Home)
- First tab should read **Listen Now** with a featured card and a Recently Played rail
- The red shuffle button starts playback immediately

### Alphabetical library
- Open **Songs** — rows show artwork, title and artist, with bold letter headers
  that stick to the top while scrolling
- Drag the **A–Z** strip on the right edge to jump to a letter
- Change the sort via the ⇅ action; alphabetical sorts (Title / Artist / Album)
  get headers, numeric sorts (Date, Duration, Size, Play count) don't

### Search
- Search matches instantly via the FTS index and still finds near-misses for typos
- Results show artwork thumbnails

### Mini player
- Play something, then leave Now Playing — the mini player floats above the nav bar
  with the progress line along its bottom edge and a subtle press-scale on tap

### Adaptive layout
- Narrow window: bottom navigation bar
- Medium width: navigation rail
- Wide / tablet: two-pane with a persistent Now Playing pane

## Verification Commands

```powershell
# Kotlin compile check
.\gradlew :app:compileDebugKotlin

# Unit tests
.\gradlew testDebugUnitTest

# Instrumented tests (needs a connected device or emulator)
.\gradlew connectedDebugAndroidTest

# Lint
.\gradlew lintDebug
```

## Troubleshooting

**SDK not found**
```
Set sdk.dir in local.properties (gradle reads it), or export ANDROID_HOME.

  local.properties:
  sdk.dir=C\:\\Android\\Sdk

Alternatively: File → Project Structure → SDK Location, or export
ANDROID_HOME / ANDROID_SDK_ROOT and reopen the terminal so PATH refreshes.
```

**Gradle sync failed**
```
1. File → Invalidate Caches → Invalidate and Restart
2. Build → Clean Project, then Build → Rebuild Project
3. If dependencies are stale: .\gradlew assembleDebug --refresh-dependencies
```

**KSP exception noise in the IDE**
```
Bumping KSP (see gradle/libs.versions.toml — currently 2.3.10) has resolved this
before with this stack. Keep KSP aligned with the Kotlin version.
```

**App crashes on launch**
```
Check Logcat (filter com.suspended.music) for:
- Permission errors      → grant media + notification permissions
- Database errors        → clear app data and rescan
- Native library errors  → check the device ABI (arm64-v8a expected)
```

**No songs detected**
```
1. Confirm files are in /sdcard/Music/ or /sdcard/Download/
2. Check the format is supported (MP3, FLAC, M4A, AAC, Opus, OGG, WAV, ALAC, DSD…)
3. Grant media permissions
4. Rescan: Settings → Rescan library
```

**Delete not working (Android 11+)**
```
Settings → Apps → Pulse Music → Permissions → Files and Media
→ "Allow management of all files"
```

**Performance feels poor**
```
You are almost certainly on a debug build: no R8/minification, plus Compose debug
overhead. Test performance with a release build before chasing anything. Also
check that the library isn't mid-scan.
```

## Development Tips

### Applying UI changes

Compose Live Edit / **Apply Changes** (`Ctrl+F10`) pushes small UI edits without a
full reinstall. For dependency, Hilt or schema changes, do a full
`.\gradlew installDebug`.

### Debugging

- **Breakpoints** — click the gutter, run with `Shift+F9` (Debug)
- **Logcat** — View → Tool Windows → Logcat, filter `com.suspended.music`
- **Database Inspector** — View → Tool Windows → App Inspection → Database Inspector

### Testing changes

```powershell
# Install the latest build
.\gradlew installDebug

# Clean install (also drops app data)
adb uninstall com.suspended.music
.\gradlew installDebug

# Watch this app's logs
adb logcat | Select-String "com.suspended.music"
```

## Build Variants

| Variant | Minified | Debuggable | Notes |
|---|---|---|---|
| `debug` | No | Yes | Faster iteration, slower runtime — use for UI work |
| `release` | Yes (`isMinifyEnabled`, `isShrinkResources`) | No | Unsigned; use for real performance testing |

## APK Location

```
app/build/outputs/apk/debug/pulse-music-v1.7.0.apk
app/build/outputs/apk/release/pulse-music-v1.7.0.apk   (unsigned)
```

## Quick Commands Reference

```powershell
# Build
.\gradlew assembleDebug
.\gradlew assembleRelease
.\gradlew clean assembleDebug

# Install / uninstall
.\gradlew installDebug
adb uninstall com.suspended.music

# Verify
.\gradlew :app:compileDebugKotlin
.\gradlew testDebugUnitTest
.\gradlew lintDebug

# Devices and logs
adb devices
adb logcat | Select-String "com.suspended.music"
adb shell pm clear com.suspended.music

# Build timing report → build/reports/profile/
.\gradlew assembleDebug --profile
```

To inspect APK size and contents, use **Build → Analyze APK…** in Android Studio,
or check the file size in `app/build/outputs/apk/`.

---

**Happy building!**

Further reading:
- [README.md](README.md) — project overview, features, tech stack
- [GESTURE_AND_ANIMATION_FEATURES.md](GESTURE_AND_ANIMATION_FEATURES.md)
- [ADVANCED_SEARCH_AND_HIBY_UI.md](ADVANCED_SEARCH_AND_HIBY_UI.md)
- [OPTIMIZATIONS_SUMMARY.md](OPTIMIZATIONS_SUMMARY.md)
