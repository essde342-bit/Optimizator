package com.essde.optimizator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;

public final class OptimizatorRuntime {
    private static final int CONTROL_INTERVAL_TICKS = 40;
    private static final int REQUIRED_LOW_SAMPLES = 2;
    private static final int REQUIRED_HIGH_SAMPLES = 4;
    private static final int MAX_REDUCTION = 8;

    private static boolean initialized;
    private static int controlTicks;
    private static int lowSamples;
    private static int highSamples;
    private static int cloudLowSamples;
    private static int cloudHighSamples;
    private static int reductionLevel;

    private static int baseRenderDistance = -1;
    private static double baseEntityDistance = -1.0D;
    private static CloudRenderMode baseCloudMode;

    private static int lastAppliedRenderDistance = -1;
    private static double lastAppliedEntityDistance = -1.0D;
    private static CloudRenderMode lastAppliedCloudMode;

    private static long particleWindowStartNanos = System.nanoTime();
    private static long particleNowNanos = particleWindowStartNanos;
    private static int particlesInWindow;
    private static double particleCameraX;
    private static double particleCameraY;
    private static double particleCameraZ;
    private static int currentParticleBudget = OptimizatorConfig.particleBudget;

    private OptimizatorRuntime() {
    }

    public static void initialize() {
        initialized = true;
        controlTicks = 0;
        lowSamples = 0;
        highSamples = 0;
        cloudLowSamples = 0;
        cloudHighSamples = 0;
        reductionLevel = 0;
        baseRenderDistance = -1;
        baseEntityDistance = -1.0D;
        baseCloudMode = null;
        lastAppliedRenderDistance = -1;
        lastAppliedEntityDistance = -1.0D;
        lastAppliedCloudMode = null;
        particleWindowStartNanos = System.nanoTime();
        particleNowNanos = particleWindowStartNanos;
        particlesInWindow = 0;
        particleCameraX = 0.0D;
        particleCameraY = 0.0D;
        particleCameraZ = 0.0D;
        currentParticleBudget = OptimizatorConfig.particleBudget;
    }

    public static void tick(MinecraftClient client) {
        if (!initialized) {
            initialize();
        }

        if (!OptimizatorConfig.enabled) {
            if (client.world != null && client.player != null) {
                restoreUserOptions(client.options);
            }
            return;
        }

        if (client.world == null || client.player == null) {
            return;
        }

        particleNowNanos = System.nanoTime();
        if (particleNowNanos - particleWindowStartNanos >= 1_000_000_000L) {
            particleWindowStartNanos = particleNowNanos;
            particlesInWindow = 0;
        }
        Vec3d particleCamera = client.cameraEntity != null
                ? client.cameraEntity.getPos()
                : client.player.getPos();
        particleCameraX = particleCamera.x;
        particleCameraY = particleCamera.y;
        particleCameraZ = particleCamera.z;

        GameOptions options = client.options;
        rememberUserBaseline(options);

        if (!OptimizatorConfig.adaptive && !OptimizatorConfig.disableCloudsUnderLoad) {
            restoreUserOptions(options);
            return;
        }

        controlTicks++;
        if (controlTicks < CONTROL_INTERVAL_TICKS) {
            return;
        }
        controlTicks = 0;

        int fps = Math.max(client.getCurrentFps(), 1);

        if (OptimizatorConfig.adaptive) {
            updateAdaptiveController(fps);
            applyAdaptiveOptions(options);
        } else {
            lowSamples = 0;
            highSamples = 0;
            reductionLevel = 0;
            currentParticleBudget = OptimizatorConfig.particleBudget;
        }

        if (OptimizatorConfig.disableCloudsUnderLoad) {
            updateCloudController(options, fps);
        } else if (baseCloudMode != null
                && options.getCloudRenderMode().getValue() != baseCloudMode) {
            options.getCloudRenderMode().setValue(baseCloudMode);
            lastAppliedCloudMode = baseCloudMode;
        }
    }

    private static void updateAdaptiveController(int fps) {
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
    }

