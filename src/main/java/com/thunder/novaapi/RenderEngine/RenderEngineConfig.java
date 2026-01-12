package com.thunder.novaapi.RenderEngine;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RenderEngineConfig {
    public static final ModConfigSpec CONFIG_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_OVERLAY_BATCHING;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLE_CULLING;
    public static final ModConfigSpec.IntValue PARTICLE_CULL_DISTANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Overlay Rendering");
        ENABLE_OVERLAY_BATCHING = builder
                .comment("Group overlay draw calls by texture to reduce state changes.")
                .define("enableOverlayBatching", true);
        builder.pop();

        builder.push("Particle Rendering");
        ENABLE_PARTICLE_CULLING = builder
                .comment("Cull modded particle submissions based on camera distance and frustum visibility.")
                .define("enableParticleCulling", true);
        PARTICLE_CULL_DISTANCE = builder
                .comment("Maximum distance in blocks for modded particle submissions before culling.")
                .defineInRange("particleCullingDistance", 128, 0, 4096);
        builder.pop();

        CONFIG_SPEC = builder.build();
    }

    private RenderEngineConfig() {
    }

    public static boolean isOverlayBatchingEnabled() {
        return ENABLE_OVERLAY_BATCHING.get();
    }

    public static boolean isParticleCullingEnabled() {
        return ENABLE_PARTICLE_CULLING.get();
    }

    public static int getParticleCullingDistance() {
        return PARTICLE_CULL_DISTANCE.get();
    }
}
