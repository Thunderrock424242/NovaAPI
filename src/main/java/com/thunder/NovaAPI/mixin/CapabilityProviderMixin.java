package com.thunder.NovaAPI.mixin;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CapabilityProvider.class)
public class CapabilityProviderMixin {
    @Inject(method = "serializeNBT", at = @At("RETURN"), cancellable = true)
    private void nova$interceptCapability(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        com.thunder.NovaAPI.NBTUpgrader.NovaNbtInterceptor.tryIntercept(tag);
        cir.setReturnValue(tag);
    }
}
