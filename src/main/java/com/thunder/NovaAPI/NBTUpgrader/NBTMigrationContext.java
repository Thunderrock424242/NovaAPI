package com.thunder.NovaAPI.NBTUpgrader;

import net.minecraft.nbt.CompoundTag;

public record NBTMigrationContext(String id, NBTType type, CompoundTag tag) {}