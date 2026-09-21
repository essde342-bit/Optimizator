package com.essde.optimizator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ProfilerScreen extends Screen {
    private final Screen parent;

    public ProfilerScreen(Screen parent) {
        super(Text.translatable("screen.profiler.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(310, this.width - 18);
        int left = (this.width - width) / 2;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("profiler.option.enabled",
                        status(OptimizatorConfig.profilerEnabled)),
                button -> {
                    OptimizatorConfig.profilerEnabled =
                            !OptimizatorConfig.profilerEnabled;
                    if (OptimizatorConfig.profilerEnabled) {
                        OptimizatorConfig.enabled = true;
                    }
                    button.setMessage(Text.translatable(
                            "profiler.option.enabled",
                            status(OptimizatorConfig.profilerEnabled)));
                    OptimizatorConfig.save();
                }).dimensions(left, rowY(0), width, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("profiler.option.overlay",
                        status(OptimizatorConfig.profilerOverlay)),
                button -> {
                    OptimizatorConfig.profilerOverlay =
                            !OptimizatorConfig.profilerOverlay;
                    if (OptimizatorConfig.profilerOverlay) {
                        OptimizatorConfig.profilerEnabled = true;
                        OptimizatorConfig.enabled = true;
                    }
                    button.setMessage(Text.translatable(
                            "profiler.option.overlay",
                            status(OptimizatorConfig.profilerOverlay)));
                    OptimizatorConfig.save();
                }).dimensions(left, rowY(1), width, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("profiler.option.reset"),
                button -> PerformanceProfiler.resetCounters()
        ).dimensions(left, rowY(2), width, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("profiler.navigation.done"),
                button -> this.client.setScreen(parent)
        ).dimensions(left, rowY(3), width, 20).build());
    }

    private int rowY(int row) {
        return 80 + row * 22;
    }

    private String status(boolean value) {
        return Text.translatable(value
                ? "particles.status.on"
                : "particles.status.off").getString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int width = Math.min(330, this.width - 10);
        int left = (this.width - width) / 2;
        int top = 28;
        int bottom = this.height - 6;

        context.fill(left, top, left + width, bottom, 0xD0101010);
        context.fill(left + 1, top + 1, left + width - 1, top + 2, 0xFF3F3F3F);

        context.drawCenteredTextWithShadow(
                this.textRenderer, this.title, this.width / 2, 35, 0xFFFFFFFF);

        MinecraftClient client = this.client;
        if (client != null) {
            int y = 146;
            drawStat(context, "profiler.stat.fps", Integer.toString(client.getCurrentFps()), left + 10, y); y += 14;
            drawStat(context, "profiler.stat.frame", format(PerformanceProfiler.frameMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.average", format(PerformanceProfiler.averageFrameMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.low", format(PerformanceProfiler.onePercentLowMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.world", format(PerformanceProfiler.worldMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.entities", format(PerformanceProfiler.entitiesMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.particles", format(PerformanceProfiler.particlesMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.chunk_upload", format(PerformanceProfiler.chunkUploadMs()) + " ms", left + 10, y); y += 14;
            drawStat(context, "profiler.stat.top", PerformanceProfiler.bottleneckName(client), left + 10, y);
        }

        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable("profiler.help"),
                left + 10,
                bottom - 18,
                0xFF777777
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawStat(DrawContext context, String key, String value, int x, int y) {
        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable(key, value),
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
