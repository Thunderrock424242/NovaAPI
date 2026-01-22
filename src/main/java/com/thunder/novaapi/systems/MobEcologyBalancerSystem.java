package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

public class MobEcologyBalancerSystem implements AdaptiveSystem {
    private static final double SAMPLE_RADIUS = 48.0D;
    private final Object2IntOpenHashMap<ResourceKey<Biome>> biomePressure = new Object2IntOpenHashMap<>();

    @Override
    public String id() {
        return "mob_ecology";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableMobEcology();
    }

    @Override
    public void tick(MinecraftServer server, SafetyContext context) {
        if (!context.allowWork()) {
            return;
        }
        if (context.tickCounter() % context.config().sampleIntervalTicks() != 0) {
            return;
        }

        biomePressure.clear();
        int samples = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!context.sampleBudget().tryConsume(1)) {
                    return;
                }
                int mobCount = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SAMPLE_RADIUS)).size();
                Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
                Optional<ResourceKey<Biome>> biomeKey = biomeHolder.unwrapKey();
                biomeKey.ifPresent(key -> biomePressure.addTo(key, mobCount));
                samples++;
            }
        }

        if (samples == 0) {
            return;
        }
    }
}
