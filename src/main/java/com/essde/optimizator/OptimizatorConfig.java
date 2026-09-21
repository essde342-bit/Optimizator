package com.essde.optimizator;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

public final class OptimizatorConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Optimizator");
    private static final Path CONFIG_FILE =
            FabricLoader.getInstance().getConfigDir().resolve("optimizator.properties");

    public static boolean enabled = true;
    public static boolean adaptive = true;
    public static boolean particleLimiter = true;
    public static boolean particleCulling = false;
    public static boolean deepEntityCulling = true;
    public static boolean fastEntityShadows = false;
    public static boolean chunkUploadBudget = true;
    public static boolean disableCloudsUnderLoad = true;
    public static boolean profilerEnabled = true;
    public static boolean profilerOverlay = false;

    /**
     * 0 = ALL, 1 = DECREASED, 2 = MINIMAL.
     * The default is ALL so Optimizator does not silently reduce visible
     * particle density compared with the normal Minecraft setting.
     */
    public static int particleQuality = 0;

    public static int minRenderDistance = 5;
    public static double minEntityDistance = 0.35D;
    public static int fpsFloor = 35;

    /**
     * Large enough to stay visually equivalent in normal scenes.
     * The adaptive controller can lower the effective budget under sustained load.
     */
    public static int particleBudget = 10000;

    public static int farEntityCullDistance = 48;
    public static int particleCullDistance = 128;
    public static int maxChunkUploadsPerFrame = 6;
    public static int chunkUploadBudgetMicros = 2500;

    public static final Set<String> particleDisabledTypes = new LinkedHashSet<>();
    public static final Set<String> particleReducedTypes = new LinkedHashSet<>();

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
            particleLimiter =
                    getBoolean(properties, "particle_limiter", particleLimiter);
            particleCulling =
                    getBoolean(properties, "particle_culling", particleCulling);
            deepEntityCulling =
                    getBoolean(properties, "deep_entity_culling", deepEntityCulling);
            fastEntityShadows =
                    getBoolean(properties, "fast_entity_shadows", fastEntityShadows);
            chunkUploadBudget =
                    getBoolean(properties, "chunk_upload_budget", chunkUploadBudget);
            disableCloudsUnderLoad =
                    getBoolean(properties, "disable_clouds_under_load", disableCloudsUnderLoad);

            particleQuality =
                    clamp(getInt(properties, "particle_quality", particleQuality), 0, 2);
            minRenderDistance =
                    clamp(getInt(properties, "min_render_distance", minRenderDistance), 4, 32);
            minEntityDistance =
                    clamp(getDouble(properties, "min_entity_distance", minEntityDistance),
                            0.15D, 1.0D);
            fpsFloor =
                    clamp(getInt(properties, "fps_floor", fpsFloor), 20, 120);
            particleBudget =
                    clamp(getInt(properties, "particle_budget", particleBudget), 100, 10000);

            farEntityCullDistance =
                    clamp(getInt(properties, "far_entity_cull_distance", farEntityCullDistance),
                            16, 128);
            particleCullDistance =
                    clamp(getInt(properties, "particle_cull_distance", particleCullDistance),
                            16, 256);
            maxChunkUploadsPerFrame =
                    clamp(getInt(properties, "max_chunk_uploads_per_frame",
                            maxChunkUploadsPerFrame), 1, 32);
            chunkUploadBudgetMicros =
                    clamp(getInt(properties, "chunk_upload_budget_micros",
                            chunkUploadBudgetMicros), 250, 10000);

            parseTypeSet(properties.getProperty("particle_disabled", ""), particleDisabledTypes);
            parseTypeSet(properties.getProperty("particle_reduced", ""), particleReducedTypes);
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
            properties.setProperty("particle_limiter", Boolean.toString(particleLimiter));
            properties.setProperty("particle_culling", Boolean.toString(particleCulling));
            properties.setProperty("deep_entity_culling", Boolean.toString(deepEntityCulling));
            properties.setProperty("fast_entity_shadows", Boolean.toString(fastEntityShadows));
            properties.setProperty("chunk_upload_budget", Boolean.toString(chunkUploadBudget));
            properties.setProperty(
                    "disable_clouds_under_load", Boolean.toString(disableCloudsUnderLoad));
            properties.setProperty("profiler_enabled", Boolean.toString(profilerEnabled));
            properties.setProperty("profiler_overlay", Boolean.toString(profilerOverlay));

            properties.setProperty("particle_quality", Integer.toString(particleQuality));
            properties.setProperty("min_render_distance", Integer.toString(minRenderDistance));
            properties.setProperty("min_entity_distance", Double.toString(minEntityDistance));
            properties.setProperty("fps_floor", Integer.toString(fpsFloor));
            properties.setProperty("particle_budget", Integer.toString(particleBudget));

            properties.setProperty(
                    "far_entity_cull_distance", Integer.toString(farEntityCullDistance));
            properties.setProperty(
                    "particle_cull_distance", Integer.toString(particleCullDistance));
            properties.setProperty(
                    "max_chunk_uploads_per_frame",
                    Integer.toString(maxChunkUploadsPerFrame));
            properties.setProperty(
                    "chunk_upload_budget_micros",
                    Integer.toString(chunkUploadBudgetMicros));

            properties.setProperty(
                    "particle_disabled", String.join(",", particleDisabledTypes));
            properties.setProperty(
                    "particle_reduced", String.join(",", particleReducedTypes));

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                properties.store(writer, "Optimizator - deep client optimization settings");
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save config: {}", CONFIG_FILE, exception);
        }
    }

    public static void resetDefaults() {
        enabled = true;
        adaptive = true;
        particleLimiter = true;
        particleCulling = false;
        deepEntityCulling = true;
        fastEntityShadows = false;
        chunkUploadBudget = true;
        disableCloudsUnderLoad = true;
        profilerEnabled = true;
        profilerOverlay = false;

        particleQuality = 0;
        minRenderDistance = 5;
        minEntityDistance = 0.35D;
        fpsFloor = 35;
        particleBudget = 10000;

        farEntityCullDistance = 48;
        particleCullDistance = 128;
        maxChunkUploadsPerFrame = 6;
        chunkUploadBudgetMicros = 2500;

        particleDisabledTypes.clear();
        particleReducedTypes.clear();
    }

    public static int getParticleMode(String typeId) {
        if (matches(typeId, particleDisabledTypes)) {
            return 2;
        }
        if (matches(typeId, particleReducedTypes)) {
            return 1;
        }
        return 0;
    }

    private static boolean matches(String typeId, Set<String> rules) {
        String normalized = typeId.toLowerCase(Locale.ROOT);
        for (String rawRule : rules) {
            String rule = rawRule.trim().toLowerCase(Locale.ROOT);
            if (rule.isEmpty()) {
                continue;
            }
            if (rule.endsWith("*")) {
                if (normalized.startsWith(rule.substring(0, rule.length() - 1))) {
                    return true;
                }
            } else if (normalized.equals(rule)) {
                return true;
            }
        }
        return false;
    }

    private static void parseTypeSet(String value, Set<String> target) {
        target.clear();
        if (value == null || value.isBlank()) {
            return;
        }

        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(s -> target.add(s.toLowerCase(Locale.ROOT)));
    }

    private static boolean getBoolean(
            Properties properties, String key, boolean fallback) {
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

    private static double getDouble(
            Properties properties, String key, double fallback) {
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
