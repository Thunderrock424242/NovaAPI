package com.thunder.novaapi.RenderEngine.API;

import com.thunder.novaapi.RenderEngine.particles.ParticleCullingManager;
import net.minecraft.world.phys.Vec3;

public final class ParticleCullingAPI {
    private ParticleCullingAPI() {
    }

    /**
     * Queue a particle render submission that will be culled by distance and frustum before rendering.
     */
    public static void queueParticle(Vec3 position, float radius, Runnable submit) {
        ParticleCullingManager.queue(new ParticleCullingManager.ParticleRenderRequest(position, radius, submit));
    }
}
