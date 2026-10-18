# Pulse Music

A local-first Android music player built with Jetpack Compose, Material 3 and Media3 ExoPlayer.
No account, no cloud — it scans the audio already on your device and plays it.

## What's in 1.7.0

- **Apple Music-style redesign** — "Listen Now" home, bold large titles, alphabetical
  section headers and an A–Z scrubber across Songs/Albums/Artists, and a floating
  mini player with bottom progress and press feedback.
- **Full-text + fuzzy search** — an FTS table backs instant matching, with fuzzy
  fallback for typos and partial words.
- **WorkManager scan pipeline** — library scans run as survivable background work
  with observable progress instead of a foreground-only task.
- **Adaptive layouts** — bottom bar on phones, navigation rail on medium widths, and a
  two-pane layout with a persistent Now Playing pane on tablets and foldables.
- **Removals** — the Glance home-screen widget and the in-app GitHub update checker
  are gone; releases are handled outside the app.

## Current Status

| | |
|---|---|
| Version | 1.7.0 (versionCode 7) |
| Package | `com.suspended.music` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Compile SDK | 37 |
| Build tools | 36.0.0 |
| Java / JVM | 21 |
| Release APK name | `pulse-music-v<version>.apk` |

## Features

### Playback
- Media3 ExoPlayer engine with background playback through `MediaSessionService`
- Notification and lock-screen media controls
- Queue, shuffle and repeat
- Playback speed control, gapless playback and skip-silence
- Equalizer screen with a full audio-effect controller
- Hardware decoders for FLAC, ALAC and DSD, plus an FFmpeg software decoder for
  AAC, M4A, Opus and other formats ExoPlayer can't always sniff

### Library and search
- Songs, Albums, Artists, Playlists, Queue, Search and Now Playing screens
- Full-text search (Room FTS) with fuzzy matching for typos
- Smart playlists: Favorites and Recently Played
- Tag editor for title, artist, album, genre, lyrics and embedded artwork
  (backed by jaudiotagger)
- Local metadata persistence with Room, plus WorkManager-driven rescans
- Song actions: share, delete, info, add to playlist

### UI and personalization
- Apple Music-style visual language: Apple red accent, neutral system surfaces,
  grouped sections and a right-edge A–Z scrubber
- Light and dark themes, an AMOLED pure-black mode, and custom accent overrides
  (Apple hues by default)
- Edge-to-edge layout with animated navigation and mini-player transitions
- Adaptive: bottom bar → navigation rail → two-pane with persistent Now Playing

### Reliability and performance
- Lifecycle-aware state collection on the main screens
- Coil 3 image caching with memoized requests on playback surfaces
- Playback timing sanitization and seek/progress clamping
- Baseline profile scaffold at `app/src/main/baseline-prof.txt`

> **Note on paging:** Paging 3 is still part of the stack for Room-backed queries, but
> the Songs list intentionally loads the full sorted library. An A–Z scrubber has to
> resolve "where does M start", which a pager can't do for pages it hasn't loaded —
> and it avoids the recomposition churn that paging caused while scrolling.

## Tech Stack

| Component | Version |
|---|---|
| Android Gradle Plugin | 9.2.1 |
| Gradle wrapper | 9.4.1 |
| Kotlin | 2.4.10 |
| Compose BOM | 2026.06.01 |
| Media3 (ExoPlayer) | 1.11.0 |
| Navigation Compose | 2.9.8 |
| Hilt | 2.60.1 |
| Room | 2.8.4 |
| Coil | 3.5.0 |
| DataStore | 1.2.1 |
| Paging | 3.5.0 |
| WorkManager | 2.11.2 |
| KSP | 2.3.10 |
| Coroutines | 1.11.0 |
| Lottie | 6.7.1 |
| Accompanist | 0.36.0 |
| jaudiotagger | 3.0.1 |

Dependency versions are centralized in `gradle/libs.versions.toml`.

