package com.thunder.NovaAPI.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * An unload-aware, TTL-governed cache that keys entries by dimension + chunk position (as a long).
 *
 * @param <V> value type stored in the cache
 */
public class RegionScopedCache<V> {
    private final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Entry<V>>> caches = new HashMap<>();
    private final int maxEntriesPerDimension;
    private final long ttlMillis;

    public RegionScopedCache(int maxEntriesPerDimension, long ttlMillis) {
        this.maxEntriesPerDimension = maxEntriesPerDimension;
        this.ttlMillis = ttlMillis;
    }

    public synchronized V getOrCompute(ResourceKey<Level> dimension, ChunkPos chunkPos, Supplier<V> supplier) {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(chunkPos, "chunkPos");
        Objects.requireNonNull(supplier, "supplier");

        Long2ObjectOpenHashMap<Entry<V>> cache = caches.computeIfAbsent(dimension, ignored -> new Long2ObjectOpenHashMap<>());
        long key = chunkPos.toLong();

        purgeExpired(cache);
        Entry<V> existing = cache.get(key);
        if (existing != null) {
            return existing.value();
        }

        V value = supplier.get();
        cache.put(key, new Entry<>(value, Instant.now().toEpochMilli()));
        enforceLimit(cache);
        return value;
    }

    public synchronized void remove(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        Long2ObjectOpenHashMap<Entry<V>> cache = caches.get(dimension);
        if (cache != null) {
            cache.remove(chunkPos.toLong());
            if (cache.isEmpty()) {
                caches.remove(dimension);
            }
        }
    }

    public synchronized void clear() {
        caches.clear();
    }

    public synchronized Metrics metrics() {
        int dimensions = caches.size();
        long entries = caches.values().stream().mapToLong(Long2ObjectOpenHashMap::size).sum();
        return new Metrics(dimensions, entries, ttlMillis, maxEntriesPerDimension);
    }

    private void enforceLimit(Long2ObjectOpenHashMap<Entry<V>> cache) {
        while (cache.size() > maxEntriesPerDimension) {
            // Remove the oldest entry to stay under the cap.
            long[] keys = cache.keySet().toLongArray();
            Entry<V>[] values = cache.values().toArray(new Entry[0]);
            long oldestTimestamp = Long.MAX_VALUE;
            long oldestKey = 0;

            for (int i = 0; i < keys.length; i++) {
                Entry<V> entry = values[i];
                if (entry != null && entry.createdAt() < oldestTimestamp) {
                    oldestTimestamp = entry.createdAt();
                    oldestKey = keys[i];
                }
            }

            cache.remove(oldestKey);
        }
    }

    private void purgeExpired(Long2ObjectOpenHashMap<Entry<V>> cache) {
        if (cache.isEmpty() || ttlMillis <= 0) {
            return;
        }

        long cutoff = Instant.now().toEpochMilli() - ttlMillis;
        long[] keys = cache.keySet().toLongArray();
        for (long key : keys) {
            Entry<V> entry = cache.get(key);
            if (entry != null && entry.createdAt() < cutoff) {
                cache.remove(key);
            }
        }
    }

    public record Metrics(int trackedDimensions, long cachedEntries, long ttlMillis, int maxEntriesPerDimension) {
    }

    private record Entry<V>(V value, long createdAt) {
    }
}
