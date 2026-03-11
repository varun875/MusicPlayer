
## 2025-02-23 - Optimize Compose LazyRow and AsyncImage Recomposition
**Learning:** In Jetpack Compose, directly passing URIs to `AsyncImage` in `LazyRow` items without explicit `remember`-ed `ImageRequest` objects with defined `.size()` and cache keys causes unnecessary image processing overhead and lags during scroll. Additionally, `items` blocks in `LazyRow` must use the `key` parameter to prevent excessive recomposition.
**Action:** When working with image loading in lists, always inject `key = { it.id }` in `items()` and use `val imageRequest = remember(context, item.id) { ImageRequest.Builder(context)...build() }` with explicit sizes before passing to `AsyncImage`.

## 2024-05-19 - Coil ImageRequest Memoization in Lazy Lists
**Learning:** In Jetpack Compose, directly passing `ImageRequest.Builder(LocalContext.current)...build()` into `AsyncImage` within a lazy list item (`AlbumItem`, `AlbumCard`) causes constant reallocation and prevents effective caching by Coil, leading to UI jank and excessive memory allocation during scrolling.
**Action:** Always memoize `ImageRequest` objects using `remember(key)` and explicitly specify `.size()`, `.memoryCacheKey()`, and `.diskCacheKey()` when working with Coil inside Compose list items, especially for lists of albums or songs.
