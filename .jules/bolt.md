
## 2025-02-23 - Optimize Compose LazyRow and AsyncImage Recomposition
**Learning:** In Jetpack Compose, directly passing URIs to `AsyncImage` in `LazyRow` items without explicit `remember`-ed `ImageRequest` objects with defined `.size()` and cache keys causes unnecessary image processing overhead and lags during scroll. Additionally, `items` blocks in `LazyRow` must use the `key` parameter to prevent excessive recomposition.
**Action:** When working with image loading in lists, always inject `key = { it.id }` in `items()` and use `val imageRequest = remember(context, item.id) { ImageRequest.Builder(context)...build() }` with explicit sizes before passing to `AsyncImage`.

## 2025-02-23 - Memoizing AsyncImage Builders for Scrolling Performance
**Learning:** In Jetpack Compose, calling `ImageRequest.Builder` directly within a composable that is rendered many times (e.g., in a `LazyColumn` or `LazyVerticalGrid`) causes significant CPU overhead because the builder is re-evaluated and reconstructed on every scroll or recomposition. This causes scroll stuttering, especially if proper disk and memory caching keys aren't set explicitly to avoid Coil redownloading or recomputing.
**Action:** When rendering lists of images (e.g., album art) with Coil in Compose, wrap the `ImageRequest.Builder` logic in a `remember` block keyed by a unique item ID (e.g., `album.id`). Also, explicitly provide `.memoryCacheKey` and `.diskCacheKey` to the builder to ensure efficient caching without the cost of recreation during scrolling.
