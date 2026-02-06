package com.thunder.novaapi.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class NovaAPIConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec CONFIG;

    // 🔹 General Settings
    public static final ModConfigSpec.BooleanValue ENABLE_NOVA_API;
    public static final ModConfigSpec.BooleanValue MODPACK_RESOURCE_PROFILE;
    public static final ModConfigSpec.BooleanValue ENABLE_MEMORY_THREAD_LOGS;
    public static final ModConfigSpec.BooleanValue ENABLE_DEBUG_COMMANDS;
    public static final ModConfigSpec.BooleanValue ENABLE_AI_PERFORMANCE_ADVISOR;
    public static final ModConfigSpec.BooleanValue ENABLE_AUTOMATIC_PERFORMANCE_MITIGATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_ASYNC_TASKS;
    public static final ModConfigSpec.BooleanValue ENABLE_CHUNK_STREAMING;
    public static final ModConfigSpec.BooleanValue ENABLE_MOD_DATA_CACHE;
    public static final ModConfigSpec.BooleanValue ENABLE_RESOURCEPACK_OPTIMIZATIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_RENDER_ENGINE_OPTIMIZATIONS;

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
        ENABLE_MEMORY_THREAD_LOGS = BUILDER
                .comment("Enable debug logs for memory usage and thread counts.")
                .define("enableMemoryThreadLogs", false);
        ENABLE_DEBUG_COMMANDS = BUILDER
                .comment("Register NovaAPI debug and profiling commands. Disable on production servers to reduce overhead.")
                .define("enableDebugCommands", false);
        ENABLE_AI_PERFORMANCE_ADVISOR = BUILDER
                .comment("Enable periodic AI performance advisor analysis and recommendation logging.")
                .define("enableAiPerformanceAdvisor", false);
        ENABLE_AUTOMATIC_PERFORMANCE_MITIGATIONS = BUILDER
                .comment("Enable automatic mitigation tick processing (pathfinding throttle, distance adjustments, etc.).")
                .define("enableAutomaticPerformanceMitigations", true);
        BUILDER.pop();

        BUILDER.push("Feature Toggles");
        ENABLE_ASYNC_TASKS = BUILDER
                .comment("Enable the async task system (worker threads, task queues).")
                .define("enableAsyncTasks", true);
        ENABLE_CHUNK_STREAMING = BUILDER
                .comment("Enable the chunk streaming pipeline and related optimizations.")
                .define("enableChunkStreaming", true);
        ENABLE_MOD_DATA_CACHE = BUILDER
                .comment("Enable the persistent mod data cache for downloadable content.")
                .define("enableModDataCache", true);
        ENABLE_RESOURCEPACK_OPTIMIZATIONS = BUILDER
                .comment("Enable NovaAPI resource pack optimization features.")
                .define("enableResourcepackOptimizations", true);
        ENABLE_RENDER_ENGINE_OPTIMIZATIONS = BUILDER
                .comment("Enable NovaAPI render engine optimizations (batching, culling, instancing).")
                .define("enableRenderEngineOptimizations", true);
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
                .define("enableOcclusionCulling", false);
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

    public static boolean isNovaApiEnabled() {
        try {
            return ENABLE_NOVA_API.get();
        } catch (IllegalStateException ex) {
            return ENABLE_NOVA_API.getDefault();
        }
    }

    public static boolean isAsyncSystemEnabled() {
        try {
            return isNovaApiEnabled() && ENABLE_ASYNC_TASKS.get();
        } catch (IllegalStateException ex) {
            return isNovaApiEnabled() && ENABLE_ASYNC_TASKS.getDefault();
        }
    }

    public static boolean isChunkStreamingEnabled() {
        try {
            return isNovaApiEnabled() && ENABLE_CHUNK_STREAMING.get();
        } catch (IllegalStateException ex) {
            return isNovaApiEnabled() && ENABLE_CHUNK_STREAMING.getDefault();
        }
    }

    public static boolean isModDataCacheEnabled() {
        try {
            return isNovaApiEnabled() && ENABLE_MOD_DATA_CACHE.get();
        } catch (IllegalStateException ex) {
            return isNovaApiEnabled() && ENABLE_MOD_DATA_CACHE.getDefault();
        }
    }

    public static boolean isResourcePackOptimizationsEnabled() {
        try {
            return isNovaApiEnabled() && ENABLE_RESOURCEPACK_OPTIMIZATIONS.get();
        } catch (IllegalStateException ex) {
            return isNovaApiEnabled() && ENABLE_RESOURCEPACK_OPTIMIZATIONS.getDefault();
        }
    }

    public static boolean isRenderEngineOptimizationsEnabled() {
        try {
            return isNovaApiEnabled() && ENABLE_RENDER_ENGINE_OPTIMIZATIONS.get();
        } catch (IllegalStateException ex) {
            return isNovaApiEnabled() && ENABLE_RENDER_ENGINE_OPTIMIZATIONS.getDefault();
        }
    }

    public static boolean isMemoryThreadLogsEnabled() {
        try {
            return ENABLE_MEMORY_THREAD_LOGS.get();
        } catch (IllegalStateException ex) {
            return ENABLE_MEMORY_THREAD_LOGS.getDefault();
        }
    }


    public static boolean isChunkOptimizationsEnabled() {
        try {
            return ENABLE_CHUNK_OPTIMIZATIONS.get();
        } catch (IllegalStateException ex) {
            return ENABLE_CHUNK_OPTIMIZATIONS.getDefault();
        }
    }

    public static boolean isAsyncChunkLoadingEnabled() {
        try {
            return ASYNC_CHUNK_LOADING.get();
        } catch (IllegalStateException ex) {
            return ASYNC_CHUNK_LOADING.getDefault();
        }
    }

    public static boolean isSmartChunkRetentionEnabled() {
        try {
            return SMART_CHUNK_RETENTION.get();
        } catch (IllegalStateException ex) {
            return SMART_CHUNK_RETENTION.getDefault();
        }
    }

    public static boolean isDebugCommandsEnabled() {
        try {
            return ENABLE_DEBUG_COMMANDS.get();
        } catch (IllegalStateException ex) {
            return ENABLE_DEBUG_COMMANDS.getDefault();
        }
    }

    public static boolean isAiPerformanceAdvisorEnabled() {
        try {
            return ENABLE_AI_PERFORMANCE_ADVISOR.get();
        } catch (IllegalStateException ex) {
            return ENABLE_AI_PERFORMANCE_ADVISOR.getDefault();
        }
    }

    public static boolean isAutomaticPerformanceMitigationsEnabled() {
        try {
            return ENABLE_AUTOMATIC_PERFORMANCE_MITIGATIONS.get();
        } catch (IllegalStateException ex) {
            return ENABLE_AUTOMATIC_PERFORMANCE_MITIGATIONS.getDefault();
        }
    }
}
