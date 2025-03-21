package com.thunder.NovaAPI.NBTUpgrader;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.File;

public class StructureMigrationSupport {

    public static void loadAndMigrateStructure(ResourceLocation id, MinecraftServer server) {
        File structureFile = server.getWorldPath(server.getLevelIdName())
                .resolve("generated/minecraft/structures/" + id.getNamespace() + "/" + id.getPath() + ".nbt")
                .toFile();

        if (structureFile.exists()) {
            try {
                CompoundTag tag = StructureTemplate.loadStructureTag(structureFile.toPath());

                NBTMigrationManager.tryMigrate(id.toString(), NBTType.STRUCTURE, tag);

                // You could optionally save the tag back here
            } catch (Exception e) {
                System.err.println("[NovaAPI] Failed to read/migrate structure: " + id + " - " + e.getMessage());
            }
        }
    }
}