    private static void updateCloudController(GameOptions options, int fps) {
        if (fps < OptimizatorConfig.fpsFloor) {
            cloudLowSamples++;
            cloudHighSamples = 0;

            if (cloudLowSamples >= REQUIRED_LOW_SAMPLES) {
                cloudLowSamples = 0;
                if (options.getCloudRenderMode().getValue() != CloudRenderMode.OFF) {
                    options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                    lastAppliedCloudMode = CloudRenderMode.OFF;
                }
            }
        } else if (fps >= OptimizatorConfig.fpsFloor + 15) {
            cloudHighSamples++;
            cloudLowSamples = 0;

            if (cloudHighSamples >= REQUIRED_HIGH_SAMPLES) {
                cloudHighSamples = 0;
                if (baseCloudMode != null
                        && options.getCloudRenderMode().getValue() != baseCloudMode) {
                    options.getCloudRenderMode().setValue(baseCloudMode);
                    lastAppliedCloudMode = baseCloudMode;
                }
            }
        } else {
            cloudLowSamples = 0;
            cloudHighSamples = 0;
        }
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

    /**
     * This filter runs in ClientWorld before ParticleManager allocates a Particle
     * object. That is intentionally earlier than a render-time check, so rejected
     * particles do not create Java objects or enter the particle queues.
     */
    public static boolean allowParticle(
            ParticleEffect parameters,
            boolean force,
            boolean canSpawnOnMinimal,
            double x,
            double y,
            double z
    ) {
        if (!OptimizatorConfig.enabled || parameters == null || force) {
            return true;
        }

        boolean typeRulesActive =
                !OptimizatorConfig.particleDisabledTypes.isEmpty()
                        || !OptimizatorConfig.particleReducedTypes.isEmpty();
        boolean qualityActive = OptimizatorConfig.particleQuality != 0;
        if (!OptimizatorConfig.particleLimiter
                && !OptimizatorConfig.particleCulling
                && !qualityActive
                && !typeRulesActive) {
            return true;
        }

        if (OptimizatorConfig.particleQuality == 2 && !canSpawnOnMinimal) {
            return false;
        }

        int typeMode = 0;
        int typeHash = parameters.getType().hashCode();
        if (!OptimizatorConfig.particleDisabledTypes.isEmpty()
                || !OptimizatorConfig.particleReducedTypes.isEmpty()) {
            String typeId = Registries.PARTICLE_TYPE.getId(parameters.getType()).toString();
            typeMode = OptimizatorConfig.getParticleMode(typeId);
            if (typeMode == 2) {
                return false;
            }
        }

        if (OptimizatorConfig.particleCulling) {
            double dx = x - particleCameraX;
            double dy = y - particleCameraY;
            double dz = z - particleCameraZ;
            double limit = OptimizatorConfig.particleCullDistance;
            if (dx * dx + dy * dy + dz * dz > limit * limit) {
                return false;
            }
        }

        double probability = 1.0D;

        if (OptimizatorConfig.particleQuality == 1) {
            probability *= 0.60D;
        } else if (OptimizatorConfig.particleQuality == 2) {
            probability *= 0.25D;
        }

        if (typeMode == 1) {
            probability *= 0.50D;
        }

        if (probability < 1.0D && !sampleParticle(probability, typeHash, x, y, z)) {
            return false;
        }

        if (OptimizatorConfig.particleLimiter) {
            if (particlesInWindow >= currentParticleBudget) {
                return false;
            }
            particlesInWindow++;
        }

        return true;
    }

    private static boolean sampleParticle(
            double probability,
            int typeHash,
            double x,
            double y,
            double z
    ) {
        long bits = Double.doubleToLongBits(x);
        bits = bits * 31L + Double.doubleToLongBits(y);
        bits = bits * 31L + Double.doubleToLongBits(z);
        bits = bits * 31L + typeHash;
        bits ^= bits >>> 33;
        long positive = bits & Long.MAX_VALUE;
        double normalized = positive / (double) Long.MAX_VALUE;
        return normalized < probability;
    }

    public static int getReductionLevel() {
        return reductionLevel;
    }

    public static int getCurrentParticleBudget() {
        return currentParticleBudget;
    }

    public static boolean isFarEntityCullingEnabled() {
        return OptimizatorConfig.enabled && OptimizatorConfig.deepEntityCulling;
    }

    public static boolean shouldCullFarEntity(
            net.minecraft.entity.Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        if (!OptimizatorConfig.enabled || !OptimizatorConfig.deepEntityCulling) {
            return false;
        }

        if (entity == null
                || entity == MinecraftClient.getInstance().player
                || entity.isSpectator()) {
            return false;
        }

        boolean allowedType =
                (OptimizatorConfig.cullItemEntities
                        && entity instanceof net.minecraft.entity.ItemEntity)
                        || (OptimizatorConfig.cullExperienceOrbs
                        && entity instanceof net.minecraft.entity.ExperienceOrbEntity);

        if (!allowedType) {
            return false;
        }

        double dx = entity.getX() - cameraX;
        double dy = entity.getY() - cameraY;
        double dz = entity.getZ() - cameraZ;
        double distance = dx * dx + dy * dy + dz * dz;
        double limit = OptimizatorConfig.farEntityCullDistance;
        return distance > limit * limit;
    }
}
