package com.thunder.NovaAPI.RenderEngine.texture;

import com.thunder.NovaAPI.RenderEngine.Threading.RenderThreadManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TextureStreamingManager {
    private static final Map<ResourceLocation, Integer> loadedTextures = new ConcurrentHashMap<>();
    private static final TextureManager textureManager = new TextureManager(null); // Should be replaced with game instance

    public static CompletableFuture<Integer> loadTexture(ResourceLocation texturePath) {
        // Detect which mod is calling this method
        String modName = getCurrentModName();

        return CompletableFuture.supplyAsync(() -> {
            if (loadedTextures.containsKey(texturePath)) {
                return loadedTextures.get(texturePath);
            }

            // Locate PNG texture
            File pngFile = new File("assets/" + texturePath.getNamespace() + "/textures/" + texturePath.getPath() + ".png");

            // Compress PNG using Java-based compression
            ByteBuffer textureBuffer = JavaKTX2Compressor.compressPNGToByteBuffer(pngFile);
            if (textureBuffer == null) {
                System.err.println("[TextureStreamingManager] Failed to process texture: " + texturePath);
                return -1;
            }

            // Upload compressed texture to OpenGL
            int textureID = uploadCompressedTexture(textureBuffer, pngFile);

            loadedTextures.put(texturePath, textureID);
            return textureID;
        }, task -> RenderThreadManager.executeWithSafeGuard(modName, task));
    }

    private static String getCurrentModName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            return ModList.get().getMods().stream()
                    .filter(mod -> className.startsWith(mod.getModId()))
                    .map(mod -> mod.getModId())
                    .findFirst()
                    .orElse("unknown");
        }
        return "unknown";
    }

    private static int uploadCompressedTexture(ByteBuffer buffer, File textureFile) {
        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);

        // Use OpenGL's built-in compression (S3TC, BPTC, or ETC2 based on GPU support)
        GL13.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL13.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GL13.glCompressedTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT,
                buffer.capacity() / 3, buffer.capacity() / 3, 0, buffer);

        return textureID;
    }

    public static void unloadTexture(ResourceLocation texturePath) {
        if (loadedTextures.containsKey(texturePath)) {
            textureManager.release(texturePath);
            loadedTextures.remove(texturePath);
        }
    }
}
