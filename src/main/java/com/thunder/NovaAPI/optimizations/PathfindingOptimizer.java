package com.thunder.NovaAPI.optimizations;

import com.thunder.NovaAPI.NovaAPI;
import com.thunder.NovaAPI.config.NovaAPIConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EventBusSubscriber
public class PathfindingOptimizer {
    private static ExecutorService threadPool;

    public static void initialize(int threadCount) {
        if (!NovaAPIConfig.ENABLE_AI_OPTIMIZATIONS.get()) return;

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow(); // Re-initialize if already set
        }

        threadPool = Executors.newFixedThreadPool(threadCount);
        NeoForge.EVENT_BUS.register(PathfindingOptimizer.class);
        NovaAPI.LOGGER.info("[Nova API] PathfindingOptimizer initialized with {} thread(s).", threadCount);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingUpdate(EntityTickEvent.Pre event) {
        if (!NovaAPIConfig.ENABLE_AI_OPTIMIZATIONS.get()) return;

        LivingEntity entity = event.getEntity().getControllingPassenger();
        if (!(entity instanceof Mob mob)) return;

        threadPool.submit(() -> optimizePathfinding(mob));
    }

    private static void optimizePathfinding(Mob mob) {
        PathNavigation navigator = mob.getNavigation();
        if (navigator == null || !navigator.isDone()) return;

        navigator.setCanFloat(false);
        navigator.setSpeedModifier(1.2f);

        NovaAPI.LOGGER.debug("[Nova API] Optimized pathfinding for {}", mob.getName().getString());
    }

    public static void shutdown() {
        if (threadPool != null) {
            threadPool.shutdownNow();
            NovaAPI.LOGGER.info("[Nova API] PathfindingOptimizer thread pool shut down.");
        }
    }
}