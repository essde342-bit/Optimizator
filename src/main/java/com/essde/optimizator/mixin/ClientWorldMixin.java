package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorRuntime;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void optimizator$limitParticleSpawning(
            ParticleEffect parameters,
            boolean force,
            boolean canSpawnOnMinimal,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfo callbackInfo
    ) {
        if (!OptimizatorRuntime.allowParticle(force)) {
            callbackInfo.cancel();
        }
    }
}
