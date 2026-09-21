package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorConfig;
import com.essde.optimizator.PerformanceProfiler;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerProfilerMixin {
    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void optimizator$beginParticles(
            Camera camera,
            float tickDelta,
            VertexConsumerProvider.Immediate vertexConsumers,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) PerformanceProfiler.beginParticles();
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void optimizator$endParticles(
            Camera camera,
            float tickDelta,
            VertexConsumerProvider.Immediate vertexConsumers,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) PerformanceProfiler.endParticles();
    }
}
