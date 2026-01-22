package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Iterator;
import java.util.Map;

public class RegionIntelligenceSystem implements AdaptiveSystem {
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2LongOpenHashMap> lastSeen = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2IntOpenHashMap> activityScore = new Object2ObjectOpenHashMap<>();

    @Override
    public String id() {
        return "region_intelligence";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableRegionIntelligence();
    }

    @Override
    public void tick(MinecraftServer server, SafetyContext context) {
        if (!context.allowWork()) {
            return;
        }
        if (context.tickCounter() % context.config().sampleIntervalTicks() != 0) {
            return;
        }
        int playerCount = server.getPlayerList().getPlayerCount();
        if (!context.sampleBudget().tryConsume(Math.max(1, playerCount))) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimension = level.dimension();
            Long2LongOpenHashMap dimensionLastSeen = lastSeen.computeIfAbsent(dimension, key -> new Long2LongOpenHashMap());
            Long2IntOpenHashMap dimensionScore = activityScore.computeIfAbsent(dimension, key -> new Long2IntOpenHashMap());
            for (ServerPlayer player : level.players()) {
                ChunkPos chunkPos = player.chunkPosition();
                long chunkKey = chunkPos.toLong();
                dimensionLastSeen.put(chunkKey, context.tickCounter());
                dimensionScore.addTo(chunkKey, 1);
            }
            prune(dimensionLastSeen, dimensionScore, context.config(), context.tickCounter());
        }
    }

    private void prune(Long2LongOpenHashMap lastSeenMap,
                       Long2IntOpenHashMap scoreMap,
                       AdaptiveSystemsConfig.AdaptiveSystemsValues config,
                       long tickCounter) {
        long ttl = config.chunkActivityTtlTicks();
        if (ttl > 0) {
            Iterator<Map.Entry<Long, Long>> iterator = lastSeenMap.long2LongEntrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Long> entry = iterator.next();
                if (tickCounter - entry.getValue() > ttl) {
                    long key = entry.getKey();
                    iterator.remove();
                    scoreMap.remove(key);
                }
            }
        }

        int maxTracked = config.maxTrackedChunks();
        if (lastSeenMap.size() <= maxTracked) {
            return;
        }

        int toRemove = Math.max(0, lastSeenMap.size() - maxTracked);
        Iterator<Map.Entry<Long, Long>> iterator = lastSeenMap.long2LongEntrySet().iterator();
        while (iterator.hasNext() && toRemove > 0) {
            Map.Entry<Long, Long> entry = iterator.next();
            long key = entry.getKey();
            iterator.remove();
            scoreMap.remove(key);
            toRemove--;
        }
    }
}
