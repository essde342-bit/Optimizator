package com.essde.optimizator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ProfilerScreen extends Screen {
    private final Screen parent;

    public ProfilerScreen(Screen parent) {
        super(Text.literal("Optimizator Profiler"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(300, this.width - 20);
        int left = (this.width - width) / 2;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Profiler: " + (OptimizatorConfig.profilerEnabled ? "ON" : "OFF")),
                button -> {
                    OptimizatorConfig.profilerEnabled = !OptimizatorConfig.profilerEnabled;
                    button.setMessage(Text.literal(
                            "Profiler: " + (OptimizatorConfig.profilerEnabled ? "ON" : "OFF")));
                    OptimizatorConfig.save();
                }).dimensions(left, 65, width, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Overlay: " + (OptimizatorConfig.profilerOverlay ? "ON" : "OFF")),
                button -> {
                    OptimizatorConfig.profilerOverlay = !OptimizatorConfig.profilerOverlay;
                    button.setMessage(Text.literal(
                            "Overlay: " + (OptimizatorConfig.profilerOverlay ? "ON" : "OFF")));
                    OptimizatorConfig.save();
                }).dimensions(left, 90, width, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Reset profiler counters"),
                button -> PerformanceProfiler.resetCounters()
        ).dimensions(left, 115, width, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                button -> this.client.setScreen(parent)
        ).dimensions(left, 145, width, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int width = Math.min(340, this.width - 10);
        int left = (this.width - width) / 2;
        int top = 35;
        int bottom = Math.min(this.height - 8, 390);

        context.fill(left, top, left + width, bottom, 0xD0101010);
        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 44, 0xFFFFFFFF);

        MinecraftClient client = this.client;
        if (client != null) {
            int y = 180;
            drawStat(context, "FPS", Integer.toString(client.getCurrentFps()), left + 10, y); y += 15;
            drawStat(context, "Frame", format(PerformanceProfiler.frameMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "Average", format(PerformanceProfiler.averageFrameMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "1% sample", format(PerformanceProfiler.onePercentLowMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "World", format(PerformanceProfiler.worldMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "Entities", format(PerformanceProfiler.entitiesMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "Block entities", format(PerformanceProfiler.blockEntitiesMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "Particles", format(PerformanceProfiler.particlesMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "Chunk upload", format(PerformanceProfiler.chunkUploadMs()) + " ms", left + 10, y); y += 15;
            drawStat(context, "Particle created", Long.toString(PerformanceProfiler.particlesCreated()), left + 10, y); y += 15;
            drawStat(context, "Particle rejected", Long.toString(PerformanceProfiler.particlesRejected()), left + 10, y); y += 15;
            drawStat(context, "TOP BOTTLENECK", PerformanceProfiler.bottleneckName(client), left + 10, y);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStat(DrawContext context, String name, String value, int x, int y) {
        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal(name + ": " + value),
                x, y, 0xFFE0E0E0);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override
    public void close() {
        OptimizatorConfig.save();
        this.client.setScreen(parent);
    }
}
