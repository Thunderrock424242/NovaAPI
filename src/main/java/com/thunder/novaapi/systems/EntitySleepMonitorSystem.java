package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class EntitySleepMonitorSystem implements AdaptiveSystem {
    private static final double SAMPLE_RADIUS = 48.0D;
    private static final double ACTIVE_MOVEMENT_THRESHOLD = 0.0025D;

    private final Object2ObjectOpenHashMap<ResourceKey<Level>, Int2LongOpenHashMap> lastActiveTick = new Object2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<ResourceKey<Level>> idleCounts = new Object2IntOpenHashMap<>();

    @Override
    public String id() {
        return "entity_sleep_monitor";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableEntitySleepMonitor();
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
            ResourceKey<Level> dimension = level.dimension();
            Int2LongOpenHashMap lastActive = lastActiveTick.computeIfAbsent(dimension, key -> new Int2LongOpenHashMap());
            long now = level.getGameTime();
            for (ServerPlayer player : level.players()) {
                if (!context.sampleBudget().tryConsume(1)) {
                    return;
                }
                for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SAMPLE_RADIUS))) {
                    if (mob.getDeltaMovement().lengthSqr() >= ACTIVE_MOVEMENT_THRESHOLD) {
                        lastActive.put(mob.getId(), now);
                    }
                }
            }

            int idle = 0;
            ObjectIterator<Int2LongMap.Entry> iterator = lastActive.int2LongEntrySet().iterator();
            while (iterator.hasNext()) {
                Int2LongMap.Entry entry = iterator.next();
                if (now - entry.getLongValue() > context.config().entityIdleTicks()) {
                    idle++;
                }
                if (lastActive.size() > context.config().maxTrackedChunks()) {
                    iterator.remove();
                }
            }
            idleCounts.put(dimension, idle);
        }
    }
}
