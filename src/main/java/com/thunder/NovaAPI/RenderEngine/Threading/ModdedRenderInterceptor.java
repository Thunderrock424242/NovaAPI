package com.thunder.NovaAPI.RenderEngine.Threading;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

public class ModdedRenderInterceptor {
    public static void executeModRender(Runnable task) {
        String modName = getCurrentModName();
        RenderThreadManager.executeWithSafeGuard(modName, task);
    }

    private static String getCurrentModName() {
        return ModList.get().getMods().stream()
                .map(IModInfo::getModId)
                .filter(modId -> Thread.currentThread().getStackTrace()[2].getClassName().startsWith(modId)).findFirst().orElse("unknown");
    }
}