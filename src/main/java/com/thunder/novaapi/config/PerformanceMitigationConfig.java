package com.thunder.novaapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config entries for performance mitigation features.
 */
public final class PerformanceMitigationConfig {
    public static final ModConfigSpec CONFIG_SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_RENDER_DISTANCE_MITIGATION;
    public static final ModConfigSpec.IntValue RENDER_DISTANCE_FPS_THRESHOLD_MS;
    public static final ModConfigSpec.DoubleValue RENDER_DISTANCE_QUEUE_PRESSURE_THRESHOLD;
    public static final ModConfigSpec.IntValue RENDER_DISTANCE_VIEW_DROP;
    public static final ModConfigSpec.IntValue RENDER_DISTANCE_ENTITY_DROP;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("performanceMitigation");
        ENABLE_RENDER_DISTANCE_MITIGATION = BUILDER
                .comment("Enable render-distance mitigation suggestions/actions.")
                .define("enableRenderDistanceMitigation", true);
        RENDER_DISTANCE_FPS_THRESHOLD_MS = BUILDER
                .comment("Worst tick time (ms) treated as a render-distance spike.")
                .defineInRange("renderDistanceFpsThresholdMs", 55, 20, 200);
        RENDER_DISTANCE_QUEUE_PRESSURE_THRESHOLD = BUILDER
                .comment("Chunk I/O queue saturation ratio that signals render queue pressure.")
                .defineInRange("renderDistanceQueuePressureThreshold", 0.85D, 0.0D, 2.0D);
        RENDER_DISTANCE_VIEW_DROP = BUILDER
                .comment("Chunks to drop from client view distance during render-distance mitigation.")
                .defineInRange("renderDistanceViewDrop", 2, 0, 16);
        RENDER_DISTANCE_ENTITY_DROP = BUILDER
                .comment("Chunks to drop from simulation/entity distance during render-distance mitigation.")
                .defineInRange("renderDistanceEntityDrop", 1, 0, 16);
        BUILDER.pop();

        CONFIG_SPEC = BUILDER.build();
    }

    private PerformanceMitigationConfig() {
    }

    public static PerformanceMitigationValues values() {
        try {
            return new PerformanceMitigationValues(
                    ENABLE_RENDER_DISTANCE_MITIGATION.get(),
                    RENDER_DISTANCE_FPS_THRESHOLD_MS.get(),
                    RENDER_DISTANCE_QUEUE_PRESSURE_THRESHOLD.get(),
                    RENDER_DISTANCE_VIEW_DROP.get(),
                    RENDER_DISTANCE_ENTITY_DROP.get()
            );
        } catch (IllegalStateException ex) {
            return defaultValues();
        }
    }

    public static PerformanceMitigationValues effectiveValues() {
        PerformanceMitigationValues base = values();
        if (!NovaAPIConfig.isModpackProfileEnabled()) {
            return base;
        }
        int fpsThreshold = Math.min(base.renderDistanceFpsThresholdMs(), 50);
        double queueThreshold = Math.min(base.renderDistanceQueuePressureThreshold(), 0.75D);
        return new PerformanceMitigationValues(
                base.renderDistanceEnabled(),
                fpsThreshold,
                queueThreshold,
                base.renderDistanceViewDrop(),
                base.renderDistanceEntityDrop()
        );
    }

    public static PerformanceMitigationValues defaultValues() {
        return new PerformanceMitigationValues(
                ENABLE_RENDER_DISTANCE_MITIGATION.getDefault(),
                RENDER_DISTANCE_FPS_THRESHOLD_MS.getDefault(),
                RENDER_DISTANCE_QUEUE_PRESSURE_THRESHOLD.getDefault(),
                RENDER_DISTANCE_VIEW_DROP.getDefault(),
                RENDER_DISTANCE_ENTITY_DROP.getDefault()
        );
    }

    public record PerformanceMitigationValues(
            boolean renderDistanceEnabled,
            int renderDistanceFpsThresholdMs,
            double renderDistanceQueuePressureThreshold,
            int renderDistanceViewDrop,
            int renderDistanceEntityDrop
    ) { }
}
