package com.thunder.novaapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NovaAPIConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec CONFIG;

    // 🔹 General Settings
    public static final ModConfigSpec.BooleanValue ENABLE_NOVA_API;

    // 🔹 Chunk Optimization Settings
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_OPTIMIZATIONS;
    public static final ModConfigSpec.BooleanValue ASYNC_CHUNK_LOADING;
    public static final ModConfigSpec.BooleanValue SMART_CHUNK_RETENTION;

    static {
        BUILDER.push("General Settings");
        ENABLE_NOVA_API = BUILDER
                .comment("Enable Nova API. If false, all features are disabled.")
                .define("enableNovaAPI", true);
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

        CONFIG = BUILDER.build();
    }
}
