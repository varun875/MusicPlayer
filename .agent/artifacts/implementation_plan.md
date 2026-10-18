# 🎵 Premium Music Player - Implementation Plan

## Project Overview
A premium Android music player that surpasses Hiby and Samsung Music with Material You design, Hi-Res audio support, and advanced features.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3 (Material You)
- **Audio Engine**: Media3 (ExoPlayer) with FFmpeg extension for Hi-Res codecs
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Image Loading**: Coil
- **Navigation**: Compose Navigation

---

## Phase 1: Core Foundation (Week 1-2)
### 1.1 Project Setup
- [x] Update dependencies (Media3, Hilt, Room, Coil, Navigation)
- [x] Set up project architecture (data, domain, presentation layers)
- [x] Configure permissions (storage, media, foreground service)
- [x] Set up Material You theming with dynamic colors

### 1.2 Audio Playback Engine
- [x] Integrate Media3 (ExoPlayer) with Media Session
- [x] Add FFmpeg extension for DSD, MQA, ALAC support
- [x] Implement MediaBrowserService for system integration
- [x] Configure audio focus handling
- [x] Implement Now Playing notification (Now Bar support)

### 1.3 Media Library Scanner
- [x] Scan device for audio files
- [x] Extract metadata (title, artist, album, duration, artwork)
- [x] Store in Room database
- [x] Support folder-based browsing

---

## Phase 2: Core UI (Week 2-3) ✅
### 2.1 Home Screen
- [x] Recently played section
- [x] Quick access tiles (Shuffle, Favorites)
- [x] Album/Artist highlights

### 2.2 Library Screens
- [x] Songs list with sorting/filtering
- [x] Advanced search filters (duration, artist, album, genre, favorites)
- [x] Song options menu (delete, share, info)
- [x] Albums grid with artwork
- [x] Artists list with avatars
- [x] Playlists management
- [x] **Update Libraries**:
  - [x] Hilt to 2.55, Coil to 3.3.0.
  - [x] Kotlin to 2.1.10, Core KTX to 1.15.0.
  - [x] Compose BOM to 2026.01.00, Lifecycle to 2.10.0.
  - [x] Coroutines to 1.10.1, DataStore to 1.2.0, Media3 to 1.9.1, Paging to 3.3.6.
  - [x] Fix Gradle build files and resolve compatibility issues.
  - [x] Verify successful build (locally and/or ready for Studio).

## Phase 8: Quality Assurance & Optimization (v1.4-v1.5) ✅
- [x] Resolve 95+ lint and API compatibility issues
- [x] Locale, lifecycle, and permission warning cleanup
- [x] Deferred startup scanning for faster perceived launch
- [x] Songs screen performance refactor (Paging-backed rendering)
- [x] Transition timing overhaul for smoother navigation
- [x] Now Playing recomposition and image request optimizations
- [x] Settings scrolling/rendering optimization
- [x] Playback polling cadence tuning (500ms -> 650ms)
- [x] Baseline profile scaffolding (profile installer + baseline-prof.txt)
- [ ] Performance profiling: memory and startup benchmark pass (Macrobenchmark + traces)
- [x] Folder browser

### 2.3 Now Playing Screen
- [x] Large album artwork with animations
- [x] Material 3 rounded containers design
- [x] Playback controls (play/pause, seek, skip)
- [x] Material 3 expressive slider with spring animations
- [x] Progress bar with drag support
- [x] Queue management
- [x] Song information dialog
- [ ] Lyrics display (synced)

### 2.4 Mini Player
- [x] Collapsible bottom bar
- [x] Swipe gestures
- [x] Quick controls

---

## Phase 3: Audio Features (Week 3-4) ✅
### 3.1 Equalizer
- [x] 10-band EQ
- [x] Presets (Rock, Pop, Jazz, Classical, etc.)
- [x] Custom presets
- [x] Bass boost slider
- [x] Virtualizer

### 3.2 Audio Effects
- [ ] Reverb
- [ ] 3D/Spatial audio
- [ ] Playback speed control
- [ ] Pitch adjustment

### 3.3 Advanced Playback
- [x] Gapless playback
- [x] FLAC software decoder with hardware acceleration
- [ ] Crossfade (0-12 seconds)
- [ ] Replay Gain (track/album mode)
- [x] Hi-Res audio output support

---

## Phase 4: Smart Features (Week 4-5) ✅
### 4.1 Smart Playlists
- [x] Auto-generated (Favorites)
- [ ] Most Played, Recently Added rules
- [ ] Rule-based playlists

### 4.2 Lyrics
- [x] Embedded lyrics extraction
- [x] Lyrics editor
- [ ] Online lyrics fetch
- [ ] Synced lyrics with karaoke mode

### 4.3 Tag Editor
- [x] Edit title, artist, album, genre
- [x] Album artwork picker
- [x] Lyrics editing
- [ ] Batch editing

---

