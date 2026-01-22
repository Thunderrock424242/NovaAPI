package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SimulationDensitySystem implements AdaptiveSystem {
    private static final double NEAR_RADIUS = 64.0D;
    private static final double MID_RADIUS = 128.0D;
    private static final double FAR_RADIUS = 192.0D;

    private final Object2ObjectOpenHashMap<ResourceKey<Level>, DensitySnapshot> densitySnapshot = new Object2ObjectOpenHashMap<>();

    @Override
    public String id() {
        return "simulation_density_scaler";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableSimulationDensityScaler();
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
            int maxNear = 0;
            int maxMid = 0;
            int maxFar = 0;
            for (ServerPlayer player : level.players()) {
                if (!context.sampleBudget().tryConsume(1)) {
                    return;
                }
                maxNear = Math.max(maxNear, level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(NEAR_RADIUS)).size());
                maxMid = Math.max(maxMid, level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(MID_RADIUS)).size());
                maxFar = Math.max(maxFar, level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(FAR_RADIUS)).size());
            }
            densitySnapshot.put(level.dimension(), new DensitySnapshot(maxNear, maxMid, maxFar));
        }
    }

    private record DensitySnapshot(int nearPlayers, int midPlayers, int farPlayers) {
    }
}
