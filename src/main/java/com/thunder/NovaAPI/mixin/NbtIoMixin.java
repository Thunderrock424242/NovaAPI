package com.thunder.NovaAPI.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

@Mixin(NbtIo.class)
public class NbtIoMixin {
    @Inject(method = "read", at = @At("RETURN"), cancellable = true)
    private static void nova$interceptRead(InputStream input, NbtAccounter accounter, CallbackInfoReturnable<Tag> cir) {
        Tag tag = cir.getReturnValue();
        if (tag instanceof CompoundTag compound) {
            com.thunder.NovaAPI.NBTUpgrader.NovaNbtInterceptor.tryIntercept(compound);
        }
    }

    @Inject(method = "readCompressed", at = @At("RETURN"), cancellable = true)
    private static void nova$interceptReadCompressed(InputStream input, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        com.thunder.NovaAPI.NBTUpgrader.NovaNbtInterceptor.tryIntercept(tag);
    }
}
