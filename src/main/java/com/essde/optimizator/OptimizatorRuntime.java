package com.essde.optimizator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;

public final class OptimizatorRuntime {
    private static final int CONTROL_INTERVAL_TICKS = 40;
    private static final int REQUIRED_LOW_SAMPLES = 2;
    private static final int REQUIRED_HIGH_SAMPLES = 4;
    private static final int MAX_REDUCTION = 8;

    private static boolean initialized;
    private static int controlTicks;
    private static int lowSamples;
    private static int highSamples;
    private static int reductionLevel;

    private static int baseRenderDistance = -1;
    private static double baseEntityDistance = -1.0D;
    private static CloudRenderMode baseCloudMode;

    private static int lastAppliedRenderDistance = -1;
    private static double lastAppliedEntityDistance = -1.0D;
    private static CloudRenderMode lastAppliedCloudMode;

    private static long particleWindowStartNanos = System.nanoTime();
    private static int particlesInWindow;
    private static int currentParticleBudget = OptimizatorConfig.particleBudget;

    private OptimizatorRuntime() {
    }

    public static void initialize() {
        initialized = true;
        controlTicks = 0;
        lowSamples = 0;
        highSamples = 0;
        reductionLevel = 0;
        baseRenderDistance = -1;
        baseEntityDistance = -1.0D;
        baseCloudMode = null;
        lastAppliedRenderDistance = -1;
        lastAppliedEntityDistance = -1.0D;
        lastAppliedCloudMode = null;
        particleWindowStartNanos = System.nanoTime();
        particlesInWindow = 0;
        currentParticleBudget = OptimizatorConfig.particleBudget;
    }

    public static void tick(MinecraftClient client) {
        if (!initialized) {
            initialize();
        }

        if (!OptimizatorConfig.enabled) {
            return;
        }

        if (client.world == null || client.player == null) {
            return;
        }

        GameOptions options = client.options;
        rememberUserBaseline(options);

        if (!OptimizatorConfig.adaptive) {
            restoreUserOptions(options);
            return;
        }

        controlTicks++;
        if (controlTicks < CONTROL_INTERVAL_TICKS) {
            return;
        }
        controlTicks = 0;

        int fps = Math.max(client.getCurrentFps(), 1);

        if (fps < OptimizatorConfig.fpsFloor) {
            lowSamples++;
            highSamples = 0;

            if (lowSamples >= REQUIRED_LOW_SAMPLES) {
                lowSamples = 0;
                reductionLevel = Math.min(reductionLevel + 1, MAX_REDUCTION);
            }
        } else if (fps >= OptimizatorConfig.fpsFloor + 15) {
            highSamples++;
            lowSamples = 0;

            if (highSamples >= REQUIRED_HIGH_SAMPLES) {
                highSamples = 0;
                reductionLevel = Math.max(reductionLevel - 1, 0);
            }
        } else {
            lowSamples = 0;
            highSamples = 0;
        }

        applyAdaptiveOptions(options);
    }

    private static void rememberUserBaseline(GameOptions options) {
        int liveRenderDistance = options.getViewDistance().getValue();
        double liveEntityDistance = options.getEntityDistanceScaling().getValue();
        CloudRenderMode liveCloudMode = options.getCloudRenderMode().getValue();

        if (baseRenderDistance < 0) {
            baseRenderDistance = liveRenderDistance;
            lastAppliedRenderDistance = liveRenderDistance;
        } else if (liveRenderDistance != lastAppliedRenderDistance) {
            baseRenderDistance = liveRenderDistance;
            reductionLevel = 0;
        }

        if (baseEntityDistance < 0.0D) {
            baseEntityDistance = liveEntityDistance;
            lastAppliedEntityDistance = liveEntityDistance;
        } else if (Double.compare(liveEntityDistance, lastAppliedEntityDistance) != 0) {
            baseEntityDistance = liveEntityDistance;
            reductionLevel = 0;
        }

        if (baseCloudMode == null) {
            baseCloudMode = liveCloudMode;
            lastAppliedCloudMode = liveCloudMode;
        } else if (liveCloudMode != lastAppliedCloudMode) {
            baseCloudMode = liveCloudMode;
        }
    }

