# v1.3.0 - Remaining Work & Future Enhancements

## ✅ COMPLETED (Session 2)
- Fixed build errors (MainActivity predictive back implementation)
- All Material 3 + Kotlin best practices implemented
- Build status: **PASSING ✅**

---

## 🔄 CURRENTLY IN PROGRESS

### 1. **Favorites Playlist** (UI ready, needs persistence)
   - Location: Home/Music library
   - Status: Favorite button exists in SongItem
   - **What to do**: 
     - Create `FavoritesPlaylist` data class in Room
     - Add `getFavoritesPlaylist()` DAO method
     - Create dedicated FavoritesScreen with sync to playlist

### 2. **Tag Editor Artwork Picker**
   - Location: TagEditorScreen.kt
   - Status: Basic metadata editor exists (title/artist/album/year/genre)
   - **What to do**:
     - Add image picker (MediaStore or file picker)
     - Preview album artwork before save
     - Integrate with Tag repository for embedded artwork
     - Show current artwork thumbnail

### 3. **Batch Editing Support**
   - Current: Single song metadata editing
   - **What to do**:
     - Multi-select in SongsScreen
     - Batch tag editor for common fields
     - Apply same genre/album to multiple songs
     - Bulk favorite/unfavorite

### 4. **Layout Customization**
   - Current: Fixed Material 3 layout
   - **What to do**:
     - Compact vs. Standard song list modes
     - Grid vs. List album view toggle
     - Adjustable mini player height
     - Custom bottom nav ordering
     - Save layout preferences to SharedPreferences

---

## 📋 AVAILABLE ENHANCEMENTS (Can implement now)

### A. **Search & Filtering**
   - ✅ Flow debounce (300ms) already implemented
   - [ ] Add advanced filters UI (artist/album/genre/duration/year)
   - [ ] Save search history
   - [ ] Search by metadata (rating, date added)
   - [ ] Regex search support

### B. **Now Playing Screen Polish**
   - Current: Basic playback controls
   - [ ] Apply shared element transitions (album art scale animation)
   - [ ] Lyrics display integration
   - [ ] Queue management UI
   - [ ] Playback speed visualizer
   - [ ] Waveform progress indicator

### C. **Album Details Enhancement**
   - Current: Shows album songs list
   - [ ] Album artwork with parallax scroll
   - [ ] Artist info section
   - [ ] Play/shuffle/queue controls
   - [ ] Track ratings
   - [ ] Sort by track/added date

### D. **Equalizer Features**
   - Current: 10-band EQ with presets
   - [ ] Save custom presets
   - [ ] Preset favorites/library
   - [ ] Real-time audio visualization
   - [ ] Bass boost/Virtualizer animation indicators

### E. **Library Organization**
   - [ ] Playlists CRUD (create/edit/delete done, needs UI refinement)
   - [ ] Folder browsing mode
   - [ ] Recently added section
   - [ ] Most played section
   - [ ] Smart playlists (by genre, year, rating)

### F. **Notifications & Widget**
   - ✅ Widget redesigned with playback controls
   - [ ] Lock screen media control (Android 13+)
   - [ ] Notification custom actions
   - [ ] Floating mini player
   - [ ] Widget size variants

### G. **Performance Optimizations**
   - ✅ Coil disk cache (256MB configured)
   - [ ] LazyColumn key optimization in song lists
   - [ ] Image loading placeholder/skeleton
   - [ ] Database query pagination (currently loads all)
   - [ ] Memory profiling & optimization

### H. **User Experience**
   - [ ] Undo/redo for metadata edits
   - [ ] Snackbar feedback for actions
   - [ ] Haptic feedback on interactions
   - [ ] Animated empty states
   - [ ] Swipe gestures (left = add to queue, right = favorite)

### I. **Scrobbling & Integrations**
   - Current: LastFM & ListenBrainz placeholders
   - [ ] Implement actual scrobbling API calls
   - [ ] Setup OAuth for authentication
   - [ ] Sync play count
   - [ ] Import profile data

