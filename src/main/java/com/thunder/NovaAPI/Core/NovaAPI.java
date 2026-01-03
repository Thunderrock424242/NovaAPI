package com.thunder.NovaAPI.Core;

import com.thunder.NovaAPI.AI.AI_perf.PerformanceAdvisor;
import com.thunder.NovaAPI.AI.AI_perf.PerformanceAdvisoryRequest;
import com.thunder.NovaAPI.AI.AI_perf.PerformanceMitigationController;
import com.thunder.NovaAPI.analytics.AnalyticsTracker;
import com.thunder.NovaAPI.async.AsyncTaskManager;
import com.thunder.NovaAPI.async.AsyncThreadingConfig;
import com.thunder.NovaAPI.chunk.ChunkDeltaTracker;
import com.thunder.NovaAPI.chunk.ChunkStoragePaths;
import com.thunder.NovaAPI.chunk.ChunkStreamManager;
import com.thunder.NovaAPI.chunk.ChunkStreamingConfig;
import com.thunder.NovaAPI.chunk.DiskChunkStorageAdapter;
import com.thunder.NovaAPI.chunk.ChunkTickThrottler;
import com.thunder.NovaAPI.config.NovaAPIConfig;
import com.thunder.NovaAPI.io.BufferPool;
import com.thunder.NovaAPI.io.IoExecutors;
import com.thunder.NovaAPI.server.NovaAPIServerManager;
import com.thunder.NovaAPI.utils.ThreadMonitor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Mod(NovaAPI.MOD_ID)
public class NovaAPI {
    public static final Logger LOGGER = LogManager.getLogger("novaapi");
    public static final String MOD_ID = "novaapi";
    public static final String PLAYERUUID = "380df991-f603-344c-a090-369bad2a924a";

    private static final int LOG_INTERVAL = 200;

    private static Path chunkStorageRoot;
    private static long lastTickTimeNanos = 0L;
    private static long worstTickTimeNanos = 0L;
    private static int serverTickCounter = 0;

    public NovaAPI(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("NovaAPI initialized with async + chunk streaming pipeline.");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::onConfigLoaded);
        modEventBus.addListener(this::onConfigReloaded);

        container.registerConfig(ModConfig.Type.COMMON, NovaAPIConfig.CONFIG);
        container.registerConfig(ModConfig.Type.COMMON, AsyncThreadingConfig.CONFIG_SPEC, "novaapi-async.toml");
        container.registerConfig(ModConfig.Type.COMMON, ChunkStreamingConfig.CONFIG_SPEC, "novaapi-chunk-streaming.toml");

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        initializeAsyncAndChunkSystems(server);
        AnalyticsTracker.initialize(server, server.getFile("config"));
        ThreadMonitor.startMonitoring();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        if (NovaAPIConfig.ENABLE_DEDICATED_SERVER.get()) {
            boolean ok = NovaAPIServerManager.connectToDedicatedServer(
                    NovaAPIConfig.DEDICATED_SERVER_IP.get(),
                    server
            );
            if (!ok) {
                LOGGER.warn("[NovaAPI] Could not reach Dedicated Server; falling back to Local Mode.");
                startLocalServer();
            }
        } else {
            startLocalServer();
        }
    }

    private static void startLocalServer() {
        LOGGER.info("[NovaAPI] Starting in Local Mode...");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
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

        if (!event.hasTime()) {
            return;
        }
        if (++serverTickCounter >= LOG_INTERVAL) {
            serverTickCounter = 0;
            long worstTickMillis = TimeUnit.NANOSECONDS.toMillis(worstTickTimeNanos);
            worstTickTimeNanos = 0L;
            if (worstTickMillis > PerformanceAdvisor.DEFAULT_TICK_BUDGET_MS) {
                PerformanceAdvisoryRequest request = PerformanceAdvisor.observe(server, worstTickMillis);
                PerformanceMitigationController.buildActionsFromRequest(request);
                LOGGER.info("[NovaAPI][Advisor]\n{}", PerformanceAdvisor.buildLocalAdvice(request));
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

    public static void shutdown() {
        ThreadMonitor.stopMonitoring();
    }
}