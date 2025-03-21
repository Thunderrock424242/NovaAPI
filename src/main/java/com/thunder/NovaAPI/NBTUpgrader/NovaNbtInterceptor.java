package com.thunder.NovaAPI.NBTUpgrader;

import net.minecraft.nbt.CompoundTag;

public class NovaNbtInterceptor {
    public static void tryIntercept(CompoundTag tag) {
        if (tag == null) return;

        String id = tag.getString("id");
        NBTType type = guessType(tag);

        if (id.isEmpty()) id = "unknown";

        NBTMigrationManager.tryMigrate(id, type, tag);
    }

    private static NBTType guessType(CompoundTag tag) {
        if (tag.contains("Pos") && tag.contains("Motion")) return NBTType.ENTITY;
        if (tag.contains("Inventory") && tag.contains("Abilities")) return NBTType.PLAYER;
        if (tag.contains("palette") && tag.contains("blocks")) return NBTType.STRUCTURE;
        if (tag.contains("x") && tag.contains("y") && tag.contains("z") && tag.contains("id")) return NBTType.BLOCK_ENTITY;
        if (tag.contains("DataVersion") || tag.contains("WorldGenSettings")) return NBTType.WORLD;
        return NBTType.UNKNOWN;
    }
}
