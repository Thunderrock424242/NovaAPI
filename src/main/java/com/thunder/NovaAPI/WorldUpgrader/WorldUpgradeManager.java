package com.thunder.NovaAPI.WorldUpgrader;

import net.minecraft.server.MinecraftServer;

public class WorldUpgradeManager {
    public static void upgradeWorld(MinecraftServer server) {
        if (!WorldVersionTracker.needsUpgrade(server)) return;

        System.out.println("[Wilderness Odyssey] World upgrade detected! Upgrading to version " + WildernessOdysseyAPIMainModClass.VERSION);

        BackupSystem.createBackup(server);
        DataMigrationHandler.migrateWorld(server);

        WorldVersionTracker.saveCurrentVersion(server);
        System.out.println("[Wilderness Odyssey] Upgrade complete! Now running version " + WildernessOdysseyAPIMainModClass.VERSION);
    }
}