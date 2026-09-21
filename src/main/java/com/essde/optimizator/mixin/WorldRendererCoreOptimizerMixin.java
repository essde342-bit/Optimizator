package com.essde.optimizator.mixin;

import com.essde.optimizator.CoreRendererOptimizer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererCoreOptimizerMixin {
    @Inject(
            method = "setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void optimizator$reuseStableSectionList(
            Camera camera,
            net.minecraft.client.render.Frustum frustum,
            boolean hasForcedFrustum,
            boolean spectator,
            CallbackInfo callbackInfo
    ) {
        if (CoreRendererOptimizer.tryReuseTerrain(
                (WorldRenderer) (Object) this,
                camera,
                hasForcedFrustum,
                spectator
        )) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "scheduleTerrainUpdate()V", at = @At("HEAD"))
    private void optimizator$invalidateTerrain(CallbackInfo callbackInfo) {
        CoreRendererOptimizer.invalidateTerrain();
    }

    @Inject(method = "scheduleChunkRender(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void optimizator$coalesceChunkRebuild(
            int x,
            int y,
            int z,
            boolean important,
            CallbackInfo callbackInfo
    ) {
        if (!CoreRendererOptimizer.allowChunkRebuild(x, y, z, important)) {
            callbackInfo.cancel();
        } else {
            CoreRendererOptimizer.invalidateTerrain();
        }
    }

    @Inject(method = "onChunkUnload(J)V", at = @At("HEAD"))
    private void optimizator$invalidateOnChunkUnload(long sectionPos, CallbackInfo callbackInfo) {
        CoreRendererOptimizer.invalidateTerrain();
    }

    @Inject(method = "setWorld(Lnet/minecraft/client/world/ClientWorld;)V", at = @At("HEAD"))
    private void optimizator$invalidateOnWorldChange(ClientWorld world, CallbackInfo callbackInfo) {
        CoreRendererOptimizer.invalidateTerrain();
        CoreRendererOptimizer.endClientTick();
    }
}
