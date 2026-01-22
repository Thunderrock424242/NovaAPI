package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Iterator;
import java.util.Map;

public class InventoryHeatSystem implements AdaptiveSystem {
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2IntOpenHashMap> accessCounts = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Long2LongOpenHashMap> lastAccess = new Object2ObjectOpenHashMap<>();

    @Override
    public String id() {
        return "inventory_heat";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableInventoryHeatTracking();
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
            prune(level.dimension(), context.config(), level.getGameTime());
        }
    }

    @SubscribeEvent
    public void onContainerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container)) {
            return;
        }
        ResourceKey<Level> dimension = level.dimension();
        Long2IntOpenHashMap counts = accessCounts.computeIfAbsent(dimension, key -> new Long2IntOpenHashMap());
        Long2LongOpenHashMap last = lastAccess.computeIfAbsent(dimension, key -> new Long2LongOpenHashMap());
        long key = pos.asLong();
        counts.addTo(key, 1);
        last.put(key, level.getGameTime());
    }

    private void prune(ResourceKey<Level> dimension,
                       AdaptiveSystemsConfig.AdaptiveSystemsValues config,
                       long gameTime) {
        Long2LongOpenHashMap last = lastAccess.get(dimension);
        if (last == null) {
            return;
        }
        Long2IntOpenHashMap counts = accessCounts.get(dimension);
        long ttl = config.chunkActivityTtlTicks();
        Iterator<Map.Entry<Long, Long>> iterator = last.long2LongEntrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > ttl) {
                long key = entry.getKey();
                iterator.remove();
                if (counts != null) {
                    counts.remove(key);
                }
            }
        }
        if (last.size() <= config.maxTrackedChunks()) {
            return;
        }
        int toRemove = last.size() - config.maxTrackedChunks();
        iterator = last.long2LongEntrySet().iterator();
        while (iterator.hasNext() && toRemove > 0) {
            Map.Entry<Long, Long> entry = iterator.next();
            long key = entry.getKey();
            iterator.remove();
            if (counts != null) {
                counts.remove(key);
            }
            toRemove--;
        }
    }
}
