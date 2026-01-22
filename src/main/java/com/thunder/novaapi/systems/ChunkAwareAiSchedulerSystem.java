package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class ChunkAwareAiSchedulerSystem implements AdaptiveSystem {
    private static final double SAMPLE_RADIUS = 40.0D;
    private final Object2IntOpenHashMap<ResourceKey<Level>> aiDensity = new Object2IntOpenHashMap<>();

    @Override
    public String id() {
        return "chunk_aware_ai_scheduler";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableChunkAwareAiScheduler();
    }

    @Override
    public void tick(MinecraftServer server, SafetyContext context) {
        if (!context.allowWork()) {
            return;
        }
        if (context.tickCounter() % context.config().sampleIntervalTicks() != 0) {
            return;
        }

        aiDensity.clear();
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimension = level.dimension();
            int density = 0;
            for (ServerPlayer player : level.players()) {
                if (!context.sampleBudget().tryConsume(1)) {
                    return;
                }
                density = Math.max(density, level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SAMPLE_RADIUS)).size());
            }
            aiDensity.put(dimension, density);
        }
    }
}
