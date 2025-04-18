package com.thunder.NovaAPI.WorldUpgrader;

import com.thunder.NovaAPI.NovaAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WorldVersionTracker {
    private static final String VERSION_FILE = "world_version.dat";

    public static String getStoredVersion(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(VERSION_FILE);
        if (Files.exists(path)) {
            try {
                CompoundTag tag = NbtIo.readCompressed(Files.newInputStream(path), NbtAccounter.unlimitedHeap());
                return tag.getString("ModpackVersion");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "0.0.0";
    }

    public static void saveCurrentVersion(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(VERSION_FILE);
        CompoundTag tag = new CompoundTag();
        tag.putString("ModpackVersion", NovaAPI.VERSION);
        try {
            NbtIo.writeCompressed(tag, Files.newOutputStream(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean needsUpgrade(MinecraftServer server) {
        return !getStoredVersion(server).equals(NovaAPI.VERSION);
    }
}