## Project Structure

```
app/
  src/main/
    java/com/suspended/music/
      data/
        local/        # Room entities, DAOs, migrations, FTS
        model/        # Song, Album, Artist, Playlist, Settings
        preferences/  # DataStore-backed settings
        repository/   # Media scanner + tag repository
        worker/       # WorkManager media-scan pipeline
      di/             # Hilt modules
      player/         # ExoPlayer wrapper, playback service, audio effects
      ui/
        components/   # Reusable Compose components (rows, mini player, sheets)
        navigation/   # Routes and nav host
        screens/      # Feature screens
        theme/        # Material 3 theme, colors, type, shapes
        utils/        # Adaptive layout, haptics, battery helpers
        viewmodel/    # MainViewModel and state handling
      util/           # Fuzzy search, error handling, perf logging
    res/              # Android resources
    baseline-prof.txt
  build.gradle.kts

gradle/
  libs.versions.toml  # Dependency version catalog
```

## Build and Run

### Prerequisites

- Android Studio (latest stable recommended)
- Android SDK 36 / 37 components installed
- JDK 21
- A physical device or emulator running Android 8.0+

### Windows (PowerShell)

```powershell
# Debug APK
.\gradlew assembleDebug

# Install on a connected device or emulator
.\gradlew installDebug

# Release APK (minified + resource-shrunk)
.\gradlew assembleRelease
```

### macOS / Linux

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew assembleRelease
```

Output lands in `app/build/outputs/apk/debug/pulse-music-v1.7.0.apk`.

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

## Runtime Permissions

Storage and media:
- `READ_MEDIA_AUDIO` (Android 13+)
- `READ_EXTERNAL_STORAGE` (maxSdkVersion 32)
- `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 29)
- `MANAGE_EXTERNAL_STORAGE` — broad library access for scanning and tag editing

Playback:
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `POST_NOTIFICATIONS`
- `WAKE_LOCK`
- `MODIFY_AUDIO_SETTINGS`
- `VIBRATE`

Network (album art and casting only):
- `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`

## Docs in This Repo

- [BUILD_AND_RUN_GUIDE.md](BUILD_AND_RUN_GUIDE.md)
- [ADVANCED_SEARCH_AND_HIBY_UI.md](ADVANCED_SEARCH_AND_HIBY_UI.md)
- [GESTURE_AND_ANIMATION_FEATURES.md](GESTURE_AND_ANIMATION_FEATURES.md)
- [OPTIMIZATIONS_SUMMARY.md](OPTIMIZATIONS_SUMMARY.md)
- [K2_PREDICTIVE_BACK_CHANGELOG.md](K2_PREDICTIVE_BACK_CHANGELOG.md)
- [REMAINING_WORK.md](REMAINING_WORK.md)
- [v1.5.0_CHANGELOG.md](v1.5.0_CHANGELOG.md)
- [v1.4.0_CHANGELOG.md](v1.4.0_CHANGELOG.md)
- [v1.3.0_CHANGELOG.md](v1.3.0_CHANGELOG.md)

The v1.2-era notes (`*_V1.2.md`, `VERSION_1.2_NOTES.md`, `RELEASE_NOTES_v1.2.0.md`)
are kept for history; the implementation has moved on considerably since.

## Troubleshooting

- If Gradle sync or a build goes stale after dependency changes:

```powershell
.\gradlew clean
.\gradlew assembleDebug --refresh-dependencies
```

- If KSP surfaces IDE exception noise with this stack, bumping KSP from 2.3.5 to
  2.3.6 has resolved it in this project before.
- If Gradle can't find the Android SDK, set `sdk.dir` in `local.properties` or
  export `ANDROID_HOME`.

## Contributing

1. Fork the repository.
2. Create a feature branch.
3. Add tests for behaviour changes where possible.
4. Run the compile, test and lint commands above.
5. Open a pull request with a clear change summary.
