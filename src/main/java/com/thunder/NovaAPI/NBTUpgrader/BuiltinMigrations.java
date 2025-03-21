package com.thunder.NovaAPI.NBTUpgrader;

import net.minecraft.nbt.CompoundTag;

public class BuiltinMigrations {
    public static void registerAll() {
        NBTMigrationRegistry.register("minecraft:chest", NBTType.BLOCK_ENTITY, (ctx) -> {
            CompoundTag tag = ctx.tag();
            if (tag.contains("OldLoot")) {
                tag.putString("LootTable", tag.getString("OldLoot"));
                tag.remove("OldLoot");
            }
        });

        // Example modded support
        NBTMigrationRegistry.register("modid:custom_entity", NBTType.ENTITY, (ctx) -> {
            CompoundTag tag = ctx.tag();
            if (tag.contains("legacy_stat")) {
                int stat = tag.getInt("legacy_stat");
                tag.putInt("new_stat", stat * 2);
                tag.remove("legacy_stat");
            }
        });
    }
}