### J. **Advanced Playback**
   - Current: Basic play/pause/skip
   - [ ] Bookmarks (remember position in long tracks)
   - [ ] Sleep timer
   - [ ] Crossfade between tracks
   - [ ] Gapless playback validation
   - [ ] Audio normalization (volume leveling)

---

## 🎯 RECOMMENDED PRIORITY ORDER

### **Phase 1: Core Features (High Impact)**
1. ✅ Fix build errors → **DONE**
2. **Tag Editor Artwork Picker** (15 min) - Small but valuable
3. **Batch Editing** (30 min) - Common user need
4. **Search Advanced Filters** (20 min) - Better UX

### **Phase 2: Polish (Medium Impact)**
5. **Layout Customization** (30 min) - Personalization
6. **Album Details Enhancement** (25 min) - Richer info
7. **Shared Transitions** (apply existing, 15 min) - Premium feel
8. **Favorites Playlist Persistence** (20 min) - Completes feature

### **Phase 3: Premium Features (High Polish)**
9. **Now Playing Screen Polish** (30 min) - Central UX
10. **Swipe Gestures** (20 min) - Modern UX
11. **Library Smart Sections** (25 min) - Discovery

### **Phase 4: Advanced (Performance & Integration)**
12. **Database Pagination** (30 min) - Handles large libraries
13. **Scrobbling Implementation** (30 min) - Integration
14. **Lock Screen Controls** (20 min) - Android 13+ feature
15. **Audio Visualizer** (30 min) - Eye candy

---

## 📊 Feature Completion Status

| Feature | Status | Effort | Impact | Priority |
|---------|--------|--------|--------|----------|
| AMOLED Black Mode | ✅ Complete | - | High | - |
| Widget with Controls | ✅ Complete | - | High | - |
| Material 3 Theme | ✅ Complete | - | High | - |
| Sealed UI States | ✅ Complete | - | Medium | - |
| Inline ID Classes | ✅ Complete | - | Medium | - |
| Flow Debounce Search | ✅ Complete | - | Medium | - |
| Coil Disk Cache | ✅ Complete | - | High | - |
| Predictive Back | ✅ Complete | - | Low | - |
| Shared Transitions | ✅ Ready | 5min | Medium | High |
| Dynamic Colors | ✅ Validated | - | High | - |
| Tag Editor Artwork | 50% | 15min | High | **1** |
| Batch Editing | 0% | 30min | High | **2** |
| Advanced Search Filters | 0% | 20min | High | **3** |
| Layout Customization | 0% | 30min | Medium | **4** |
| Favorites Persistence | 0% | 20min | High | **5** |
| Album Details Polish | 30% | 25min | Medium | **6** |
| Smart Playlists | 0% | 40min | Medium | **7** |
| Scrobbling API | 0% | 30min | Low | **8** |
| Lock Screen Controls | 0% | 20min | Medium | **9** |
| Sleep Timer | 0% | 15min | Low | **10** |

---

## 🚀 Quick Wins (5-15 min each)

1. **Apply Shared Transitions** to NowPlayingScreen (use existing `SharedTransitionSpecs`)
2. **Add Loading Skeleton** for album artwork (show placeholder while loading)
3. **Haptic Feedback** on play button tap
4. **Snackbar Messages** for copy/add to playlist/favorite actions
5. **Animate Empty States** with Compose animation
6. **Add Swipe Gestures** (Modifier.swipe* for favorite/queue)

---

## 🏗️ Architecture Notes

- **Database**: Room with indices, DAO methods exist
- **State Management**: Sealed interfaces + Flow operators (ready)
- **Caching**: Coil with 256MB disk cache (ready)
- **Playback**: Media3 ExoPlayer + MediaSession (stable)
- **UI**: Jetpack Compose Material 3 (complete)
- **DI**: Hilt (fully configured)

---

## 📌 Next Session Recommendations

1. Start with **Tag Editor Artwork Picker** (quick win, high value)
2. Add **Batch Editing UI** (checkbox multi-select + bulk operations)
3. Implement **Advanced Search Filters** (date range, rating, etc.)
4. Save **Layout Preferences** to SharedPreferences
5. Polish **Now Playing Screen** with existing SharedTransitions

**Estimated time for all: ~2-3 hours for professional quality implementation**

