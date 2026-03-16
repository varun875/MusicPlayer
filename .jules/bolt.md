
## 2025-02-23 - Optimize Compose LazyRow and AsyncImage Recomposition
**Learning:** In Jetpack Compose, directly passing URIs to `AsyncImage` in `LazyRow` items without explicit `remember`-ed `ImageRequest` objects with defined `.size()` and cache keys causes unnecessary image processing overhead and lags during scroll. Additionally, `items` blocks in `LazyRow` must use the `key` parameter to prevent excessive recomposition.
**Action:** When working with image loading in lists, always inject `key = { it.id }` in `items()` and use `val imageRequest = remember(context, item.id) { ImageRequest.Builder(context)...build() }` with explicit sizes before passing to `AsyncImage`.

## 2025-02-24 - Optimize Compose LazyGrid and AsyncImage Recomposition
**Learning:** In Jetpack Compose, passing `ImageRequest.Builder` inline inside an `AsyncImage` component in lists (`AlbumItem` or `AlbumCard` in `LazyVerticalGrid`/`LazyColumn`) causes unnecessary `ImageRequest` re-allocation and image processing lag during scrolling. Since recomposition in Lists happens often as elements appear/disappear, creating image requests must be prevented if the input URL/ID does not change.
**Action:** Always wrap `ImageRequest` objects for list items with `val imageRequest = remember(context, item.imageUri) { ImageRequest.Builder(context).data(item.imageUri).size(x).memoryCacheKey(item.imageUri).diskCacheKey(item.imageUri).build() }` to ensure cache is shared effectively and doesn't load stale data when content changes.
