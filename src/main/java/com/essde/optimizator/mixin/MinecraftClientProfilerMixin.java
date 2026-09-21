package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorConfig;
import com.essde.optimizator.PerformanceProfiler;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientProfilerMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void optimizator$beginFrame(boolean tick, CallbackInfo callbackInfo) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.beginFrame();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void optimizator$endFrame(boolean tick, CallbackInfo callbackInfo) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.endFrame((MinecraftClient) (Object) this);
        }
    }
}
