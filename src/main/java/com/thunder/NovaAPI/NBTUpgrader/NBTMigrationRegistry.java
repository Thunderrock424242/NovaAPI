package com.thunder.NovaAPI.NBTUpgrader;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NBTMigrationRegistry {
    private static final Map<String, Map<NBTType, Consumer<NBTMigrationContext>>> MIGRATIONS = new HashMap<>();

    public static void register(String id, NBTType type, Consumer<NBTMigrationContext> migration) {
        MIGRATIONS.computeIfAbsent(id, k -> new HashMap<>()).put(type, migration);
    }

    public static boolean hasMigration(String id, NBTType type) {
        return MIGRATIONS.containsKey(id) && MIGRATIONS.get(id).containsKey(type);
    }

    public static void migrate(String id, NBTType type, CompoundTag tag) {
        try {
            if (hasMigration(id, type)) {
                MIGRATIONS.get(id).get(type).accept(new NBTMigrationContext(id, type, tag));
            }
        } catch (Exception e) {
            System.err.println("[NovaAPI] Migration error for " + id + ": " + e.getMessage());
        }
    }
}