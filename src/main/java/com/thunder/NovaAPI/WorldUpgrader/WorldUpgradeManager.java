package com.thunder.NovaAPI.WorldUpgrader;

import com.thunder.NovaAPI.NovaAPI;
import net.minecraft.server.MinecraftServer;

public class WorldUpgradeManager {
    public static void upgradeWorld(MinecraftServer server) {
        if (!WorldVersionTracker.needsUpgrade(server)) return;

        System.out.println("[Wilderness Odyssey] World upgrade detected! Upgrading to version " + NovaAPI.VERSION);

        BackupSystem.createBackup(server);
        DataMigrationHandler.migrateWorld(server);

        WorldVersionTracker.saveCurrentVersion(server);
        System.out.println("[Wilderness Odyssey] Upgrade complete! Now running version " + NovaAPI.VERSION);
    }
}