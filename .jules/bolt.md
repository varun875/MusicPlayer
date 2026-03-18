
## 2025-02-23 - Optimize Compose LazyRow and AsyncImage Recomposition
**Learning:** In Jetpack Compose, directly passing URIs to `AsyncImage` in `LazyRow` items without explicit `remember`-ed `ImageRequest` objects with defined `.size()` and cache keys causes unnecessary image processing overhead and lags during scroll. Additionally, `items` blocks in `LazyRow` must use the `key` parameter to prevent excessive recomposition.
**Action:** When working with image loading in lists, always inject `key = { it.id }` in `items()` and use `val imageRequest = remember(context, item.id) { ImageRequest.Builder(context)...build() }` with explicit sizes before passing to `AsyncImage`.

## 2024-03-01 - [Jetpack Compose Inline ImageRequest Recomposition Bottleneck]
**Learning:** Instantiating Coil `ImageRequest.Builder` inline inside a Composable list item without memoizing it with `remember` triggers unnecessary allocations and request rebuilding. Without explicit `.memoryCacheKey` and `.diskCacheKey` using the content URI as the key, lists can experience performance degradation and flickering during scroll, because the list attempts to reload imagery when content cycles during fast scrolling.
**Action:** Always wrap `ImageRequest` objects in a `remember` block using the actual image URI as the key (e.g., `remember(context, item.imageUri)`), and explicitly specify memory/disk cache keys rather than relying solely on the item's position or generic parameters.
