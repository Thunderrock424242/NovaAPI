package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

public class LogisticsNetworkMonitorSystem implements AdaptiveSystem {
    private static final double SAMPLE_RADIUS = 32.0D;
    private final Object2IntOpenHashMap<ResourceKey<Level>> itemPressure = new Object2IntOpenHashMap<>();

    @Override
    public String id() {
        return "logistics_network_monitor";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableLogisticsNetworkMonitor();
    }

    @Override
    public void tick(MinecraftServer server, SafetyContext context) {
        if (!context.allowWork()) {
            return;
        }
        if (context.tickCounter() % context.config().sampleIntervalTicks() != 0) {
            return;
        }

        itemPressure.clear();
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimension = level.dimension();
            int pressure = 0;
            for (ServerPlayer player : level.players()) {
                if (!context.sampleBudget().tryConsume(1)) {
                    return;
                }
                pressure += level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(SAMPLE_RADIUS)).size();
            }
            itemPressure.put(dimension, pressure);
        }
    }
}
