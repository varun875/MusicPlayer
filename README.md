# Music Player v1.2 🎵

A modern, feature-rich Android music player built with Jetpack Compose, Material 3, and Media3 ExoPlayer.

## ✨ Key Features

### 🎵 Core Playback
- **High-quality audio playback** with Media3 ExoPlayer
- **Multiple format support**: MP3, FLAC, M4A, WAV, OGG, and more
- **Software decoder preference** for stability (especially FLAC)
- **Gapless playback** for seamless listening
- **Background playback** with notification controls
- **Samsung Now Bar support** for compatible devices

### 🎨 Modern UI
- **Material 3 Design** with dynamic color theming
- **Dark/Light modes** with custom theme colors
- **Smooth animations** with spring physics
- **Expressive color system** for vibrant UI
- **Bottom sheet patterns** for contextual actions
- **Responsive layouts** for all screen sizes

### 📁 Song Management
- **3-dot contextual menu** on every song
- **Delete songs** with confirmation dialog
- **Share songs** securely via FileProvider
- **Edit tags** (framework ready)
- **Song information** viewer
- **Add to playlists** (integration ready)

### 🔍 Advanced Search
- **Multi-criteria filtering**:
  - Text search (title, artist, album)
  - Duration range slider
  - Artist filter dropdown
  - Album filter dropdown
  - Genre filter dropdown
  - Favorites-only toggle
- **Real-time results** as you type
- **Indexed queries** for fast searching (10,000+ song libraries)

### ⚡ Performance Optimizations
- **Intelligent image caching** (25% memory, 100MB disk)
- **13 database indices** for lightning-fast queries
- **Optimized search performance** (up to 50x faster)
- **Memory-efficient** album art loading
- **Smooth 60 FPS animations**

### 🎛️ Additional Features
- **Equalizer** with presets
- **Favorites system** for quick access
- **Recent plays** tracking
- **Play count** statistics
- **Shuffle and repeat modes**
- **Sleep timer** (planned)

## 📱 Screenshots

_[Coming Soon]_

## 🚀 Getting Started

### Requirements
- Android 8.0 (API 26) or higher
- Recommended: Android 13+ for best experience

### Installation

1. **Download APK**:
   - Get the latest release from [Releases](../../releases)
   - Or build from source (see below)

2. **Install**:
   ```
   adb install app-release.apk
   ```
   Or transfer to device and install manually

3. **Grant Permissions**:
   - On first launch, grant media/storage permissions
   - For delete functionality (Android 11+), grant "Manage all files" permission

4. **Add Music**:
   - Place music files in `/sdcard/Music/` or `/sdcard/Download/`
   - App will auto-scan on launch
   - Supported formats: MP3, FLAC, M4A, WAV, OGG, OPUS, AAC

## 🛠️ Building from Source

### Prerequisites
- Android Studio (latest version)
- JDK 17+
- Android SDK (API 26-35)

### Steps

1. **Clone Repository**:
   ```bash
   git clone https://github.com/yourusername/MusicPlayer.git
   cd MusicPlayer
   ```

2. **Open in Android Studio**:
   ```
   File → Open → Select MusicPlayer folder
   ```

3. **Sync Gradle**:
   - Wait for automatic Gradle sync
   - Or: File → Sync Project with Gradle Files

4. **Build & Run**:
   ```bash
   # Debug build
   ./gradlew assembleDebug
   
   # Release build (requires signing)
   ./gradlew assembleRelease
   
   # Build and install
   ./gradlew installDebug
   ```

For detailed build instructions, see [BUILD_AND_RUN_GUIDE.md](BUILD_AND_RUN_GUIDE.md)

## 📚 Documentation

- **[VERSION_1.2_NOTES.md](VERSION_1.2_NOTES.md)** - Release notes and changelog
- **[COMPLETE_FEATURE_SUMMARY_V1.2.md](COMPLETE_FEATURE_SUMMARY_V1.2.md)** - Comprehensive feature overview
- **[IMPLEMENTATION_DETAILS_V1.2.md](IMPLEMENTATION_DETAILS_V1.2.md)** - Technical implementation guide
- **[TESTING_GUIDE_V1.2.md](TESTING_GUIDE_V1.2.md)** - Testing procedures and checklist
- **[BUILD_AND_RUN_GUIDE.md](BUILD_AND_RUN_GUIDE.md)** - Build and deployment guide

## 🏗️ Architecture

### Tech Stack

