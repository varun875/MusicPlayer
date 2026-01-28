# Quick Build & Run Guide - Music Player v1.2

## Prerequisites
- Android Studio (latest version recommended)
- JDK 17 or higher
- Android SDK with API 26-35
- Physical device or emulator running Android 8.0+

## Building the App

### Method 1: Android Studio GUI

1. **Open Project**:
   ```
   File → Open → Select MusicPlayer folder
   ```

2. **Sync Gradle**:
   - Wait for Gradle sync to complete
   - If prompted, click "Sync Now"

3. **Build**:
   ```
   Build → Make Project
   ```
   Or press: `Ctrl+F9` (Windows/Linux) or `Cmd+F9` (Mac)

4. **Run**:
   ```
   Run → Run 'app'
   ```
   Or press: `Shift+F10` (Windows/Linux) or `Ctrl+R` (Mac)

### Method 2: Command Line (Gradle)

1. **Navigate to project directory**:
   ```powershell
   cd C:\Users\Varun\Documents\GitHub\MusicPlayer
   ```

2. **Build Debug APK**:
   ```powershell
   .\gradlew assembleDebug
   ```
   
   Output: `app/build/outputs/apk/debug/app-debug.apk`

3. **Build Release APK** (requires signing):
   ```powershell
   .\gradlew assembleRelease
   ```
   
   Output: `app/build/outputs/apk/release/app-release.apk`

4. **Install on Connected Device**:
   ```powershell
   .\gradlew installDebug
   ```

5. **Build and Install in One Step**:
   ```powershell
   .\gradlew clean installDebug
   ```

### Method 3: Clean Build (if issues)

```powershell
# Clean previous build files
.\gradlew clean

# Build from scratch
.\gradlew assembleDebug

# Or build and install
.\gradlew clean installDebug
```

## Running the App

### On Physical Device

1. **Enable Developer Options**:
   - Settings → About Phone
   - Tap "Build Number" 7 times

2. **Enable USB Debugging**:
   - Settings → Developer Options
   - Enable "USB Debugging"

3. **Connect Device**:
   - Connect via USB
   - Accept "Allow USB Debugging" prompt

4. **Verify Connection**:
   ```powershell
   adb devices
   ```
   Should show your device

5. **Run from Android Studio**:
   - Click green "Run" button
   - Select your device from list

### On Emulator

1. **Create AVD** (if needed):
   ```
   Tools → Device Manager → Create Device
   ```
   
   Recommended specs:
   - Phone: Pixel 5 or similar
   - API Level: 33 (Android 13) or higher
   - RAM: 2048 MB minimum
   - Storage: 2 GB minimum

2. **Start Emulator**:
   - Click play button next to AVD in Device Manager
   - Wait for emulator to boot

3. **Run App**:
   - Click green "Run" button in Android Studio
   - App will install and launch automatically

## First Launch Setup

### Required Permissions

The app will request these permissions on first launch:

1. **Notifications** (Optional):
   - For playback notifications
   - Can be enabled later in settings

2. **Media Access** (Required):
   - Android 13+: READ_MEDIA_AUDIO
   - Android 12-: READ_EXTERNAL_STORAGE
   - Tap "Allow" to enable music playback

### Adding Test Music

If testing on emulator or device without music:

1. **Via ADB** (Recommended):
   ```powershell
   # Push single file
   adb push "C:\path\to\song.mp3" /sdcard/Music/
   
   # Push entire folder
   adb push "C:\path\to\music\folder" /sdcard/Music/
   ```

2. **Via File Manager**:
   - Copy files to device via USB
   - Place in Music folder
   - Restart app to scan

3. **Via Downloads**:
   - Download music files on device
   - Move to Music folder
   - Restart app

### Initial Scan

1. App will automatically scan for music files
2. First scan may take 10-30 seconds depending on library size
3. Progress shown in bottom sheet
4. Once complete, songs appear in Songs tab

## Verifying v1.2 Features

### Check Song Options Menu
1. Go to Songs tab
2. Look for 3-dot icon next to each song
3. Tap to open options menu
4. Should see: Delete, Share, Edit, Info, Add to Playlist

### Check Search Filters
1. Tap search icon (magnifying glass)
2. Look for "Filters" button
3. Tap to open filter sheet
4. Should see: Duration slider, Artist/Album/Genre dropdowns, Favorites toggle

