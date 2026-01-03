# Memory and Performance Guidelines for NeoForge Mods

These practices keep NovaAPI-using mods lean and avoid hidden memory pressure when building with NeoForge and Gradle.

## 1. Prevent accidental world retention (hidden RAM killer)
- Do **not** keep long-lived references to heavy Minecraft objects (`Level`, `ServerLevel`, `ChunkAccess`/`LevelChunk`, `BlockEntity`, `Entity`, `Player`, `Biome`, `BlockState`) in static singletons or caches.
- Store identifiers instead:
  - Dimension: `ResourceKey<Level>`
  - Position: `BlockPos.asLong()`
  - Entity: `UUID` (or short-lived entity ID)
- For fast lookups, use maps keyed by dimension + `longPos` instead of holding chunk references.
- Rule of thumb: holding a chunk reference can keep an entire region loaded in memory.

## 2. Eliminate per-tick allocations
- Avoid constant allocations that grow the heap and drive GC pressure.
- Common culprits: new `ArrayList`/`HashMap` each tick, many `BlockPos`/`Vec3` creations, lambdas/streams, string concatenation in hot paths.
- Fixes: reuse buffers (`IntArrayList`, `LongArrayList`, pre-sized arrays), prefer for-loops over streams in hot paths, use `MutableBlockPos` for iteration, and build debug strings only when debugging is enabled.

## 3. Prefer primitive collections
- Replace boxed collections with primitive variants to cut memory use (often 2–5× savings).
- Examples: `Map<Long, Foo>` instead of `Map<BlockPos, Foo>` (store `BlockPos.asLong()`), `Long2ObjectOpenHashMap`, `LongOpenHashSet`, and other fastutil primitives.

## 4. Cap caches and make them expire
- Any cache tied to exploration can grow without bound.
- Best practices:
  - Use LRU or size-limited caches (max entries or max bytes).
  - Add TTL for “nice to have” cached data.
  - Make caches dimension-scoped and unload-aware (purge entries on chunk unload).
- If NovaAPI does chunk preloading or structure tracking, cap the cache.

## 5. Avoid caching derived data that is cheap to recompute
- Examples of risky cached data: full lists of nearby blocks/entities every tick, prebuilt path nodes, full NBT blobs stored for convenience.
- Better options: store a small summary (hash/version/timestamp), recompute on demand, or store compressed/serialized forms when recomputation is truly expensive.

## 6. Store large datasets serialized and compact
- For big datasets (migration maps, structure indexes, scan results):
  - Store compact primitives (`int`/`long`).
  - Store compressed bytes (LZ4/gzip) and inflate only when needed.
  - Keep data on disk and memory-map/page it in; load per region when possible.
- Prefer region-based indexing: key by `ChunkPos`/region coordinates and keep only active regions hot in RAM.

## 7. Avoid listener leaks and static registries that never clear
- Leaks often come from callbacks that are never unregistered:
  - Event-bus listeners kept in static instances.
  - Scheduled tasks retaining closures with world/player references.
  - Thread pools holding runnables with captured data.
- Fixes: on server stop or datapack reload, clear caches, cancel tasks, and shut down executors. Avoid background tasks that capture `Level`/`Player` references.

## 8. Track memory like TPS during development
- Add a dev-only “memory + top caches” debug command/log that reports:
  - Total heap used.
  - Sizes of major caches.
  - Entry counts per dimension.
  - Estimated bytes per entry (rough estimates are useful).

Incorporate these patterns into NovaAPI features and integrations to keep servers stable and memory-efficient.
