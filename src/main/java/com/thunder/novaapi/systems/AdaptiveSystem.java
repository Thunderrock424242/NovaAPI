package com.thunder.novaapi.systems;

import com.thunder.novaapi.config.AdaptiveSystemsConfig;
import net.minecraft.server.MinecraftServer;

public interface AdaptiveSystem {
    String id();

    boolean isEnabled(AdaptiveSystemsConfig.AdaptiveSystemsValues config);

    void tick(MinecraftServer server, SafetyContext context);
}
