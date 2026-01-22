package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class DynamicWeatherSchedulerSystem implements AdaptiveSystem {
    private final Object2ObjectOpenHashMap<ResourceKey<Level>, WeatherState> weatherState = new Object2ObjectOpenHashMap<>();

    @Override
    public String id() {
        return "dynamic_weather_scheduler";
    }

    @Override
    public boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        return config.enableDynamicWeatherScheduler();
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
            WeatherState state = weatherState.getOrDefault(dimension, new WeatherState(false, false, level.getGameTime(), 0L));
            boolean raining = level.isRaining();
            boolean thundering = level.isThundering();
            long now = level.getGameTime();
            if (state.isRaining() != raining || state.isThundering() != thundering) {
                long lastDuration = now - state.lastChangeTick();
                state = new WeatherState(raining, thundering, now, lastDuration);
            }
            weatherState.put(dimension, state);
        }
    }

    private record WeatherState(boolean isRaining, boolean isThundering, long lastChangeTick, long lastDurationTicks) {
    }
}
