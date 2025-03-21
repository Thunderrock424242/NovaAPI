package com.thunder.NovaAPI.NBTUpgrader;

import net.minecraft.nbt.CompoundTag;

public class NBTMigrationManager {
    public static final int CURRENT_VERSION = 1;

    public static void tryMigrate(String id, NBTType type, CompoundTag tag) {
        int version = tag.getInt("NovaDataVersion");

        if (version < CURRENT_VERSION && NBTMigrationRegistry.hasMigration(id, type)) {
            try {
                NBTMigrationRegistry.migrate(id, type, tag);
                tag.putInt("NovaDataVersion", CURRENT_VERSION);
            } catch (Exception e) {
                System.err.println("[NovaAPI] Migration error for " + id + ": " + e.getMessage());
            }
        } else if (!NBTMigrationRegistry.hasMigration(id, type)) {
            logUnknownNBT(id, type, tag);
        }
    }

    private static void logUnknownNBT(String id, NBTType type, CompoundTag tag) {
        // You could hash the tag or trim for logging simplicity
        System.out.println("[NovaAPI] Detected unregistered NBT structure: ID=" + id + ", Type=" + type);
        System.out.println("→ Example Keys: " + tag.getAllKeys().stream().limit(5).toList());
    }
}