- **UI Framework**: Jetpack Compose
- **Design System**: Material 3 (Expressive theme)
- **Audio Engine**: Media3 ExoPlayer 1.9.0
- **Database**: Room 2.7.0-alpha12 (with 13 optimized indices)
- **Image Loading**: Coil 2.7.0 (with memory & disk caching)
- **Dependency Injection**: Hilt
- **Build System**: Gradle with Kotlin DSL
- **Language**: Kotlin 2.1.0

### Architecture Pattern

- **MVVM** (Model-View-ViewModel)
- **Repository Pattern** for data access
- **Unidirectional Data Flow**
- **Reactive Programming** with Kotlin Flow
- **Dependency Injection** with Hilt
- **Single Activity** architecture with Compose Navigation

### Project Structure

```
app/
├── src/main/
│   ├── java/com/suspended/music/
│   │   ├── data/
│   │   │   ├── local/          # Room database, DAOs, migrations
│   │   │   ├── model/          # Data models (Song, Album, Artist, etc.)
│   │   │   └── repository/     # Repository implementations
│   │   ├── di/                 # Hilt dependency injection modules
│   │   ├── player/             # Media3 ExoPlayer & PlaybackService
│   │   ├── ui/
│   │   │   ├── components/     # Reusable UI components
│   │   │   ├── navigation/     # Navigation setup
│   │   │   ├── screens/        # App screens (Composables)
│   │   │   ├── theme/          # Material 3 theming
│   │   │   └── viewmodel/      # ViewModels
│   │   ├── util/               # Utilities and extensions
│   │   └── MainActivity.kt     # Single activity entry point
│   ├── res/
│   │   ├── xml/                # FileProvider paths, preferences
│   │   └── values/             # Strings, colors, themes
│   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

## 🔐 Permissions

### Required
- `READ_MEDIA_AUDIO` (Android 13+) - Access audio files
- `READ_EXTERNAL_STORAGE` (Android 6-12) - Access storage
- `FOREGROUND_SERVICE` - Background playback
- `POST_NOTIFICATIONS` - Playback notifications

### Optional
- `WRITE_EXTERNAL_STORAGE` (Android 6-10) - Delete songs
- `MANAGE_EXTERNAL_STORAGE` (Android 11+) - Delete songs

## 🎯 Roadmap

### v1.3 (Next Release)
- [ ] Complete edit tags functionality
- [ ] Full song info dialog implementation
- [ ] Playlist management integration
- [ ] Batch operations (multi-select delete/share)
- [ ] Search suggestions and history
- [ ] Widget support

### v1.4 (Future)
- [ ] Shared element transitions for album art
- [ ] Gesture controls (swipe to queue, swipe to favorite)
- [ ] Advanced sorting options
- [ ] Export/import playlists
- [ ] Lyrics support
- [ ] Audio effects (reverb, bass boost)

### v2.0 (Vision)
- [ ] Cloud sync (Google Drive, Dropbox)
- [ ] AI-powered smart playlists
- [ ] Music discovery and recommendations
- [ ] Social features (share playlists)
- [ ] Cross-device synchronization
- [ ] Podcast support

## 🤝 Contributing

Contributions are welcome! Here's how:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Commit changes**: `git commit -m 'Add amazing feature'`
4. **Push to branch**: `git push origin feature/amazing-feature`
5. **Open a Pull Request**

### Guidelines
- Follow existing code style (Kotlin coding conventions)
- Write meaningful commit messages
- Add tests for new features
- Update documentation as needed
- Test on multiple Android versions

## 🐛 Bug Reports

Found a bug? Please open an issue with:
- Android version and device model
- Steps to reproduce
- Expected vs actual behavior
- Logcat output (if available)
- Screenshots (if UI-related)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

### Libraries
- [Media3 ExoPlayer](https://github.com/androidx/media) - Audio playback
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Room](https://developer.android.com/training/data-storage/room) - Local database
- [Coil](https://coil-kt.github.io/coil/) - Image loading
- [Hilt](https://dagger.dev/hilt/) - Dependency injection

### Inspiration
- Material Design 3 guidelines
- Modern Android development best practices
- Community feedback and suggestions

## 📞 Contact

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)

## 🌟 Show Your Support

If you like this project, please consider:
- ⭐ Starring the repository
- 🍴 Forking for your own modifications
- 📢 Sharing with others
- 🐛 Reporting bugs
- 💡 Suggesting features

---

**Built with ❤️ using Jetpack Compose**

*Version 1.2 | Released 2024 | Android 8.0+*