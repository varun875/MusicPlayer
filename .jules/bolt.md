
## 2025-02-23 - Optimize Compose LazyRow and AsyncImage Recomposition
**Learning:** In Jetpack Compose, directly passing URIs to `AsyncImage` in `LazyRow` items without explicit `remember`-ed `ImageRequest` objects with defined `.size()` and cache keys causes unnecessary image processing overhead and lags during scroll. Additionally, `items` blocks in `LazyRow` must use the `key` parameter to prevent excessive recomposition.
**Action:** When working with image loading in lists, always inject `key = { it.id }` in `items()` and use `val imageRequest = remember(context, item.id) { ImageRequest.Builder(context)...build() }` with explicit sizes before passing to `AsyncImage`.

## 2025-02-24 - Inline ImageRequest Object Churn in Compose Lists
**Learning:** Re-creating `ImageRequest.Builder(context).build()` inline inside `AsyncImage` for every item in a `LazyColumn`/`LazyRow` causes significant GC churn and frame drops. Even if cache keys are provided, the `ImageRequest` object itself is re-instantiated on every recomposition/scroll.
**Action:** Always wrap `ImageRequest` creations inside a `remember(context, itemId)` block when rendering items in a Compose list. Provide explicit `.size()`, `.memoryCacheKey()`, and `.diskCacheKey()` properties.