## Phase 5: Theming & Customization (Week 5-6) ✅
### 5.1 Material You
- [x] Dynamic color extraction from album art
- [x] Light/Dark/System theme modes
- [x] Material 3 expressive design system
- [x] Rounded containers with elevation
- [ ] AMOLED black mode (pure black #000000 for dark theme)

### 5.2 Custom Themes
- [ ] Accent color picker
- [ ] Font selection
- [x] Layout customization (grid sizes, list density)
- [x] View modes (Grid/List)

### 5.3 Visualizations
- [ ] Audio visualizer (bars, waves, circular)
- [x] Album art animations (spring-based, smooth transitions)
- [x] Background gradient effects with dynamic blur

---

## Phase 6: Connectivity (Week 6-7)
### 6.1 Casting
- [ ] Chromecast support
- [ ] DLNA/UPnP renderer discovery
- [ ] Local network streaming

### 6.2 Android Auto
- [ ] Android Auto integration
- [ ] Voice commands support

### 6.3 Widgets
- [x] Home screen widgets (progress bar, playback controls)
- [ ] Lock screen controls (Now Bar integration)

---

## Supported Audio Formats
| Format | Extension | Bit Depth | Sample Rate |
|--------|-----------|-----------|-------------|
| MP3 | .mp3 | 16-bit | 44.1/48 kHz |
| AAC | .aac, .m4a | 16-bit | Up to 96 kHz |
| FLAC | .flac | Up to 24-bit | Up to 192 kHz |
| WAV | .wav | Up to 32-bit | Up to 384 kHz |
| ALAC | .m4a | Up to 24-bit | Up to 192 kHz |
| DSD | .dsf, .dff | 1-bit | 2.8/5.6/11.2 MHz |
| MQA | .mqa.flac | Unfolds | Up to 384 kHz |

---

## Architecture Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────────┐ │
│  │  Home   │  │ Library │  │  Now    │  │    Settings     │ │
│  │ Screen  │  │ Screens │  │ Playing │  │     Screen      │ │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────────┬────────┘ │
│       └────────────┴────────────┴────────────────┘          │
│                           │                                  │
│                    ┌──────┴──────┐                          │
│                    │  ViewModels │                          │
│                    └──────┬──────┘                          │
└───────────────────────────┼──────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────┐
│                      Domain Layer                            │
│  ┌─────────────┐  ┌───────┴───────┐  ┌─────────────────────┐│
│  │  Use Cases  │  │  Repositories │  │      Entities       ││
│  │             │  │  (Interfaces) │  │                     ││
│  └─────────────┘  └───────────────┘  └─────────────────────┘│
└───────────────────────────┬──────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────┐
│                       Data Layer                             │
│  ┌─────────────┐  ┌───────┴───────┐  ┌─────────────────────┐│
│  │    Room     │  │ Media Scanner │  │   Preferences       ││
│  │  Database   │  │               │  │   DataStore         ││
│  └─────────────┘  └───────────────┘  └─────────────────────┘│
└───────────────────────────┬──────────────────────────────────┘
                            │
┌───────────────────────────┼──────────────────────────────────┐
│                    Playback Layer                            │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                  Media3 (ExoPlayer)                     ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ ││
│  │  │  Audio   │  │  Media   │  │   EQ &   │  │  FFmpeg │ ││
│  │  │  Focus   │  │  Session │  │  Effects │  │Extension│ ││
│  │  └──────────┘  └──────────┘  └──────────┘  └─────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
```

---


## Current Status: v1.5 - Performance and Motion Overhaul Completed
**Delivered in v1.4-v1.5:**
- [x] **Stability baseline**: 95+ lint/error cleanup and API-level compatibility hardening.
- [x] **Startup smoothness**: Removed unconditional startup scan; now scan only when library is empty with deferred trigger.
- [x] **Playback responsiveness**: Faster track switching through in-memory queue resolution and IO-dispatched play count updates.
- [x] **Songs performance**: Paging-backed rendering path and jank reduction for large libraries.
- [x] **Navigation speed**: Faster, lighter transitions across core routes (home/library/settings/queue/now playing).
- [x] **Now Playing optimization**: Reduced high-frequency recomposition cost and memoized heavy image/palette work.
- [x] **Settings optimization**: Reduced nested-scroll and per-frame parsing overhead.
- [x] **UX fixes**: Wired Add to Playlist action in Now Playing, removed dead actions, improved playlist flows.
- [x] **Update infrastructure**: GitHub API release parsing improved for APK asset targeting.
- [x] **Build readiness**: Debug APK successfully produced (`pulse-music-v1.5.0.apk`).

**Next Priority (v1.6):**
- Performance validation and benchmarking (startup, frame pacing, memory).
- Feature-completion of deferred audio and connectivity items.
- Regression testing across large libraries and low-memory devices.

### Next Steps (v1.6 Priorities):
- [ ] **Performance Profiling**: Macrobenchmark startup/frame metrics and memory traces.
- [ ] **Online Lyrics**: Fetch lyrics from Genius/Musixmatch APIs.
- [ ] **Crossfade**: Smooth transitions between tracks (0-12 seconds).
- [ ] **Smart Playlists**: Most Played and Recently Added auto-rules.
- [ ] **Audio Effects**: Reverb and Spatial Audio implementation.
- [ ] **Replay Gain**: Track/album normalization modes.
- [ ] **Chromecast**: Casting support.
- [ ] **Android Auto**: Media browsing and transport controls.