    private static void applyAdaptiveOptions(GameOptions options) {
        int minimumRenderDistance = Math.min(
                baseRenderDistance,
                OptimizatorConfig.minRenderDistance
        );
        int targetRenderDistance = Math.max(
                minimumRenderDistance,
                baseRenderDistance - reductionLevel
        );

        if (targetRenderDistance != options.getViewDistance().getValue()) {
            options.getViewDistance().setValue(targetRenderDistance);
        }
        lastAppliedRenderDistance = targetRenderDistance;

        double scale = Math.max(
                OptimizatorConfig.minEntityDistance,
                baseEntityDistance * (1.0D - (reductionLevel * 0.12D))
        );

        if (Double.compare(scale, options.getEntityDistanceScaling().getValue()) != 0) {
            options.getEntityDistanceScaling().setValue(scale);
        }
        lastAppliedEntityDistance = scale;

        currentParticleBudget = calculateParticleBudget(reductionLevel);

        if (OptimizatorConfig.disableCloudsUnderLoad) {
            CloudRenderMode targetCloudMode =
                    reductionLevel >= 2 ? CloudRenderMode.OFF : baseCloudMode;

            if (targetCloudMode != options.getCloudRenderMode().getValue()) {
                options.getCloudRenderMode().setValue(targetCloudMode);
            }
            lastAppliedCloudMode = targetCloudMode;
        }
    }

    private static int calculateParticleBudget(int level) {
        int[] multipliers = {100, 75, 55, 40, 30, 25, 20, 18, 15};
        int index = Math.min(level, multipliers.length - 1);
        return Math.max(100, (OptimizatorConfig.particleBudget * multipliers[index]) / 100);
    }

    private static void restoreUserOptions(GameOptions options) {
        if (baseRenderDistance >= 0
                && options.getViewDistance().getValue() != baseRenderDistance) {
            options.getViewDistance().setValue(baseRenderDistance);
        }
        lastAppliedRenderDistance = baseRenderDistance;

        if (baseEntityDistance >= 0.0D
                && Double.compare(options.getEntityDistanceScaling().getValue(),
                baseEntityDistance) != 0) {
            options.getEntityDistanceScaling().setValue(baseEntityDistance);
        }
        lastAppliedEntityDistance = baseEntityDistance;

        if (baseCloudMode != null
                && options.getCloudRenderMode().getValue() != baseCloudMode) {
            options.getCloudRenderMode().setValue(baseCloudMode);
        }
        lastAppliedCloudMode = baseCloudMode;
        currentParticleBudget = OptimizatorConfig.particleBudget;
        reductionLevel = 0;
    }

    public static boolean allowParticle(boolean force) {
        if (!OptimizatorConfig.enabled || force || !OptimizatorConfig.particleLimiter) {
            return true;
        }

        long now = System.nanoTime();
        if (now - particleWindowStartNanos >= 1_000_000_000L) {
            particleWindowStartNanos = now;
            particlesInWindow = 0;
        }

        if (particlesInWindow >= currentParticleBudget) {
            return false;
        }

        particlesInWindow++;
        return true;
    }

    public static int getReductionLevel() {
        return reductionLevel;
    }

    public static int getCurrentParticleBudget() {
        return currentParticleBudget;
    }
}


    public static boolean shouldCullFarEntity(
            net.minecraft.entity.Entity entity,
            net.minecraft.util.math.Vec3d cameraPos
    ) {
        if (!OptimizatorConfig.enabled || !OptimizatorConfig.deepEntityCulling) {
            return false;
        }

        if (entity == null || entity == MinecraftClient.getInstance().player
                || entity.isSpectator()) {
            return false;
        }

        boolean cheapEntity = entity instanceof net.minecraft.entity.ItemEntity
                || entity instanceof net.minecraft.entity.ExperienceOrbEntity;

        if (!cheapEntity) {
            return false;
        }

        double distance = entity.squaredDistanceTo(cameraPos);
        double limit = OptimizatorConfig.farEntityCullDistance;
        return distance > limit * limit;
    }
