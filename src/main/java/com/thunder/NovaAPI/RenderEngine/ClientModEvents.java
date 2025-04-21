package com.thunder.NovaAPI.RenderEngine;


import com.thunder.NovaAPI.NovaAPI;
import com.thunder.NovaAPI.RenderEngine.Threading.ModdedRenderInterceptor;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import static com.thunder.NovaAPI.NovaAPI.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        ModdedRenderInterceptor.executeModRender(() -> {
            // TODO: Run modded world rendering logic
        });
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Pre event) {
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        try {
            ModdedRenderInterceptor.executeModRender(() -> {
                // Run actual GUI drawing here using graphics
            });
        } catch (Exception e) {
            NovaAPI.LOGGER.error("RenderInterceptor threw during overlay rendering", e);
        } finally {
            graphics.pose().popPose();
        }
    }
}