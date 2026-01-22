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
    private static final double NEAR_RADIUS_SQ = NEAR_RADIUS * NEAR_RADIUS;
    private static final double MID_RADIUS_SQ = MID_RADIUS * MID_RADIUS;
    private static final double FAR_RADIUS_SQ = FAR_RADIUS * FAR_RADIUS;

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
            var players = level.players();
            for (ServerPlayer player : players) {
                if (!context.sampleBudget().tryConsume(1)) {
                    return;
                }
                int nearCount = 0;
                int midCount = 0;
                int farCount = 0;
                for (ServerPlayer other : players) {
                    double distanceSq = player.distanceToSqr(other);
                    if (distanceSq <= FAR_RADIUS_SQ) {
                        farCount++;
                        if (distanceSq <= MID_RADIUS_SQ) {
                            midCount++;
                            if (distanceSq <= NEAR_RADIUS_SQ) {
                                nearCount++;
                            }
                        }
                    }
                }
                maxNear = Math.max(maxNear, nearCount);
                maxMid = Math.max(maxMid, midCount);
                maxFar = Math.max(maxFar, farCount);
            }
            densitySnapshot.put(level.dimension(), new DensitySnapshot(maxNear, maxMid, maxFar));
        }
    }

    private record DensitySnapshot(int nearPlayers, int midPlayers, int farPlayers) {
    }
}
