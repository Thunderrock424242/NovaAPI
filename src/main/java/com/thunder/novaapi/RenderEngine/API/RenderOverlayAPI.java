package com.thunder.novaapi.RenderEngine.API;

import com.thunder.novaapi.RenderEngine.overlay.OverlayBatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class RenderOverlayAPI {
    private RenderOverlayAPI() {
    }

    /**
     * Queue an overlay draw call that will be grouped by texture where possible.
     */
    public static void queueOverlay(ResourceLocation texture, Consumer<GuiGraphics> drawCall) {
        OverlayBatcher.queue(texture, drawCall);
    }

    /**
     * Queue an overlay draw call without specifying a texture.
     */
    public static void queueOverlay(Consumer<GuiGraphics> drawCall) {
        OverlayBatcher.queue(drawCall);
    }
}
