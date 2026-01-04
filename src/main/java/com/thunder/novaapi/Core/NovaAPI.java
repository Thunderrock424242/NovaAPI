package com.thunder.novaapi.Core;

import com.mojang.brigadier.CommandDispatcher;
import com.thunder.novaapi.AI.AI_perf.PerformanceAdvisor;
import com.thunder.novaapi.AI.AI_perf.PerformanceAdvisoryRequest;
import com.thunder.novaapi.AI.AI_perf.PerformanceMitigationController;
import com.thunder.novaapi.AI.AI_perf.requestperfadvice;
import com.thunder.novaapi.MemUtils.MemCheckCommand;
import com.thunder.novaapi.MemUtils.MemoryUtils;
import com.thunder.novaapi.analytics.AnalyticsTracker;
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
import com.thunder.novaapi.io.BufferPool;
import com.thunder.novaapi.io.IoExecutors;
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
    public static final String PLAYERUUID = "380df991-f603-344c-a090-369bad2a924a";

    private static final int LOG_INTERVAL = 600;
    public static int dynamicModCount = 0;
    private static final String CONFIG_FOLDER = NovaAPI.MOD_ID + "/";



    public static final RegionScopedCache<String> REGION_CACHE = new RegionScopedCache<>(512, 10 * 60 * 1000L);

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
                CONFIG_FOLDER + "wildernessodysseyapi-common.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, ModDataCacheConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "wildernessodysseyapi-cache.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, AsyncThreadingConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "wildernessodysseyapi-async.toml");

        ConfigRegistrationValidator.register(container, ModConfig.Type.COMMON, ChunkStreamingConfig.CONFIG_SPEC,
                CONFIG_FOLDER + "wildernessodysseyapi-chunk-streaming.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            System.out.println("Wilderness Odyssey setup complete!");
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
        AnalyticsTracker.initialize(server, server.getFile("config"));
        ThreadMonitor.startMonitoring();
        AsyncTaskManager.initialize(AsyncThreadingConfig.values());
        ChunkStreamingConfig.ChunkConfigValues chunkConfig = ChunkStreamingConfig.values();
        BufferPool.configure(chunkConfig);
        IoExecutors.initialize(chunkConfig);
        chunkStorageRoot = ChunkStoragePaths.resolveCacheRoot(event.getServer(), chunkConfig);
        ChunkStreamManager.initialize(chunkConfig, new DiskChunkStorageAdapter(chunkStorageRoot, chunkConfig.compressionLevel(), chunkConfig.compressionCodec()));
        ChunkDeltaTracker.configure(chunkConfig);
        AnalyticsTracker.initialize(event.getServer(), event.getServer().getFile("config"));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        LOGGER.info("[NovaAPI] Starting in Local Mode...");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        MemoryDebugCommand.register(event.getDispatcher());
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        MemCheckCommand.register(event.getDispatcher());
        AiAdvisorCommand.register(event.getDispatcher());
        AsyncStatsCommand.register(dispatcher);
        ChunkStatsCommand.register(dispatcher);
        DebugChunkCommand.register(dispatcher);
        AnalyticsCommand.register(dispatcher);
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
        for (ServerLevel level : server.getAllLevels()) {
            ChunkTickThrottler.tick(level);
        }
        AsyncTaskManager.drainMainThreadQueue(server);
        if (server.overworld() != null) {
            ChunkStreamManager.tick(server.overworld().getGameTime());
        }
        PerformanceMitigationController.tick(server);
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
            if (worstTickMillis > PerformanceAdvisor.DEFAULT_TICK_BUDGET_MS) {
                PerformanceAdvisoryRequest request = PerformanceAdvisor.observe(server, worstTickMillis);
                PerformanceMitigationController.buildActionsFromRequest(request);
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
        AnalyticsTracker.shutdown();
        shutdown();
        REGION_CACHE.clear();
        BackgroundTaskScheduler.shutdown();
    }

    private void initializeAsyncAndChunkSystems(MinecraftServer server) {
        AsyncTaskManager.initialize(AsyncThreadingConfig.values());
        ChunkStreamingConfig.ChunkConfigValues chunkConfig = ChunkStreamingConfig.values();
        BufferPool.configure(chunkConfig);
        IoExecutors.initialize(chunkConfig);
        chunkStorageRoot = ChunkStoragePaths.resolveCacheRoot(server, chunkConfig);
        ChunkStreamManager.initialize(chunkConfig, new DiskChunkStorageAdapter(chunkStorageRoot, chunkConfig.compressionLevel(), chunkConfig.compressionCodec()));
        ChunkDeltaTracker.configure(chunkConfig);
    }

    public void onConfigLoaded(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ModDataCacheConfig.CONFIG_SPEC) {
            ModDataCache.initialize();
        }
        if (event.getConfig().getSpec() == AsyncThreadingConfig.CONFIG_SPEC) {
            AsyncTaskManager.initialize(AsyncThreadingConfig.values());
        }
        if (event.getConfig().getSpec() == ChunkStreamingConfig.CONFIG_SPEC && chunkStorageRoot != null) {
            ChunkStreamingConfig.ChunkConfigValues chunkConfig = ChunkStreamingConfig.values();
            BufferPool.configure(chunkConfig);
            IoExecutors.initialize(chunkConfig);
            ChunkStreamManager.initialize(chunkConfig, new DiskChunkStorageAdapter(chunkStorageRoot, chunkConfig.compressionLevel(), chunkConfig.compressionCodec()));
            ChunkDeltaTracker.configure(chunkConfig);
        }
    }

    public void onConfigReloaded(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModDataCacheConfig.CONFIG_SPEC) {
            ModDataCache.initialize();
        }
        if (event.getConfig().getSpec() == AsyncThreadingConfig.CONFIG_SPEC) {
            AsyncTaskManager.initialize(AsyncThreadingConfig.values());
        }
        if (event.getConfig().getSpec() == ChunkStreamingConfig.CONFIG_SPEC && chunkStorageRoot != null) {
            ChunkStreamingConfig.ChunkConfigValues chunkConfig = ChunkStreamingConfig.values();
            BufferPool.configure(chunkConfig);
            IoExecutors.initialize(chunkConfig);
            ChunkStreamManager.initialize(chunkConfig, new DiskChunkStorageAdapter(chunkStorageRoot, chunkConfig.compressionLevel(), chunkConfig.compressionCodec()));
            ChunkDeltaTracker.configure(chunkConfig);
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
