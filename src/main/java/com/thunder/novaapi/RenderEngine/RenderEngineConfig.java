package com.thunder.novaapi.RenderEngine;

import net.neoforged.neoforge.common.ModConfigSpec;
import com.thunder.novaapi.config.NovaAPIConfig;

public final class RenderEngineConfig {
    public static final ModConfigSpec CONFIG_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_OVERLAY_BATCHING;
    public static final ModConfigSpec.BooleanValue ENABLE_PARTICLE_CULLING;
    public static final ModConfigSpec.IntValue PARTICLE_CULL_DISTANCE;
    public static final ModConfigSpec.BooleanValue ENABLE_INSTANCED_RENDERING;
    public static final ModConfigSpec.DoubleValue INSTANCED_LOD0_DISTANCE;
    public static final ModConfigSpec.DoubleValue INSTANCED_LOD1_DISTANCE;

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

        builder.push("Instanced Rendering");
        ENABLE_INSTANCED_RENDERING = builder
                .comment("Enable instanced rendering for supported entity models.")
                .define("enableInstancedRendering", true);
        INSTANCED_LOD0_DISTANCE = builder
                .comment("Distance in blocks for LOD0 instanced models.")
                .defineInRange("instancedLod0Distance", 16.0D, 1.0D, 256.0D);
        INSTANCED_LOD1_DISTANCE = builder
                .comment("Distance in blocks for LOD1 instanced models.")
                .defineInRange("instancedLod1Distance", 48.0D, 1.0D, 512.0D);
        builder.pop();

        CONFIG_SPEC = builder.build();
    }

    private RenderEngineConfig() {
    }

    public static boolean isOverlayBatchingEnabled() {
        return NovaAPIConfig.isRenderEngineOptimizationsEnabled() && ENABLE_OVERLAY_BATCHING.get();
    }

    public static boolean isParticleCullingEnabled() {
        return NovaAPIConfig.isRenderEngineOptimizationsEnabled() && ENABLE_PARTICLE_CULLING.get();
    }

    public static int getParticleCullingDistance() {
        return PARTICLE_CULL_DISTANCE.get();
    }

    public static boolean isInstancedRenderingEnabled() {
        return NovaAPIConfig.isRenderEngineOptimizationsEnabled() && ENABLE_INSTANCED_RENDERING.get();
    }

    public static double getInstancedLod0Distance() {
        return INSTANCED_LOD0_DISTANCE.get();
    }

    public static double getInstancedLod1Distance() {
        return INSTANCED_LOD1_DISTANCE.get();
    }
}
