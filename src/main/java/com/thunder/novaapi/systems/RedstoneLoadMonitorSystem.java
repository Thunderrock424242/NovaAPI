package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class RedstoneLoadMonitorSystem implements AdaptiveSystem {
    private final Object2LongOpenHashMap<ResourceKey<Level>> scheduledTickLoad = new Object2LongOpenHashMap<>();

    @Override
    public String id() {
        return "redstone_load_monitor";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableRedstoneLoadMonitor();
    }

    @Override
    public void tick(MinecraftServer server, SafetyContext context) {
        if (!context.allowWork()) {
            return;
        }
        if (context.tickCounter() % context.config().sampleIntervalTicks() != 0) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (!context.sampleBudget().tryConsume(1)) {
                return;
            }
            ResourceKey<Level> dimension = level.dimension();
            long blockTicks = level.getBlockTicks().count();
            long fluidTicks = level.getFluidTicks().count();
            scheduledTickLoad.put(dimension, blockTicks + fluidTicks);
        }
    }
}
