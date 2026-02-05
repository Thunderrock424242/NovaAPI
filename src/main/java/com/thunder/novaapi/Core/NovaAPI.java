package com.thunder.novaapi.Core;

import com.mojang.brigadier.CommandDispatcher;
import com.thunder.novaapi.AI.AI_perf.PerformanceAdvisor;
import com.thunder.novaapi.AI.AI_perf.PerformanceAdvisoryRequest;
import com.thunder.novaapi.AI.AI_perf.PerformanceMitigationController;
import com.thunder.novaapi.AI.AI_perf.requestperfadvice;
import com.thunder.novaapi.MemUtils.MemCheckCommand;
import com.thunder.novaapi.MemUtils.MemoryUtils;
import com.thunder.novaapi.async.AsyncTaskManager;
import com.thunder.novaapi.async.AsyncThreadingConfig;
import com.thunder.novaapi.cache.ModDataCache;
import com.thunder.novaapi.cache.ModDataCacheConfig;
import com.thunder.novaapi.cache.RegionScopedCache;
import com.thunder.novaapi.chunk.ChunkDeltaTracker;
import com.thunder.novaapi.chunk.ChunkStoragePaths;
import com.thunder.novaapi.chunk.ChunkStreamManager;
import com.thunder.novaapi.chunk.ChunkStreamingConfig;
import com.thunder.novaapi.chunk.DiskChunkStorageAdapter;
import com.thunder.novaapi.chunk.ChunkTickThrottler;
import com.thunder.novaapi.command.*;
import com.thunder.novaapi.config.ConfigRegistrationValidator;
import com.thunder.novaapi.config.NovaAPIConfig;
import com.thunder.novaapi.config.PerformanceMitigationConfig;
import com.thunder.novaapi.RenderEngine.RenderEngineConfig;
import com.thunder.novaapi.io.BufferPool;
import com.thunder.novaapi.io.IoExecutors;
import com.thunder.novaapi.resourcepack.ResourcePackOptimizationConfig;
import com.thunder.novaapi.task.BackgroundTaskScheduler;
import com.thunder.novaapi.utils.ThreadMonitor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Mod(NovaAPI.MOD_ID)
public class NovaAPI {
    public static final Logger LOGGER = LogManager.getLogger("novaapi");
    public static final String MOD_ID = "novaapi";

    private static final int LOG_INTERVAL = 600;
    public static int dynamicModCount = 0;
    private static final String CONFIG_FOLDER = NovaAPI.MOD_ID + "/";



    public static final RegionScopedCache<String> REGION_CACHE = new RegionScopedCache<>(512, 10 * 60 * 1000L);
    private boolean asyncInitialized = false;
    private boolean chunkStreamingInitialized = false;

    private static Path chunkStorageRoot;
    private static long lastTickTimeNanos = 0L;
    private static long worstTickTimeNanos = 0L;
    private static int serverTickCounter = 0;
    private final requestperfadvice requestperfadvice = new requestperfadvice();

    public NovaAPI(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("NovaAPI initialized with async + chunk streaming pipeline.");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);

