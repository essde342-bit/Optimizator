package com.essde.optimizator;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class OptimizatorConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Optimizator");
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("optimizator.properties");

    public static boolean enabled = true;
    public static boolean adaptive = true;
    public static boolean particleLimiter = true;
    public static boolean deepEntityCulling = true;
    public static boolean fastEntityShadows = true;
    public static boolean chunkUploadBudget = true;

    public static int minRenderDistance = 5;
    public static double minEntityDistance = 0.35D;
    public static int fpsFloor = 35;
    public static int particleBudget = 1200;

    public static int farEntityCullDistance = 48;
    public static int maxChunkUploadsPerFrame = 6;
    public static int chunkUploadBudgetMicros = 2500;

    private OptimizatorConfig() {
    }

    public static void load() {
        resetDefaults();

        if (!Files.exists(CONFIG_FILE)) {
            save();
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            properties.load(reader);

            enabled = getBoolean(properties, "enabled", enabled);
            adaptive = getBoolean(properties, "adaptive", adaptive);
            particleLimiter = getBoolean(properties, "particle_limiter", particleLimiter);
            deepEntityCulling =
                    getBoolean(properties, "deep_entity_culling", deepEntityCulling);
            fastEntityShadows =
                    getBoolean(properties, "fast_entity_shadows", fastEntityShadows);
            chunkUploadBudget =
                    getBoolean(properties, "chunk_upload_budget", chunkUploadBudget);

            minRenderDistance = clamp(
                    getInt(properties, "min_render_distance", minRenderDistance), 4, 32);
            minEntityDistance = clamp(
                    getDouble(properties, "min_entity_distance", minEntityDistance),
                    0.15D, 1.0D);
            fpsFloor = clamp(
                    getInt(properties, "fps_floor", fpsFloor), 20, 120);
            particleBudget = clamp(
                    getInt(properties, "particle_budget", particleBudget), 100, 10000);

            farEntityCullDistance = clamp(
                    getInt(properties, "far_entity_cull_distance", farEntityCullDistance),
                    16, 128);
            maxChunkUploadsPerFrame = clamp(
                    getInt(properties, "max_chunk_uploads_per_frame", maxChunkUploadsPerFrame),
                    1, 32);
            chunkUploadBudgetMicros = clamp(
                    getInt(properties, "chunk_upload_budget_micros", chunkUploadBudgetMicros),
                    250, 10000);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read config; using safe defaults.", exception);
            resetDefaults();
        }

        save();
    }

    public static void save() {
        try {
            Path parent = CONFIG_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Properties properties = new Properties();
            properties.setProperty("enabled", Boolean.toString(enabled));
            properties.setProperty("adaptive", Boolean.toString(adaptive));
            properties.setProperty(
                    "particle_limiter", Boolean.toString(particleLimiter));
            properties.setProperty(
                    "deep_entity_culling", Boolean.toString(deepEntityCulling));
            properties.setProperty(
                    "fast_entity_shadows", Boolean.toString(fastEntityShadows));
            properties.setProperty(
                    "chunk_upload_budget", Boolean.toString(chunkUploadBudget));

            properties.setProperty(
                    "min_render_distance", Integer.toString(minRenderDistance));
            properties.setProperty(
                    "min_entity_distance", Double.toString(minEntityDistance));
            properties.setProperty(
                    "fps_floor", Integer.toString(fpsFloor));
            properties.setProperty(
                    "particle_budget", Integer.toString(particleBudget));

            properties.setProperty(
                    "far_entity_cull_distance", Integer.toString(farEntityCullDistance));
            properties.setProperty(
                    "max_chunk_uploads_per_frame",
                    Integer.toString(maxChunkUploadsPerFrame));
            properties.setProperty(
                    "chunk_upload_budget_micros",
                    Integer.toString(chunkUploadBudgetMicros));

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                properties.store(writer, "Optimizator 0.2 - deep client optimization settings");
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save config: {}", CONFIG_FILE, exception);
        }
    }

    public static void resetDefaults() {
        enabled = true;
        adaptive = true;
        particleLimiter = true;
        deepEntityCulling = true;
        fastEntityShadows = true;
        chunkUploadBudget = true;

        minRenderDistance = 5;
        minEntityDistance = 0.35D;
        fpsFloor = 35;
        particleBudget = 1200;

        farEntityCullDistance = 48;
        maxChunkUploadsPerFrame = 6;
        chunkUploadBudgetMicros = 2500;
    }

    private static boolean getBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int getInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(
                    properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double getDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(
                    properties.getProperty(key, Double.toString(fallback)).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
