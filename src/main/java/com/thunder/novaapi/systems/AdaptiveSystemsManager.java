package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class AdaptiveSystemsManager {
    private final List<AdaptiveSystem> systems = List.of(
            new RegionIntelligenceSystem(),
            new MobEcologyBalancerSystem(),
            new RedstoneLoadMonitorSystem(),
            new LogisticsNetworkMonitorSystem(),
            new ChunkAwareAiSchedulerSystem(),
            new StructureIndexSystem(),
            new DynamicWeatherSchedulerSystem(),
            new SimulationDensitySystem(),
            new InventoryHeatSystem(),
            new EntitySleepMonitorSystem()
    );
    private final SafetyMonitor safetyMonitor = new SafetyMonitor();
    private long tickCounter = 0L;
    private long cooldownUntilTick = 0L;
    private boolean active = false;
    private boolean eventsRegistered = false;

    public void onServerStarting() {
        active = true;
        registerEvents();
    }

    public void onServerStopping() {
        active = false;
        cooldownUntilTick = 0L;
    }

    public void tick(MinecraftServer server) {
        if (!active) {
            return;
        }
        AdaptiveSystemsConfig.AdaptiveSystemsValues config = AdaptiveSystemsConfig.values();
        if (!config.enableAdaptiveSystems()) {
            return;
        }

        SafetyMonitor.SafetySnapshot snapshot = safetyMonitor.sample(config);
        if (snapshot.overTickBudget() || snapshot.memoryPressure()) {
            cooldownUntilTick = Math.max(cooldownUntilTick, tickCounter + config.safetyCooldownTicks());
        }
        boolean cooldownActive = tickCounter < cooldownUntilTick;
        SampleBudget budget = new SampleBudget(config.maxSamplesPerTick());

        SafetyContext context = new SafetyContext(
                config,
                budget,
                tickCounter,
                snapshot.tickTimeMs(),
                snapshot.usedMemoryMb(),
                snapshot.maxMemoryMb(),
                snapshot.freeMemoryMb(),
                snapshot.overTickBudget(),
                snapshot.memoryPressure(),
                cooldownActive
        );

        for (AdaptiveSystem system : systems) {
            if (!system.isEnabled(config)) {
                continue;
            }
            system.tick(server, context);
        }
        tickCounter++;
    }

    private void registerEvents() {
        if (eventsRegistered) {
            return;
        }
        for (AdaptiveSystem system : systems) {
            NeoForge.EVENT_BUS.register(system);
        }
        eventsRegistered = true;
    }
}
