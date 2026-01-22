package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Iterator;
import java.util.Map;

public class StructureIndexSystem implements AdaptiveSystem {
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2LongOpenHashMap> loadedChunks = new Object2ObjectOpenHashMap<>();

    @Override
    public String id() {
        return "structure_index";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableStructureIndex();
    }

    @Override
    public void tick(MinecraftServer server, SafetyContext context) {
        if (!context.allowWork()) {
            return;
        }
        if (context.tickCounter() % context.config().sampleIntervalTicks() != 0) {
            return;
        }
        if (!context.sampleBudget().tryConsume(1)) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimension = level.dimension();
            Long2LongOpenHashMap chunkMap = loadedChunks.get(dimension);
            if (chunkMap == null) {
                continue;
            }
            prune(chunkMap, context.config(), level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        Long2LongOpenHashMap chunkMap = loadedChunks.computeIfAbsent(dimension, key -> new Long2LongOpenHashMap());
        ChunkPos chunkPos = event.getChunk().getPos();
        chunkMap.put(chunkPos.toLong(), level.getGameTime());
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        Long2LongOpenHashMap chunkMap = loadedChunks.get(dimension);
        if (chunkMap == null) {
            return;
        }
        ChunkPos chunkPos = event.getChunk().getPos();
        chunkMap.remove(chunkPos.toLong());
    }

    private void prune(Long2LongOpenHashMap chunkMap,
                       AdaptiveSystemsConfig.AdaptiveSystemsValues config,
                       long gameTime) {
        long ttl = config.chunkActivityTtlTicks();
        Iterator<Map.Entry<Long, Long>> iterator = chunkMap.long2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > ttl) {
                iterator.remove();
            }
        }

        int maxTracked = config.maxTrackedChunks();
        if (chunkMap.size() <= maxTracked) {
            return;
        }
        int toRemove = chunkMap.size() - maxTracked;
        iterator = chunkMap.long2LongEntrySet().iterator();
        while (iterator.hasNext() && toRemove > 0) {
            iterator.next();
            iterator.remove();
            toRemove--;
        }
    }
}
