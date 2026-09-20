package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(
            method = "renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;FFLnet/minecraft/world/WorldView;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void optimizator$skipEntityShadows(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            EntityRenderState renderState,
            float opacity,
            float tickDelta,
            WorldView world,
            float radius,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.enabled && OptimizatorConfig.fastEntityShadows) {
            callbackInfo.cancel();
        }
    }
}
