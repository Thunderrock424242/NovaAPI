package com.thunder.novaapi.systems;

import com.thunder.novaapi.MemUtils.MemoryUtils;
import com.thunder.novaapi.config.AdaptiveSystemsConfig;

public class SafetyMonitor {
    private long lastTickNanos = 0L;
    private long lastTickDurationMs = 0L;

    public SafetySnapshot sample(AdaptiveSystemsConfig.AdaptiveSystemsValues config) {
        long now = System.nanoTime();
        if (lastTickNanos != 0L) {
            lastTickDurationMs = (now - lastTickNanos) / 1_000_000L;
        }
        lastTickNanos = now;

        long usedMb = MemoryUtils.getUsedMemoryMB();
        long maxMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        long freeMb = Math.max(0L, maxMb - usedMb);
        boolean overTickBudget = lastTickDurationMs > config.safetyTickBudgetMs();
        boolean memoryPressure = freeMb < config.safetyMinFreeMemoryMb();

        return new SafetySnapshot(lastTickDurationMs, usedMb, maxMb, freeMb, overTickBudget, memoryPressure);
    }

    public record SafetySnapshot(
            long tickTimeMs,
            long usedMemoryMb,
            long maxMemoryMb,
            long freeMemoryMb,
            boolean overTickBudget,
            boolean memoryPressure
    ) {
    }
}
