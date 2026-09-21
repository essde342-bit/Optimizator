package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorRuntime;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererEntityCullingMixin {
    @Inject(
            method = "renderEntity(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void optimizator$cullCheapFarEntity(
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            CallbackInfo callbackInfo
    ) {
        if (!OptimizatorRuntime.isFarEntityCullingEnabled()) {
            return;
        }

        if (OptimizatorRuntime.shouldCullFarEntity(
                entity, cameraX, cameraY, cameraZ)) {
            callbackInfo.cancel();
        }
    }
}
