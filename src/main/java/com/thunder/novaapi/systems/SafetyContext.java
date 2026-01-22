package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;

public record SafetyContext(
        AdaptiveSystemsConfig.AdaptiveSystemsValues config,
        SampleBudget sampleBudget,
        long tickCounter,
        long tickTimeMs,
        long usedMemoryMb,
        long maxMemoryMb,
        long freeMemoryMb,
        boolean overTickBudget,
        boolean memoryPressure,
        boolean cooldownActive
) {
    public boolean allowWork() {
        return !overTickBudget && !memoryPressure && !cooldownActive;
    }
}
