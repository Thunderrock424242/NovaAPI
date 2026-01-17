package com.thunder.novaapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NovaAPIConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec CONFIG;

    // 🔹 General Settings
    public static final ModConfigSpec.BooleanValue ENABLE_NOVA_API;
    public static final ModConfigSpec.BooleanValue MODPACK_RESOURCE_PROFILE;

    // 🔹 Chunk Optimization Settings
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_OPTIMIZATIONS;
    public static final ModConfigSpec.BooleanValue ASYNC_CHUNK_LOADING;
    public static final ModConfigSpec.BooleanValue SMART_CHUNK_RETENTION;

    // 🔹 Render Culling Settings
    public static final ModConfigSpec.BooleanValue ENABLE_OCCLUSION_CULLING;

    static {
        BUILDER.push("General Settings");
        ENABLE_NOVA_API = BUILDER
                .comment("Enable Nova API. If false, all features are disabled.")
                .define("enableNovaAPI", true);
        MODPACK_RESOURCE_PROFILE = BUILDER
                .comment("Enable modpack-friendly tuning to lower CPU and memory usage (auto-adjusts Nova API internal limits).")
                .define("modpackResourceProfile", false);
        BUILDER.pop();

        BUILDER.push("Chunk Optimization Settings");
        ENABLE_CHUNK_OPTIMIZATIONS = BUILDER
                .comment("Enable optimized chunk loading and retention.")
                .define("enableChunkOptimizations", true);
        ASYNC_CHUNK_LOADING = BUILDER
                .comment("Use multi-threaded chunk loading to reduce lag.")
                .define("asyncChunkLoading", true);
        SMART_CHUNK_RETENTION = BUILDER
                .comment("Keep frequently accessed chunks loaded for longer to prevent constant reloads.")
                .define("smartChunkRetention", true);
        BUILDER.pop();

        BUILDER.push("Render Culling Settings");
        ENABLE_OCCLUSION_CULLING = BUILDER
                .comment("Enable occlusion culling for entity instanced rendering. Disable if compatibility issues arise.")
                .define("enableOcclusionCulling", true);
        BUILDER.pop();

        CONFIG = BUILDER.build();
    }

    public static boolean isModpackProfileEnabled() {
        try {
            return MODPACK_RESOURCE_PROFILE.get();
        } catch (IllegalStateException ex) {
            return MODPACK_RESOURCE_PROFILE.getDefault();
        }
    }
}
