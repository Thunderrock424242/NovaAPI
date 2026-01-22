package com.thunder.novaapi.cache;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

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
        cache.put(key, new Entry<>(value, System.currentTimeMillis()));
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
        if (maxEntriesPerDimension <= 0) {
            return;
        }
        while (cache.size() > maxEntriesPerDimension) {
            // Remove the oldest entry to stay under the cap.
            long oldestTimestamp = Long.MAX_VALUE;
            long oldestKey = Long.MIN_VALUE;
            for (Long2ObjectMap.Entry<Entry<V>> entry : cache.long2ObjectEntrySet()) {
                Entry<V> value = entry.getValue();
                if (value != null && value.createdAt() < oldestTimestamp) {
                    oldestTimestamp = value.createdAt();
                    oldestKey = entry.getLongKey();
                }
            }
            if (oldestKey == Long.MIN_VALUE) {
                break;
            }
            cache.remove(oldestKey);
        }
    }

    private void purgeExpired(Long2ObjectOpenHashMap<Entry<V>> cache) {
        if (cache.isEmpty() || ttlMillis <= 0) {
            return;
        }

        long cutoff = System.currentTimeMillis() - ttlMillis;
        var iterator = cache.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Entry<V> entry = iterator.next().getValue();
            if (entry != null && entry.createdAt() < cutoff) {
                iterator.remove();
            }
        }
    }

    public record Metrics(int trackedDimensions, long cachedEntries, long ttlMillis, int maxEntriesPerDimension) {
    }

    private record Entry<V>(V value, long createdAt) {
    }
}
