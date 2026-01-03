package com.thunder.novaapi;

import com.thunder.novaapi.cache.RegionScopedCache;
import com.thunder.novaapi.command.MemoryDebugCommand;
import com.thunder.novaapi.task.BackgroundTaskScheduler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(NovaAPI.MOD_ID)
public class NovaAPI {
    public static final String MOD_ID = "novaapi";

    /**
     * Shared cache for dimension + chunk scoped data. This cache is capped, TTL-governed, and unload-aware.
     */
    public static final RegionScopedCache<String> REGION_CACHE = new RegionScopedCache<>(512, 10 * 60 * 1000L);

    public NovaAPI(@SuppressWarnings("unused") net.neoforged.bus.api.IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        MemoryDebugCommand.register(event.getDispatcher());
    }

    private void onChunkUnload(ChunkEvent.Unload event) {
        LevelAccessor level = event.getLevel();
        if (level instanceof Level fullLevel) {
            ResourceKey<Level> dimension = fullLevel.dimension();
            ChunkPos chunkPos = event.getChunk().getPos();
            REGION_CACHE.remove(dimension, chunkPos);
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        REGION_CACHE.clear();
        BackgroundTaskScheduler.shutdown();
    }
}