### Check Animations
1. Navigate between tabs (Home, Songs, Albums)
2. Should see smooth fade + scale animations
3. Use back button to test pop animations

### Check Database Version
1. Android Studio → App Inspection → Database Inspector
2. Look for MusicDatabase
3. Version should show: 3
4. Song table should have 13 indices

## Troubleshooting

### Build Fails

**Issue**: Gradle sync failed
```
Solution:
1. File → Invalidate Caches → Invalidate and Restart
2. Build → Clean Project
3. Build → Rebuild Project
```

**Issue**: SDK not found
```
Solution:
1. File → Project Structure
2. SDK Location → Set Android SDK path
3. Usually: C:\Users\<username>\AppData\Local\Android\Sdk
```

**Issue**: Dependency resolution failed
```powershell
# Clear Gradle cache
cd C:\Users\Varun\.gradle
rmdir /s caches

# Re-sync in Android Studio
File → Sync Project with Gradle Files
```

### Runtime Issues

**Issue**: App crashes on launch
```
Check Logcat for:
- Permission errors → Grant required permissions
- Database errors → Clear app data and retry
- Native library errors → Check device architecture
```

**Issue**: No songs detected
```
Solution:
1. Verify music files are in /sdcard/Music/ or /sdcard/Download/
2. Check file formats (MP3, FLAC, M4A supported)
3. Grant media permissions
4. Force rescan: Settings → Rescan Library
```

**Issue**: Delete not working
```
Solution (Android 11+):
1. Settings → Apps → Music Player
2. Permissions → Files and Media
3. Select "Allow management of all files"
```

**Issue**: Slow performance
```
Solution:
1. Ensure device has sufficient RAM
2. Clear app cache: Settings → Apps → Music Player → Clear Cache
3. Reduce library size for testing
```

## Development Tips

### Hot Reload (Compose)

With app running:
1. Make UI changes in Compose code
2. Press: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)
3. UI updates without full rebuild

### Debugging

1. **Set Breakpoint**:
   - Click in gutter next to line number
   - Run in Debug mode: `Shift+F9`

2. **View Logs**:
   ```
   View → Tool Windows → Logcat
   Filter by: "com.suspended.music"
   ```

3. **Database Inspector**:
   ```
   View → Tool Windows → App Inspection → Database Inspector
   Browse tables, run queries, view data
   ```

### Testing Changes

1. **Quick Test**:
   ```powershell
   .\gradlew installDebug
   # Launch app manually
   ```

2. **With Logging**:
   ```powershell
   .\gradlew installDebug
   adb logcat | Select-String "MusicPlayer"
   ```

3. **Clean Install**:
   ```powershell
   adb uninstall com.suspended.music
   .\gradlew installDebug
   ```

## Build Variants

### Debug (Default)
- Includes debugging symbols
- Allows debugging
- Slightly larger APK
- Slower performance

```powershell
.\gradlew assembleDebug
```

### Release
- Optimized code
- ProGuard enabled
- Requires signing key
- Smaller APK, faster performance

```powershell
.\gradlew assembleRelease
```

## APK Location

After successful build:

**Debug APK**:
```
app/build/outputs/apk/debug/app-debug.apk
```

**Release APK**:
```
app/build/outputs/apk/release/app-release.apk
```

## Quick Commands Reference

```powershell
# Build only
.\gradlew assembleDebug

# Build and install
.\gradlew installDebug

# Clean build
.\gradlew clean assembleDebug

# Run tests
.\gradlew test

# Check dependencies
.\gradlew dependencies

# Generate APK + AAB
.\gradlew bundleRelease

# Uninstall from device
adb uninstall com.suspended.music

# View connected devices
adb devices

# View app logs
adb logcat | Select-String "com.suspended.music"

# Clear app data
adb shell pm clear com.suspended.music
```

## Performance Monitoring

### Check Build Time
```powershell
# With timing
.\gradlew assembleDebug --profile
# Results in: build/reports/profile/
```

### Check APK Size
```powershell
# Analyze APK
.\gradlew analyzeDebug
# Or manually check file size in:
# app/build/outputs/apk/debug/
```

### Memory Profiling
1. Run app in Android Studio
2. View → Tool Windows → Profiler
3. Click "+" → Select running app
4. Monitor memory, CPU, network

---

**Happy Building! 🚀**

For issues or questions, check:
- TESTING_GUIDE_V1.2.md
- IMPLEMENTATION_DETAILS_V1.2.md
- Android Studio Build Output
