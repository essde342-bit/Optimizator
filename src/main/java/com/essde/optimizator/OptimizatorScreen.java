package com.essde.optimizator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class OptimizatorScreen extends Screen {
    private final Screen parent;
    private int page = 0;

    private ButtonWidget enabledButton;
    private ButtonWidget adaptiveButton;
    private ButtonWidget profilerButton;
    private ButtonWidget overlayButton;
    private ButtonWidget entityButton;
    private ButtonWidget shadowButton;
    private ButtonWidget uploadButton;
    private ButtonWidget cloudButton;

    public OptimizatorScreen(Screen parent) {
        super(Text.literal("Optimizator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int buttonWidth = Math.min(240, this.width - 20);
        int left = center - buttonWidth / 2;
        int y = 64;

        if (page == 0) {
            this.enabledButton = addDrawableChild(toggleButton(
                    left, y, "Master switch",
                    () -> OptimizatorConfig.enabled,
                    value -> OptimizatorConfig.enabled = value));
            y += 24;

            this.adaptiveButton = addDrawableChild(toggleButton(
                    left, y, "Adaptive controller",
                    () -> OptimizatorConfig.adaptive,
                    value -> OptimizatorConfig.adaptive = value));
            y += 24;

            this.profilerButton = addDrawableChild(toggleButton(
                    left, y, "Profiler",
                    () -> OptimizatorConfig.profilerEnabled,
                    value -> OptimizatorConfig.profilerEnabled = value));
            y += 24;

            this.overlayButton = addDrawableChild(toggleButton(
                    left, y, "Profiler overlay",
                    () -> OptimizatorConfig.profilerOverlay,
                    value -> OptimizatorConfig.profilerOverlay = value));
            y += 30;

            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Particle settings"),
                            button -> this.client.setScreen(new ParticleSettingsScreen(this)))
                    .dimensions(left, y, buttonWidth, 20)
                    .build());
            y += 25;

            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Performance profiler"),
                            button -> this.client.setScreen(new ProfilerScreen(this)))
                    .dimensions(left, y, buttonWidth, 20)
                    .build());
        } else if (page == 1) {
            this.entityButton = addDrawableChild(toggleButton(
                    left, y, "Deep entity culling",
                    () -> OptimizatorConfig.deepEntityCulling,
                    value -> OptimizatorConfig.deepEntityCulling = value));
            y += 24;

            addDrawableChild(intSlider(
                    left, y, buttonWidth,
                    "Far entity cull",
                    OptimizatorConfig.farEntityCullDistance, 16, 128,
                    value -> OptimizatorConfig.farEntityCullDistance = value));
            y += 25;

            this.shadowButton = addDrawableChild(toggleButton(
                    left, y, "Fast entity shadows",
                    () -> OptimizatorConfig.fastEntityShadows,
                    value -> OptimizatorConfig.fastEntityShadows = value));
            y += 24;

            addDrawableChild(intSlider(
                    left, y, buttonWidth,
                    "Adaptive min render distance",
                    OptimizatorConfig.minRenderDistance, 4, 32,
                    value -> OptimizatorConfig.minRenderDistance = value));
            y += 25;

            addDrawableChild(doubleSlider(
                    left, y, buttonWidth,
                    "Adaptive min entity distance",
                    OptimizatorConfig.minEntityDistance, 0.15D, 1.0D,
                    value -> OptimizatorConfig.minEntityDistance = value));
            y += 25;

            addDrawableChild(intSlider(
                    left, y, buttonWidth,
                    "Adaptive FPS floor",
                    OptimizatorConfig.fpsFloor, 20, 120,
                    value -> OptimizatorConfig.fpsFloor = value));
            y += 25;

            this.cloudButton = addDrawableChild(toggleButton(
                    left, y, "Disable clouds under load",
                    () -> OptimizatorConfig.disableCloudsUnderLoad,
                    value -> OptimizatorConfig.disableCloudsUnderLoad = value));
        } else {
            this.uploadButton = addDrawableChild(toggleButton(
                    left, y, "Chunk upload budget",
                    () -> OptimizatorConfig.chunkUploadBudget,
                    value -> OptimizatorConfig.chunkUploadBudget = value));
            y += 24;

            addDrawableChild(intSlider(
                    left, y, buttonWidth,
                    "Max chunk uploads",
                    OptimizatorConfig.maxChunkUploadsPerFrame, 1, 32,
                    value -> OptimizatorConfig.maxChunkUploadsPerFrame = value));
            y += 25;

            addDrawableChild(intSlider(
                    left, y, buttonWidth,
                    "Chunk upload budget (µs)",
                    OptimizatorConfig.chunkUploadBudgetMicros, 250, 10000,
                    value -> OptimizatorConfig.chunkUploadBudgetMicros = value));
            y += 30;

            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Particle settings"),
                            button -> this.client.setScreen(new ParticleSettingsScreen(this)))
                    .dimensions(left, y, buttonWidth, 20)
                    .build());
            y += 25;

            addDrawableChild(ButtonWidget.builder(
                            Text.literal("Reset ALL defaults (OFF)"),
                            button -> {
                                OptimizatorConfig.resetDefaults();
                                OptimizatorConfig.save();
                                clearAndInit();
                            })
                    .dimensions(left, y, buttonWidth, 20)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Page " + (page + 1) + "/3"),
                        button -> {
                            page = (page + 1) % 3;
                            clearAndInit();
                        })
                .dimensions(left, this.height - 55, buttonWidth / 2 - 4, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> {
                            OptimizatorConfig.save();
                            this.client.setScreen(this.parent);
                        })
                .dimensions(left + buttonWidth / 2 + 4, this.height - 55,
                        buttonWidth / 2 - 4, 20)
                .build());
    }

    private ButtonWidget toggleButton(
            int x,
            int y,
            String label,
            BooleanSupplier getter,
            Consumer<Boolean> setter
    ) {
        return ButtonWidget.builder(
                        Text.literal(label + ": " + onOff(getter.getAsBoolean())),
                        button -> {
                            boolean next = !getter.getAsBoolean();
                            setter.accept(next);
                            button.setMessage(Text.literal(
                                    label + ": " + onOff(next)));
                            OptimizatorConfig.save();
                        })
                .dimensions(x, y, 240, 20)
                .build();
    }

    private <T> SliderWidget intSlider(
            int x, int y, int width,
            String label, int current, int min, int max,
            Consumer<Integer> setter
    ) {
        return new SliderWidget(
                x, y, width, 20,
                Text.literal(label + ": " + current),
                (current - (double) min) / (max - min)
        ) {
            @Override
            protected void updateMessage() {
                int value = min + (int) Math.round(this.value * (max - min));
                setMessage(Text.literal(label + ": " + value));
            }

            @Override
            protected void applyValue() {
                int value = min + (int) Math.round(this.value * (max - min));
                setter.accept(value);
                OptimizatorConfig.save();
            }
        };
    }

    private SliderWidget doubleSlider(
            int x, int y, int width,
            String label, double current, double min, double max,
            Consumer<Double> setter
    ) {
        return new SliderWidget(
                x, y, width, 20,
                Text.literal(String.format(Locale.ROOT, "%s: %.2f", label, current)),
                (current - min) / (max - min)
        ) {
            @Override
            protected void updateMessage() {
                double value = min + this.value * (max - min);
                setMessage(Text.literal(
                        String.format(Locale.ROOT, "%s: %.2f", label, value)));
            }

            @Override
            protected void applyValue() {
                double value = min + this.value * (max - min);
                setter.accept(value);
                OptimizatorConfig.save();
            }
        };
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(280, this.width - 10);
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = 40;
        int panelBottom = this.height - 16;

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xD0101010);
        context.fill(panelLeft + 1, panelTop + 1,
                panelLeft + panelWidth - 1, panelTop + 2, 0xFF3F3F3F);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                49,
                0xFFFFFFFF
        );

        String pageName = switch (page) {
            case 0 -> "GENERAL / PROFILER";
            case 1 -> "ENTITIES / ADAPTIVE";
            default -> "CHUNKS";
        };
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(pageName),
                this.width / 2,
                31,
                0xFFAAAAAA
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("All optimizations are OFF by default"),
                this.width / 2,
                panelBottom - 25,
                0xFFAAAAAA
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        OptimizatorConfig.save();
        this.client.setScreen(this.parent);
    }
}
