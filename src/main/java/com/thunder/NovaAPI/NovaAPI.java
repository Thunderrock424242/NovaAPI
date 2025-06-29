package com.thunder.NovaAPI;

import com.thunder.NovaAPI.chunk.ChunkPreloader;
import com.thunder.NovaAPI.config.NovaAPIConfig;
import com.thunder.NovaAPI.server.NovaAPIServerManager;
import com.thunder.NovaAPI.utils.ThreadMonitor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod(NovaAPI.MOD_ID)

public class NovaAPI {
    /**
     * The constant LOGGER.
     */
    public static final Logger LOGGER = LogManager.getLogger("novaapi");

    public static final String PLAYERUUID = "380df991-f603-344c-a090-369bad2a924a";


    /**
     * The constant MOD_ID.
     */
    public static final String MOD_ID = "novaapi";
    private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

    private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader,
                                                                 IPayloadHandler<T> handler) {
    }

    /**
     * Instantiates a new Wilderness odyssey api main mod class.
     *
     * @param modEventBus the mod event bus
     * @param container   the container
     */
    public NovaAPI(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("WildernessOdysseyAPI initialized. I will also start to track mod conflicts");
        // Register mod setup and creative tabs
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        container.registerConfig(ModConfig.Type.COMMON, NovaAPIConfig.CONFIG);


        // Register global events
        NeoForge.EVENT_BUS.register(this);


    }

    private void commonSetup(final FMLCommonSetupEvent event) {


    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    /**
     * On server starting.
     *
     * @param event the event
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

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
                LOGGER.warn("[Nova API] Could not reach Dedicated Server; falling back to Local Mode.");
                startLocalServer();
            }
            // if ok, we remain in Dedicated Mode and skip local init
        } else {
            startLocalServer();
        }
    }
    private static void startLocalServer() {
        LOGGER.info("[Nova API] Starting in Local Mode...");
        // chunk, AI, async registration as before...
    }

    /**
     * On register commands.
     *
     * @param event the event
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {

    }


    /**
     * On server stopping.
     *
     * @param event the event
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ChunkPreloader.shutdown();
        NovaAPI.shutdown(); // Stop monitoring when server shuts down
    }

    public static void initialize() {
        ThreadMonitor.startMonitoring(); // Start automatic monitoring
    }

    public static void shutdown() {
        ThreadMonitor.stopMonitoring(); // Stop monitoring on game exit
    }
}




