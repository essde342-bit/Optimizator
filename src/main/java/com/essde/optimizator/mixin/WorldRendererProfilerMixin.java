package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorConfig;
import com.essde.optimizator.PerformanceProfiler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererProfilerMixin {
    @Inject(method = "renderMain", at = @At("HEAD"))
    private void optimizator$beginWorld(
            FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            Fog fog,
            boolean renderBlockOutline,
            boolean renderEntityOutlines,
            RenderTickCounter renderTickCounter,
            Profiler profiler,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.beginWorld();
        }
    }

    @Inject(method = "renderMain", at = @At("RETURN"))
    private void optimizator$endWorld(
            FrameGraphBuilder frameGraphBuilder,
            Frustum frustum,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            Fog fog,
            boolean renderBlockOutline,
            boolean renderEntityOutlines,
            RenderTickCounter renderTickCounter,
            Profiler profiler,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.endWorld();
        }
    }

    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void optimizator$beginEntities(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            Camera camera,
            RenderTickCounter tickCounter,
            List<Entity> entities,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.beginEntities();
        }
    }

    @Inject(method = "renderEntities", at = @At("RETURN"))
    private void optimizator$endEntities(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            Camera camera,
            RenderTickCounter tickCounter,
            List<Entity> entities,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.endEntities();
        }
    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"))
    private void optimizator$beginBlockEntities(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            VertexConsumerProvider.Immediate immediate2,
            Camera camera,
            float tickDelta,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.beginBlockEntities();
        }
    }

    @Inject(method = "renderBlockEntities", at = @At("RETURN"))
    private void optimizator$endBlockEntities(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            VertexConsumerProvider.Immediate immediate2,
            Camera camera,
            float tickDelta,
            CallbackInfo callbackInfo
    ) {
        if (OptimizatorConfig.profilerEnabled) {
            PerformanceProfiler.endBlockEntities();
        }
    }
}
