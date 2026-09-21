package com.essde.optimizator;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.Vec3d;

public final class CoreRendererOptimizer {
    private static final double POSITION_EPSILON = 0.0005D;
    private static final float ROTATION_EPSILON = 0.001F;

    private static final Long2BooleanOpenHashMap SCHEDULED_REBUILDS =
            new Long2BooleanOpenHashMap();

    private static boolean terrainStateValid;
    private static boolean terrainDirty = true;
    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static float lastYaw;
    private static float lastPitch;
    private static int lastViewDistance = -1;
    private static int lastFov = -1;

    private static long terrainCacheHits;
    private static long terrainCacheMisses;
    private static long coalescedRebuilds;

    private CoreRendererOptimizer() {
    }

    public static boolean tryReuseTerrain(
            WorldRenderer renderer,
            Camera camera,
            boolean hasForcedFrustum,
            boolean spectator
    ) {
        if (hasForcedFrustum || spectator || camera == null || !camera.isReady()) {
            terrainCacheMisses++;
            return false;
        }

        ChunkBuilder chunkBuilder = renderer.getChunkBuilder();
        if (chunkBuilder == null || renderer.getBuiltChunks().isEmpty()) {
            terrainCacheMisses++;
            invalidateTerrain();
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int viewDistance = client.options.getViewDistance().getValue();
        int fov = client.options.getFov().getValue();
        Vec3d pos = camera.getPos();
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();

        if (terrainStateValid
                && !terrainDirty
                && lastViewDistance == viewDistance
                && lastFov == fov
                && close(pos.x, lastX)
                && close(pos.y, lastY)
                && close(pos.z, lastZ)
                && close(yaw, lastYaw)
                && close(pitch, lastPitch)) {
            chunkBuilder.setCameraPosition(pos);
            terrainCacheHits++;
            return true;
        }

        lastX = pos.x;
        lastY = pos.y;
        lastZ = pos.z;
        lastYaw = yaw;
        lastPitch = pitch;
        lastViewDistance = viewDistance;
        lastFov = fov;
        terrainStateValid = true;
        terrainDirty = false;
        terrainCacheMisses++;
        return false;
    }

    public static void invalidateTerrain() {
        terrainDirty = true;
        terrainStateValid = false;
    }

    public static boolean allowChunkRebuild(int x, int y, int z, boolean important) {
        long key = pack(x, y, z);
        if (!SCHEDULED_REBUILDS.containsKey(key)) {
            SCHEDULED_REBUILDS.put(key, important);
            return true;
        }

        boolean previousImportant = SCHEDULED_REBUILDS.get(key);
        if (!previousImportant && important) {
            SCHEDULED_REBUILDS.put(key, true);
            return true;
        }

        coalescedRebuilds++;
        return false;
    }

    public static void endClientTick() {
        SCHEDULED_REBUILDS.clear();
    }

    private static long pack(int x, int y, int z) {
        return (((long) x) & 0x3FFFFFFL)
                | ((((long) z) & 0x3FFFFFFL) << 26)
                | ((((long) y) & 0xFFFL) << 52);
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) <= POSITION_EPSILON;
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) <= ROTATION_EPSILON;
    }

    public static long terrainCacheHits() {
        return terrainCacheHits;
    }

    public static long terrainCacheMisses() {
        return terrainCacheMisses;
    }

    public static long coalescedRebuilds() {
        return coalescedRebuilds;
    }

    public static void resetStats() {
        terrainCacheHits = 0L;
        terrainCacheMisses = 0L;
        coalescedRebuilds = 0L;
    }
}
