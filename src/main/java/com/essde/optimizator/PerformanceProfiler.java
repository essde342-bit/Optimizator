package com.essde.optimizator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;

public final class PerformanceProfiler {
    private static long frameStart;
    private static long worldStart;
    private static long entitiesStart;
    private static long blockEntitiesStart;
    private static long particlesStart;

    private static long frameNanos;
    private static long worldNanos;
    private static long entitiesNanos;
    private static long blockEntitiesNanos;
    private static long particlesNanos;

    private static long chunkUploadNanos;
    private static long chunkUploadTasks;
    private static long particleCreated;
    private static long particleRejected;

    private static int sampleFrames;
    private static double averageFrameMs;
    private static double onePercentLowMs = 0.0D;
    private static long worstFrameNanos;

    private PerformanceProfiler() {}

    public static void beginFrame() {
        if (!OptimizatorConfig.profilerEnabled) return;
        frameStart = System.nanoTime();
    }

    public static void endFrame(MinecraftClient client) {
        if (!OptimizatorConfig.profilerEnabled || frameStart == 0L) return;

        frameNanos = System.nanoTime() - frameStart;
        sampleFrames++;

        double ms = frameNanos / 1_000_000.0D;
        averageFrameMs += (ms - averageFrameMs) / Math.min(sampleFrames, 120);
        worstFrameNanos = Math.max(worstFrameNanos, frameNanos);

        if (sampleFrames >= 120) {
            onePercentLowMs = worstFrameNanos / 1_000_000.0D;
            sampleFrames = 0;
            worstFrameNanos = 0L;
        }
    }

    public static void beginWorld() {
        if (OptimizatorConfig.profilerEnabled) worldStart = System.nanoTime();
    }

    public static void endWorld() {
        if (OptimizatorConfig.profilerEnabled && worldStart != 0L) {
            worldNanos = System.nanoTime() - worldStart;
        }
    }

    public static void beginEntities() {
        if (OptimizatorConfig.profilerEnabled) entitiesStart = System.nanoTime();
    }

    public static void endEntities() {
        if (OptimizatorConfig.profilerEnabled && entitiesStart != 0L) {
            entitiesNanos = System.nanoTime() - entitiesStart;
        }
    }

    public static void beginBlockEntities() {
        if (OptimizatorConfig.profilerEnabled) blockEntitiesStart = System.nanoTime();
    }

    public static void endBlockEntities() {
        if (OptimizatorConfig.profilerEnabled && blockEntitiesStart != 0L) {
            blockEntitiesNanos = System.nanoTime() - blockEntitiesStart;
        }
    }

    public static void beginParticles() {
        if (OptimizatorConfig.profilerEnabled) particlesStart = System.nanoTime();
    }

    public static void endParticles() {
        if (OptimizatorConfig.profilerEnabled && particlesStart != 0L) {
            particlesNanos = System.nanoTime() - particlesStart;
        }
    }

    public static void recordChunkUploads(long nanos, int tasks) {
        if (!OptimizatorConfig.profilerEnabled) return;
        chunkUploadNanos += nanos;
        chunkUploadTasks += tasks;
    }

    public static void recordParticle(boolean created) {
        if (created) particleCreated++;
        else particleRejected++;
    }

    public static void resetCounters() {
        chunkUploadNanos = 0L;
        chunkUploadTasks = 0L;
        particleCreated = 0L;
        particleRejected = 0L;
    }

    public static double frameMs() {
        return frameNanos / 1_000_000.0D;
    }

    public static double averageFrameMs() {
        return averageFrameMs;
    }

    public static double onePercentLowMs() {
        return onePercentLowMs;
    }

    public static double worldMs() {
        return worldNanos / 1_000_000.0D;
    }

    public static double entitiesMs() {
        return entitiesNanos / 1_000_000.0D;
    }

    public static double blockEntitiesMs() {
        return blockEntitiesNanos / 1_000_000.0D;
    }

    public static double particlesMs() {
        return particlesNanos / 1_000_000.0D;
    }

    public static double chunkUploadMs() {
        return chunkUploadNanos / 1_000_000.0D;
    }

    public static long chunkUploadTasks() {
        return chunkUploadTasks;
    }

    public static long particlesCreated() {
        return particleCreated;
    }

    public static long particlesRejected() {
        return particleRejected;
    }

    public static int bottleneck(MinecraftClient client) {
        double frame = Math.max(frameMs(), 0.01D);
        double world = worldMs();
        double entities = entitiesMs();
        double blockEntities = blockEntitiesMs();
        double particles = particlesMs();

        if (world / frame > 0.45D) return 1;
        if (entities / frame > 0.25D) return 2;
        if (blockEntities / frame > 0.20D) return 3;
        if (particles / frame > 0.15D) return 4;
        return 0;
    }

    public static String bottleneckName(MinecraftClient client) {
        return switch (bottleneck(client)) {
            case 1 -> "WORLD / CHUNKS";
            case 2 -> "ENTITIES";
            case 3 -> "BLOCK ENTITIES";
            case 4 -> "PARTICLES";
            default -> "BALANCED / GPU";
        };
    }

    public static String chunkDebug(WorldRenderer renderer) {
        if (renderer == null || renderer.getChunkBuilder() == null) return "n/a";
        return renderer.getChunkBuilder().getDebugString();
    }
}
