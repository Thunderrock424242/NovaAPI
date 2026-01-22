package com.thunder.novaapi.chunk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies tick throttles for warm chunks and scales random tick density based on player movement.
 */
public final class ChunkTickThrottler {
    private static ChunkStreamingConfig.ChunkConfigValues config = ChunkStreamingConfig.values();
    private static final Map<UUID, PlayerSample> LAST_PLAYER_SAMPLES = new ConcurrentHashMap<>();

    private ChunkTickThrottler() {
    }

    public static void configure(ChunkStreamingConfig.ChunkConfigValues values) {
        config = values;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            PlayerSample previous = LAST_PLAYER_SAMPLES.get(player.getUUID());
            Vec3 pos = player.position();
            if (previous == null) {
                LAST_PLAYER_SAMPLES.put(player.getUUID(), new PlayerSample(pos, now, 0.0D));
                continue;
            }
            double horizontalDelta = pos.subtract(previous.position()).horizontalDistance();
            double speedPerTick = horizontalDelta / Math.max(1L, now - previous.sampleTick());
            LAST_PLAYER_SAMPLES.put(player.getUUID(), new PlayerSample(pos, now, speedPerTick));
        }
    }

    public static boolean shouldSkipWarmTicking(ChunkPos pos) {
        return config.skipWarmCacheTicking() && ChunkStreamManager.isWarmCached(pos);
    }

    public static int scaleRandomTickDensity(ServerLevel level, ChunkPos pos, int baseRandomTicks) {
        if (baseRandomTicks <= 0) {
            return 0;
        }
        double nearestDistanceSq = Double.MAX_VALUE;
        double contributingSpeed = 0.0D;

        for (ServerPlayer player : level.players()) {
            double distanceSq = distanceSqToChunkCenter(pos, player.position());
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                contributingSpeed = LAST_PLAYER_SAMPLES.getOrDefault(player.getUUID(), PlayerSample.ZERO).speed();
            }
        }

        double throttleRadius = config.fluidRedstoneThrottleRadius();
        double randomTickBand = config.randomTickPlayerBand();
        double throttleRadiusSq = throttleRadius * throttleRadius;
        double randomTickBandSq = randomTickBand * randomTickBand;

        if (throttleRadius > 0
                && nearestDistanceSq > throttleRadiusSq
                && (level.getGameTime() % config.fluidRedstoneThrottleInterval() != 0)) {
            return 0;
        }

        if (nearestDistanceSq > randomTickBandSq) {
            return Math.max(0, (int) Math.round(baseRandomTicks * config.randomTickMinScale()));
        }

        double normalizedSpeed = Math.min(1.0D, contributingSpeed / config.movementSpeedForMaxScale());
        double scale = Mth.clampedLerp(config.randomTickMinScale(), config.randomTickMaxScale(), normalizedSpeed);
        return Math.max(0, (int) Math.round(baseRandomTicks * scale));
    }

    private static double distanceSqToChunkCenter(ChunkPos pos, Vec3 playerPos) {
        double centerX = pos.getMiddleBlockX();
        double centerZ = pos.getMiddleBlockZ();
        double dx = playerPos.x - centerX;
        double dz = playerPos.z - centerZ;
        return dx * dx + dz * dz;
    }

    private record PlayerSample(Vec3 position, long sampleTick, double speed) {
        private static final PlayerSample ZERO = new PlayerSample(Vec3.ZERO, 0L, 0.0D);
    }
}
