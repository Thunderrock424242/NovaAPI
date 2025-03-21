package com.thunder.NovaAPI.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "saveWithoutId", at = @At("RETURN"), cancellable = true)
    private void nova$interceptSave(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        com.thunder.NovaAPI.NBTUpgrader.NovaNbtInterceptor.tryIntercept(tag);
    }
}