        NeoForge.EVENT_BUS.register(this);

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, NovaAPIConfig.CONFIG,
                CONFIG_FOLDER + "novaapi-common.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, ModDataCacheConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "novaapi-cache.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, AsyncThreadingConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "novaapi-async.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, ChunkStreamingConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "novaapi-chunk-streaming.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, PerformanceMitigationConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "novaapi-performance-mitigation.toml");
        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, RenderEngineConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "novaapi-rendering.toml");
        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, ResourcePackOptimizationConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "novaapi-resourcepacks.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            System.out.println("Nova API setup complete!");
            ModDataCache.initialize();
        });
        dynamicModCount = ModList.get().getMods().size();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        initializeAsyncAndChunkSystems(server);
        ThreadMonitor.startMonitoring();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        LOGGER.info("[NovaAPI] Starting in Local Mode...");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        if (!NovaAPIConfig.isDebugCommandsEnabled()) {
            LOGGER.info("[NovaAPI] Debug commands disabled by config.");
            return;
        }
        MemoryDebugCommand.register(event.getDispatcher());
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        MemCheckCommand.register(event.getDispatcher());
        AiAdvisorCommand.register(event.getDispatcher());
        AsyncStatsCommand.register(dispatcher);
        ChunkStatsCommand.register(dispatcher);
        DebugChunkCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChunkDeltaTracker.dropPlayer(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Every server tick event
        // This is equivalent to the old "END" phase.
        MinecraftServer server = event.getServer();
        long now = System.nanoTime();
        if (lastTickTimeNanos != 0L) {
            long duration = now - lastTickTimeNanos;
            worstTickTimeNanos = Math.max(worstTickTimeNanos, duration);
        }
        lastTickTimeNanos = now;
        if (NovaAPIConfig.isChunkOptimizationsEnabled()) {
            for (ServerLevel level : server.getAllLevels()) {
                ChunkTickThrottler.tick(level);
            }
        }
        AsyncTaskManager.drainMainThreadQueue(server);
        if (server.overworld() != null) {
            ChunkStreamManager.tick(server.overworld().getGameTime());
        }
        if (NovaAPIConfig.isAutomaticPerformanceMitigationsEnabled()) {
            PerformanceMitigationController.tick(server);
        }
        if (!event.hasTime()) return;

        if (++serverTickCounter >= LOG_INTERVAL) {
            serverTickCounter = 0;
            long usedMB = MemoryUtils.getUsedMemoryMB();
            long totalMB = MemoryUtils.getTotalMemoryMB();

            // Use the dynamic mod count
            int recommendedMB = MemoryUtils.calculateRecommendedRAM(usedMB, dynamicModCount);

            LOGGER.info("[ResourceManager] Memory usage: {}MB / {}MB. Recommended ~{}MB for {} loaded mods.", (Object) Optional.of(usedMB), (Object) totalMB, (Object) recommendedMB, (Object) dynamicModCount);

            long worstTickMillis = TimeUnit.NANOSECONDS.toMillis(worstTickTimeNanos);
            worstTickTimeNanos = 0L;
            if (NovaAPIConfig.isAiPerformanceAdvisorEnabled()
                    && worstTickMillis > PerformanceAdvisor.DEFAULT_TICK_BUDGET_MS) {
                PerformanceAdvisoryRequest request = PerformanceAdvisor.observe(server, worstTickMillis);
                if (NovaAPIConfig.isAutomaticPerformanceMitigationsEnabled()) {
                    PerformanceMitigationController.buildActionsFromRequest(request);
                }
                String advisory = requestperfadvice.requestPerformanceAdvice(request);
                LOGGER.info("[AI Advisor] {}", advisory);
            }
        }
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            ChunkStreamManager.flushAll(serverLevel.getGameTime());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        long gameTime = event.getServer().overworld() != null ? event.getServer().overworld().getGameTime() : 0L;
        ChunkStreamManager.flushAll(gameTime);
        AsyncTaskManager.shutdown();
        ChunkStreamManager.shutdown();
        IoExecutors.shutdown();
        ChunkDeltaTracker.shutdown();
        shutdown();
        REGION_CACHE.clear();
        BackgroundTaskScheduler.shutdown();
    }

    private void restartAsyncSystems() {
        if (asyncInitialized) {
            AsyncTaskManager.shutdown();
        }
        AsyncTaskManager.initialize(resolveAsyncConfig());
        asyncInitialized = true;
    }

    private void restartChunkStreaming() {
        if (!chunkStreamingInitialized || chunkStorageRoot == null) {
            return;
        }
        ChunkStreamingConfig.ChunkConfigValues chunkConfig = resolveChunkConfig();
        ChunkStreamManager.shutdown();
        IoExecutors.shutdown();
        BufferPool.configure(chunkConfig);
        IoExecutors.initialize(chunkConfig);
        ChunkStreamManager.initialize(chunkConfig, new DiskChunkStorageAdapter(chunkStorageRoot, chunkConfig.compressionLevel(), chunkConfig.compressionCodec()));
        ChunkDeltaTracker.configure(chunkConfig);
        chunkStreamingInitialized = true;
    }

    private void initializeAsyncAndChunkSystems(MinecraftServer server) {
        AsyncTaskManager.initialize(resolveAsyncConfig());
        asyncInitialized = true;
        ChunkStreamingConfig.ChunkConfigValues chunkConfig = resolveChunkConfig();
        BufferPool.configure(chunkConfig);
        IoExecutors.initialize(chunkConfig);
        chunkStorageRoot = ChunkStoragePaths.resolveCacheRoot(server, chunkConfig);
        ChunkStreamManager.initialize(chunkConfig, new DiskChunkStorageAdapter(chunkStorageRoot, chunkConfig.compressionLevel(), chunkConfig.compressionCodec()));
        ChunkDeltaTracker.configure(chunkConfig);
        chunkStreamingInitialized = true;
    }

    private static AsyncThreadingConfig.AsyncConfigValues resolveAsyncConfig() {
        AsyncThreadingConfig.AsyncConfigValues base = AsyncThreadingConfig.values();
        if (!NovaAPIConfig.isModpackProfileEnabled()) {
            return base;
        }
        int hardwareThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        int maxThreads = Math.max(1, Math.min(base.maxThreads(), Math.max(1, hardwareThreads / 3)));
        int queueSize = Math.min(base.queueSize(), 128);
        int applyPerTick = Math.min(base.applyPerTick(), 32);
        int taskTimeoutMs = base.taskTimeoutMs();
        return new AsyncThreadingConfig.AsyncConfigValues(
                base.enabled(),
                maxThreads,
                queueSize,
                applyPerTick,
                taskTimeoutMs,
                base.debugLogging()
        );
    }

    private static ChunkStreamingConfig.ChunkConfigValues resolveChunkConfig() {
        ChunkStreamingConfig.ChunkConfigValues base = ChunkStreamingConfig.values();
        if (!NovaAPIConfig.isChunkOptimizationsEnabled()) {
            return new ChunkStreamingConfig.ChunkConfigValues(
                    false,
                    base.hotCacheLimit(),
                    base.warmCacheLimit(),
                    base.splitWarmCache(),
                    base.saveDebounceTicks(),
                    base.playerTicketTtl(),
                    base.entityTicketTtl(),
                    base.redstoneTicketTtl(),
                    base.structureTicketTtl(),
                    base.maxParallelIo(),
                    base.compressionLevel(),
                    base.compressionCodec(),
                    base.perDimensionExecutors(),
                    base.ioThreads(),
                    base.ioQueueSize(),
                    base.bufferSliceBytes(),
                    base.bufferSlicesPerThread(),
                    base.skipWarmCacheTicking(),
                    base.fluidRedstoneThrottleRadius(),
                    base.fluidRedstoneThrottleInterval(),
                    base.randomTickMinScale(),
                    base.randomTickMaxScale(),
                    base.movementSpeedForMaxScale(),
                    base.randomTickPlayerBand(),
                    base.sliceInternLimit(),
                    base.deltaChangeBudget(),
                    base.lightCompressionLevel(),
                    base.writeFlushIntervalTicks(),
                    base.maxMeshRebuildsPerTick(),
                    base.meshUploadBatchSize(),
                    base.maxPendingMeshRebuilds(),
                    base.maxPendingMeshUploads(),
                    base.cacheFolderName(),
                    base.storeCacheInWorldConfig()
            );
        }

        int hardwareThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        boolean modpackProfile = NovaAPIConfig.isModpackProfileEnabled();

        int ioThreads = base.ioThreads();
        int maxParallelIo = base.maxParallelIo();
        int ioQueueSize = base.ioQueueSize();
        int hotCacheLimit = base.hotCacheLimit();
        int warmCacheLimit = base.warmCacheLimit();
        int playerTicketTtl = base.playerTicketTtl();
        int entityTicketTtl = base.entityTicketTtl();
        int redstoneTicketTtl = base.redstoneTicketTtl();
        int structureTicketTtl = base.structureTicketTtl();
        int bufferSlicesPerThread = base.bufferSlicesPerThread();
        int maxMeshRebuildsPerTick = base.maxMeshRebuildsPerTick();
        int meshUploadBatchSize = base.meshUploadBatchSize();
        int maxPendingMeshRebuilds = base.maxPendingMeshRebuilds() == 0 ? 0 : base.maxPendingMeshRebuilds();
        int maxPendingMeshUploads = base.maxPendingMeshUploads() == 0 ? 0 : base.maxPendingMeshUploads();

        if (!NovaAPIConfig.isAsyncChunkLoadingEnabled()) {
            ioThreads = 1;
            maxParallelIo = 1;
            ioQueueSize = Math.min(ioQueueSize, 64);
        }

        if (!NovaAPIConfig.isSmartChunkRetentionEnabled()) {
            hotCacheLimit = Math.min(hotCacheLimit, 32);
            warmCacheLimit = Math.min(warmCacheLimit, 64);
            playerTicketTtl = Math.min(playerTicketTtl, 40);
            entityTicketTtl = Math.min(entityTicketTtl, 40);
            redstoneTicketTtl = Math.min(redstoneTicketTtl, 20);
            structureTicketTtl = Math.min(structureTicketTtl, 80);
        }

        if (modpackProfile) {
            ioThreads = Math.max(1, Math.min(ioThreads, Math.max(1, hardwareThreads / 6)));
            hotCacheLimit = Math.min(hotCacheLimit, 96);
            warmCacheLimit = Math.min(warmCacheLimit, 192);
            maxParallelIo = Math.min(maxParallelIo, 2);
            ioQueueSize = Math.min(ioQueueSize, 96);
            bufferSlicesPerThread = Math.min(bufferSlicesPerThread, 4);
            maxMeshRebuildsPerTick = Math.min(maxMeshRebuildsPerTick, 2);
            meshUploadBatchSize = Math.min(meshUploadBatchSize, 8);
            maxPendingMeshRebuilds = maxPendingMeshRebuilds == 0 ? 0 : Math.min(maxPendingMeshRebuilds, 1024);
            maxPendingMeshUploads = maxPendingMeshUploads == 0 ? 0 : Math.min(maxPendingMeshUploads, 2048);
        }

        return new ChunkStreamingConfig.ChunkConfigValues(
                base.enabled(),
                hotCacheLimit,
                warmCacheLimit,
                base.splitWarmCache(),
                base.saveDebounceTicks(),
                playerTicketTtl,
                entityTicketTtl,
                redstoneTicketTtl,
                structureTicketTtl,
                maxParallelIo,
                base.compressionLevel(),
                base.compressionCodec(),
                base.perDimensionExecutors(),
                ioThreads,
                ioQueueSize,
                base.bufferSliceBytes(),
                bufferSlicesPerThread,
                base.skipWarmCacheTicking(),
                base.fluidRedstoneThrottleRadius(),
                base.fluidRedstoneThrottleInterval(),
                base.randomTickMinScale(),
                base.randomTickMaxScale(),
                base.movementSpeedForMaxScale(),
                base.randomTickPlayerBand(),
                base.sliceInternLimit(),
                base.deltaChangeBudget(),
                base.lightCompressionLevel(),
                base.writeFlushIntervalTicks(),
                maxMeshRebuildsPerTick,
                meshUploadBatchSize,
                maxPendingMeshRebuilds,
                maxPendingMeshUploads,
                base.cacheFolderName(),
                base.storeCacheInWorldConfig()
        );
    }

    public void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModDataCacheConfig.CONFIG_SPEC) {
            ModDataCache.initialize();
        }
        if (event.getConfig().getSpec() == AsyncThreadingConfig.CONFIG_SPEC) {
            restartAsyncSystems();
        }
        if (event.getConfig().getSpec() == ChunkStreamingConfig.CONFIG_SPEC && chunkStorageRoot != null) {
            restartChunkStreaming();
        }
    }

    public void onConfigReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModDataCacheConfig.CONFIG_SPEC) {
            ModDataCache.initialize();
        }
        if (event.getConfig().getSpec() == AsyncThreadingConfig.CONFIG_SPEC) {
            restartAsyncSystems();
        }
        if (event.getConfig().getSpec() == ChunkStreamingConfig.CONFIG_SPEC && chunkStorageRoot != null) {
            restartChunkStreaming();
        }
    }

    @SubscribeEvent
    private void onChunkUnload(ChunkEvent.Unload event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof Level fullLevel) {
            ResourceKey<Level> dimension = fullLevel.dimension();
            ChunkPos chunkPos = event.getChunk().getPos();
            REGION_CACHE.remove(dimension, chunkPos);
        }
    }

    public static void shutdown() {
        ThreadMonitor.stopMonitoring();
    }
}
