package com.thunder.NovaAPI.WorldUpgrader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class DataMigrationHandler {
    /**
     * Runs world migration tasks: block replacement, entity fixing, item updates.
     */
    public static void migrateWorld(MinecraftServer server) {
        System.out.println("[NovaAPI] Starting automatic world migration...");

        handleBlockRemapping(server);
        handleEntityRemapping(server);
        handleItemRemapping(server);

        System.out.println("[NovaAPI] World migration complete!");
    }

    /**
     * 🔄 Automatically detects and replaces missing blocks.
     */
    private static void handleBlockRemapping(MinecraftServer server) {
        for (Level level : server.getAllLevels()) {
            for (int x = -30000000; x < 30000000; x += 16) {
                for (int z = -30000000; z < 30000000; z += 16) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                    if (chunk != null) {
                        for (BlockPos pos : BlockPos.betweenClosed(
                                chunk.getPos().getMinBlockX(), level.getMinBuildHeight(), chunk.getPos().getMinBlockZ(),
                                chunk.getPos().getMaxBlockX(), level.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ())) {

                            Block block = chunk.getBlockState(pos).getBlock();
                            Optional<ResourceLocation> blockID = BuiltInRegistries.BLOCK.getResourceKey(block).map(key -> key.location());

                            if (blockID.isEmpty() || block == Blocks.AIR) {
                                System.out.println("[NovaAPI] Missing block at " + pos + ", replacing with STONE.");
                                chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                                chunk.setUnsaved(true);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 🔄 Automatically detects and removes invalid entities.
     */
    private static void handleEntityRemapping(MinecraftServer server) {
        for (Level level : server.getAllLevels()) {
            for (Entity entity : level.getEntitiesOfClass(Entity.class, level.getWorldBorder().getCollisionShape().bounds())) {
                Holder<EntityType<?>> entityHolder = entity.getType().builtInRegistryHolder();
                Optional<ResourceLocation> entityID = entityHolder.unwrapKey().map(key -> key.location());

                if (entityID.isEmpty() || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityID.get())) {
                    System.out.println("[NovaAPI] Removing missing entity at " + entity.blockPosition());
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    /**
     * 🔄 Automatically detects and replaces missing items in player inventories.
     */
    private static void handleItemRemapping(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(player -> {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty()) {
                    Optional<ResourceLocation> itemID = stack.getItem().builtInRegistryHolder().unwrapKey().map(key -> key.location());
                    if (itemID.isEmpty() || !BuiltInRegistries.ITEM.containsKey(itemID.get())) {
                        System.out.println("[NovaAPI] Found missing item in " + player.getName().getString() + "'s inventory, replacing...");
                        inventory.setItem(i, new ItemStack(Items.STONE)); // Replacing with stone item
                    }
                }
            }
        });
    }
}
