package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorConfig;
import net.minecraft.client.render.chunk.ChunkBuilder;
import java.util.Queue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkBuilder.class)
public abstract class ChunkBuilderMixin {
    @Shadow @Final private Queue<Runnable> uploadQueue;

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void optimizator$budgetGpuUploads(CallbackInfo callbackInfo) {
        if (!OptimizatorConfig.enabled || !OptimizatorConfig.chunkUploadBudget) {
            return;
        }

        long start = System.nanoTime();
        long deadline = start
                + OptimizatorConfig.chunkUploadBudgetMicros * 1_000L;
        int processed = 0;

        while (processed < OptimizatorConfig.maxChunkUploadsPerFrame) {
            if (System.nanoTime() >= deadline) {
                break;
            }

            Runnable task = this.uploadQueue.poll();
            if (task == null) {
                break;
            }

            task.run();
            processed++;
        }

        PerformanceProfiler.recordChunkUploads(System.nanoTime() - start, processed);
        callbackInfo.cancel();
    }
}
