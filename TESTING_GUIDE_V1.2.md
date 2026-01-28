# Testing Guide for v1.2 Features

## Prerequisites
Before testing, ensure:
1. App is built and installed on device/emulator
2. Android 11+ for full delete functionality
3. Music files available on device storage

## Feature Testing Checklist

### ✅ Song Options Menu (3-Dot Button)

#### Test Delete Functionality
1. Navigate to **Songs** tab
2. Tap the 3-dot menu icon on any song
3. Select **"Delete"** from the bottom sheet
4. Confirm deletion in the dialog
5. **Expected**: Song removed from list and filesystem

**Required Permissions**:
- On first delete attempt, app will request:
  - WRITE_EXTERNAL_STORAGE (Android 10-)
  - MANAGE_EXTERNAL_STORAGE (Android 11+)
- Grant permissions when prompted

#### Test Share Functionality
1. Tap 3-dot menu on a song
2. Select **"Share"**
3. Choose a sharing target (Messages, Email, etc.)
4. **Expected**: Song file shared via chosen app

#### Test Other Options
- **Edit Tags**: Currently navigates (needs full implementation)
- **Song Info**: Opens info dialog (basic implementation)
- **Add to Playlist**: Shows placeholder (needs full implementation)

### ✅ Advanced Search & Filters

#### Test Basic Search
1. Navigate to **Songs** tab
2. Tap search icon (magnifying glass)
3. Type a song name
4. **Expected**: Real-time filtered results

#### Test Advanced Filters
1. While in search mode, tap **"Filters"** button
2. **Test Duration Range**:
   - Adjust the range slider (0-10 minutes)
   - Apply filter
   - **Expected**: Only songs within duration range shown

3. **Test Artist Filter**:
   - Select an artist from dropdown
   - Apply filter
   - **Expected**: Only songs by that artist shown

4. **Test Album Filter**:
   - Select an album from dropdown
   - Apply filter
   - **Expected**: Only songs from that album shown

5. **Test Genre Filter**:
   - Select a genre from dropdown
   - Apply filter
   - **Expected**: Only songs of that genre shown

6. **Test Favorites Filter**:
   - Toggle "Show only favorites"
   - Apply filter
   - **Expected**: Only favorited songs shown

7. **Test Multiple Filters**:
   - Combine multiple filters (e.g., artist + duration + favorites)
   - Apply
   - **Expected**: Results match ALL filter criteria

8. **Test Clear Filters**:
   - Tap "Clear" button
   - **Expected**: All filters reset, full results shown

### ✅ Performance Improvements

#### Test Album Art Caching
1. Navigate through albums multiple times
2. Observe loading times:
   - First load: May take longer
   - Subsequent loads: Should be instant (from cache)
3. **Expected**: Smooth, fast album art display on repeat views

#### Test Search Performance
1. With a large library (100+ songs):
   - Perform searches with various filters
   - **Expected**: Near-instant results (no lag)

#### Test Database Performance
1. Navigate between tabs (Songs, Albums, Artists)
2. Sort songs by different criteria (title, artist, album, duration)
3. **Expected**: Smooth, responsive sorting and filtering

### ✅ Enhanced Animations

#### Test Screen Transitions
1. Navigate between different screens (Home → Songs → Albums)
2. **Observe**:
   - Smooth fade-in/fade-out
   - Subtle scale animation (slight zoom effect)
   - Spring-based motion (gentle bounce feel)
3. Use back button:
   - **Expected**: Coordinated pop animation (reverse zoom + fade)

#### Test Bottom Sheet Animations
1. Open song options menu (3-dot)
2. **Observe**: Smooth slide-up animation
3. Dismiss by swiping down or tapping outside
4. **Expected**: Smooth slide-down dismissal

### ✅ Database Migration

#### Test First Launch After Update
1. If upgrading from v1.1:
   - Launch app
   - **Expected**: Brief loading time as migration runs
   - No crashes
   - All existing data preserved

2. Check songs list:
   - **Expected**: All songs still present
   - Favorites preserved
   - Play counts maintained

## Common Issues & Solutions

### Issue: Delete Permission Denied
**Solution**: 
- Android 11+: Go to Settings → Apps → Music Player → Permissions → Files and Media → Allow "Manage all files"
- Android 10-: Grant storage permission when prompted

### Issue: Share Not Working
**Solution**:
- Ensure song files are in accessible storage
- Check FileProvider configuration in AndroidManifest

### Issue: Search Filters Not Showing Results
**Solution**:
- Verify songs have metadata (artist, album, genre)
- Try "Clear Filters" and reapply
- Check that duration range includes your songs

### Issue: Animations Laggy
**Solution**:
- Enable "Developer Options" on device
- Disable animation scale multipliers
- Or set to 0.5x for testing

### Issue: Database Migration Failed
**Solution**:
- Uninstall and reinstall app (will lose data)
- Or check logs for migration errors

## Performance Benchmarks (Expected)

### Search Performance
- Library size: 1,000 songs
  - Basic search: < 50ms
  - Filtered search: < 100ms
  - Multiple filters: < 150ms

### Album Art Loading
- First load: 200-500ms (from storage)
- Cached load: < 50ms (from memory)
- Network load (if applicable): 1-3s

### Animation Frame Rate
- Target: 60 FPS
- Minimum acceptable: 30 FPS (on low-end devices)

## Testing Matrix

| Feature | Android 8-10 | Android 11+ | Tested ✓ |
|---------|-------------|-------------|----------|
| Song Delete | ✓ | ✓ | [ ] |
| Song Share | ✓ | ✓ | [ ] |
| Search Filters | ✓ | ✓ | [ ] |
| Duration Filter | ✓ | ✓ | [ ] |
| Artist Filter | ✓ | ✓ | [ ] |
| Album Filter | ✓ | ✓ | [ ] |
| Genre Filter | ✓ | ✓ | [ ] |
| Favorites Filter | ✓ | ✓ | [ ] |
| Album Art Cache | ✓ | ✓ | [ ] |
| Animations | ✓ | ✓ | [ ] |
| DB Migration | ✓ | ✓ | [ ] |

## Reporting Issues

When reporting bugs, include:
1. Android version
2. Device model
3. Library size (number of songs)
4. Steps to reproduce
5. Expected vs actual behavior
6. Logcat output (if available)

---

**Happy Testing! 🎵**
