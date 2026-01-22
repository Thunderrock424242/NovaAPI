package com.thunder.novaapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AdaptiveSystemsConfig {
    public static final ModConfigSpec CONFIG_SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_ADAPTIVE_SYSTEMS;
    public static final ModConfigSpec.BooleanValue ENABLE_REGION_INTELLIGENCE;
    public static final ModConfigSpec.BooleanValue ENABLE_MOB_ECOLOGY;
    public static final ModConfigSpec.BooleanValue ENABLE_REDSTONE_LOAD_MONITOR;
    public static final ModConfigSpec.BooleanValue ENABLE_LOGISTICS_NETWORK_MONITOR;
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_AWARE_AI_SCHEDULER;
    public static final ModConfigSpec.BooleanValue ENABLE_STRUCTURE_INDEX;
    public static final ModConfigSpec.BooleanValue ENABLE_DYNAMIC_WEATHER_SCHEDULER;
    public static final ModConfigSpec.BooleanValue ENABLE_SIMULATION_DENSITY_SCALER;
    public static final ModConfigSpec.BooleanValue ENABLE_INVENTORY_HEAT_TRACKING;
    public static final ModConfigSpec.BooleanValue ENABLE_ENTITY_SLEEP_MONITOR;

    public static final ModConfigSpec.IntValue SAMPLE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue MAX_SAMPLES_PER_TICK;

    public static final ModConfigSpec.IntValue SAFETY_TICK_BUDGET_MS;
    public static final ModConfigSpec.IntValue SAFETY_MIN_FREE_MEMORY_MB;
    public static final ModConfigSpec.IntValue SAFETY_COOLDOWN_TICKS;

    public static final ModConfigSpec.IntValue MAX_TRACKED_CHUNKS;
    public static final ModConfigSpec.IntValue CHUNK_ACTIVITY_TTL_TICKS;
    public static final ModConfigSpec.IntValue ENTITY_IDLE_TICKS;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("adaptiveSystems");
        ENABLE_ADAPTIVE_SYSTEMS = BUILDER
                .comment("Master switch for adaptive systems features.")
                .define("enableAdaptiveSystems", true);

        BUILDER.push("systems");
        ENABLE_REGION_INTELLIGENCE = BUILDER
                .comment("Track per-region activity and heat maps.")
                .define("enableRegionIntelligence", true);
        ENABLE_MOB_ECOLOGY = BUILDER
                .comment("Track biome population pressure and mob ecology signals.")
                .define("enableMobEcology", true);
        ENABLE_REDSTONE_LOAD_MONITOR = BUILDER
                .comment("Monitor redstone activity and hotspots.")
                .define("enableRedstoneLoadMonitor", true);
        ENABLE_LOGISTICS_NETWORK_MONITOR = BUILDER
                .comment("Track item transport pressure near players.")
                .define("enableLogisticsNetworkMonitor", true);
        ENABLE_CHUNK_AWARE_AI_SCHEDULER = BUILDER
                .comment("Track AI density around players for chunk-aware scheduling.")
                .define("enableChunkAwareAiScheduler", true);
        ENABLE_STRUCTURE_INDEX = BUILDER
                .comment("Maintain a lightweight structure activity index.")
                .define("enableStructureIndex", true);
        ENABLE_DYNAMIC_WEATHER_SCHEDULER = BUILDER
                .comment("Track weather rhythms and environmental pressure.")
                .define("enableDynamicWeatherScheduler", true);
        ENABLE_SIMULATION_DENSITY_SCALER = BUILDER
                .comment("Track simulation density bands around players.")
                .define("enableSimulationDensityScaler", true);
        ENABLE_INVENTORY_HEAT_TRACKING = BUILDER
                .comment("Track frequently accessed storage positions.")
                .define("enableInventoryHeatTracking", true);
        ENABLE_ENTITY_SLEEP_MONITOR = BUILDER
                .comment("Track idle entity populations and safe sleep candidates.")
                .define("enableEntitySleepMonitor", true);
        BUILDER.pop();

        BUILDER.push("sampling");
        SAMPLE_INTERVAL_TICKS = BUILDER
                .comment("Base sampling interval for adaptive systems.")
                .defineInRange("sampleIntervalTicks", 200, 20, 2400);
        MAX_SAMPLES_PER_TICK = BUILDER
                .comment("Maximum samples performed per tick across adaptive systems.")
                .defineInRange("maxSamplesPerTick", 128, 16, 2048);
        BUILDER.pop();

        BUILDER.push("safety");
        SAFETY_TICK_BUDGET_MS = BUILDER
                .comment("Max tick time before adaptive systems enter cooldown.")
                .defineInRange("safetyTickBudgetMs", 45, 15, 200);
        SAFETY_MIN_FREE_MEMORY_MB = BUILDER
                .comment("Minimum free memory before adaptive systems enter cooldown.")
                .defineInRange("safetyMinFreeMemoryMb", 512, 128, 32768);
        SAFETY_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown ticks after a safety trip.")
                .defineInRange("safetyCooldownTicks", 600, 40, 72000);
        BUILDER.pop();

        BUILDER.push("limits");
        MAX_TRACKED_CHUNKS = BUILDER
                .comment("Maximum number of tracked chunks per dimension for adaptive systems.")
                .defineInRange("maxTrackedChunks", 4096, 256, 65536);
        CHUNK_ACTIVITY_TTL_TICKS = BUILDER
                .comment("Ticks before inactive chunk activity data expires.")
                .defineInRange("chunkActivityTtlTicks", 24000, 600, 240000);
        ENTITY_IDLE_TICKS = BUILDER
                .comment("Ticks before entities are considered idle candidates.")
                .defineInRange("entityIdleTicks", 1200, 100, 240000);
        BUILDER.pop();

        BUILDER.pop();

        CONFIG_SPEC = BUILDER.build();
    }

    private AdaptiveSystemsConfig() {
    }

    public static AdaptiveSystemsValues values() {
        try {
            return new AdaptiveSystemsValues(
                    ENABLE_ADAPTIVE_SYSTEMS.get(),
                    ENABLE_REGION_INTELLIGENCE.get(),
                    ENABLE_MOB_ECOLOGY.get(),
                    ENABLE_REDSTONE_LOAD_MONITOR.get(),
                    ENABLE_LOGISTICS_NETWORK_MONITOR.get(),
                    ENABLE_CHUNK_AWARE_AI_SCHEDULER.get(),
                    ENABLE_STRUCTURE_INDEX.get(),
                    ENABLE_DYNAMIC_WEATHER_SCHEDULER.get(),
                    ENABLE_SIMULATION_DENSITY_SCALER.get(),
                    ENABLE_INVENTORY_HEAT_TRACKING.get(),
                    ENABLE_ENTITY_SLEEP_MONITOR.get(),
                    SAMPLE_INTERVAL_TICKS.get(),
                    MAX_SAMPLES_PER_TICK.get(),
                    SAFETY_TICK_BUDGET_MS.get(),
                    SAFETY_MIN_FREE_MEMORY_MB.get(),
                    SAFETY_COOLDOWN_TICKS.get(),
                    MAX_TRACKED_CHUNKS.get(),
                    CHUNK_ACTIVITY_TTL_TICKS.get(),
                    ENTITY_IDLE_TICKS.get()
            );
        } catch (IllegalStateException ex) {
            return defaultValues();
        }
    }

    public static AdaptiveSystemsValues defaultValues() {
        return new AdaptiveSystemsValues(
                ENABLE_ADAPTIVE_SYSTEMS.getDefault(),
                ENABLE_REGION_INTELLIGENCE.getDefault(),
                ENABLE_MOB_ECOLOGY.getDefault(),
                ENABLE_REDSTONE_LOAD_MONITOR.getDefault(),
                ENABLE_LOGISTICS_NETWORK_MONITOR.getDefault(),
                ENABLE_CHUNK_AWARE_AI_SCHEDULER.getDefault(),
                ENABLE_STRUCTURE_INDEX.getDefault(),
                ENABLE_DYNAMIC_WEATHER_SCHEDULER.getDefault(),
                ENABLE_SIMULATION_DENSITY_SCALER.getDefault(),
                ENABLE_INVENTORY_HEAT_TRACKING.getDefault(),
                ENABLE_ENTITY_SLEEP_MONITOR.getDefault(),
                SAMPLE_INTERVAL_TICKS.getDefault(),
                MAX_SAMPLES_PER_TICK.getDefault(),
                SAFETY_TICK_BUDGET_MS.getDefault(),
                SAFETY_MIN_FREE_MEMORY_MB.getDefault(),
                SAFETY_COOLDOWN_TICKS.getDefault(),
                MAX_TRACKED_CHUNKS.getDefault(),
                CHUNK_ACTIVITY_TTL_TICKS.getDefault(),
                ENTITY_IDLE_TICKS.getDefault()
        );
    }

    public record AdaptiveSystemsValues(
            boolean enableAdaptiveSystems,
            boolean enableRegionIntelligence,
            boolean enableMobEcology,
            boolean enableRedstoneLoadMonitor,
            boolean enableLogisticsNetworkMonitor,
            boolean enableChunkAwareAiScheduler,
            boolean enableStructureIndex,
            boolean enableDynamicWeatherScheduler,
            boolean enableSimulationDensityScaler,
            boolean enableInventoryHeatTracking,
            boolean enableEntitySleepMonitor,
            int sampleIntervalTicks,
            int maxSamplesPerTick,
            int safetyTickBudgetMs,
            int safetyMinFreeMemoryMb,
            int safetyCooldownTicks,
            int maxTrackedChunks,
            int chunkActivityTtlTicks,
            int entityIdleTicks
    ) {
    }
}
