package com.thunder.NovaAPI.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;

@Mixin(targets = "org.spongepowered.asm.mixin.transformer.MixinProcessor", remap = false)
public class MixinProcessorMixin {

    @Inject(method = "handleMixinApplyError", at = @At("HEAD"))
    private void onMixinApplyError(String targetClass, InvalidMixinException ex, MixinEnvironment environment, CallbackInfo ci) {
        String message = "Mixin apply failed: " + mixin.getName() + " -> " + targetClassName;
        LiveLogMonitor.captureMixinFailure(message, ex);
    }
}
