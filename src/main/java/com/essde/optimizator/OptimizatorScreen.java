package com.essde.optimizator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class OptimizatorScreen extends Screen {
    private final Screen parent;

    private ButtonWidget enabledButton;
    private ButtonWidget adaptiveButton;
    private ButtonWidget entityButton;
    private ButtonWidget shadowButton;
    private ButtonWidget uploadButton;

    public OptimizatorScreen(Screen parent) {
        super(Text.literal("Optimizator"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int buttonWidth = Math.min(220, this.width - 20);
        int left = center - buttonWidth / 2;
        int y = 70;

        this.enabledButton = addDrawableChild(toggleButton(
                left, y, "Optimizator", () -> OptimizatorConfig.enabled,
                value -> OptimizatorConfig.enabled = value));
        y += 24;

        this.adaptiveButton = addDrawableChild(toggleButton(
                left, y, "Adaptive controller", () -> OptimizatorConfig.adaptive,
                value -> OptimizatorConfig.adaptive = value));
        y += 24;

        this.entityButton = addDrawableChild(toggleButton(
                left, y, "Deep entity culling", () -> OptimizatorConfig.deepEntityCulling,
                value -> OptimizatorConfig.deepEntityCulling = value));
        y += 24;

        this.shadowButton = addDrawableChild(toggleButton(
                left, y, "Fast entity shadows", () -> OptimizatorConfig.fastEntityShadows,
                value -> OptimizatorConfig.fastEntityShadows = value));
        y += 24;

        this.uploadButton = addDrawableChild(toggleButton(
                left, y, "Chunk upload budget", () -> OptimizatorConfig.chunkUploadBudget,
                value -> OptimizatorConfig.chunkUploadBudget = value));
        y += 28;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Particle settings"),
                        button -> this.client.setScreen(new ParticleSettingsScreen(this)))
                .dimensions(left, y, buttonWidth, 20)
                .build());
        y += 28;

        addDrawableChild(new SliderWidget(
                left, y, buttonWidth, 20,
                Text.literal("Far entity cull: " + OptimizatorConfig.farEntityCullDistance + "m"),
                (OptimizatorConfig.farEntityCullDistance - 16D) / 112D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal(
                        "Far entity cull: " + OptimizatorConfig.farEntityCullDistance + "m"));
            }

            @Override
            protected void applyValue() {
                OptimizatorConfig.farEntityCullDistance =
                        16 + (int) Math.round(this.value * 112D);
                OptimizatorConfig.save();
            }
        });
        y += 28;

        addDrawableChild(new SliderWidget(
                left, y, buttonWidth, 20,
                Text.literal("Max chunk uploads: " + OptimizatorConfig.maxChunkUploadsPerFrame),
                (OptimizatorConfig.maxChunkUploadsPerFrame - 1D) / 31D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal(
                        "Max chunk uploads: " + OptimizatorConfig.maxChunkUploadsPerFrame));
            }

            @Override
            protected void applyValue() {
                OptimizatorConfig.maxChunkUploadsPerFrame =
                        1 + (int) Math.round(this.value * 31D);
                OptimizatorConfig.save();
            }
        });
        y += 28;

        addDrawableChild(new SliderWidget(
                left, y, buttonWidth, 20,
                Text.literal("Chunk upload budget: "
                        + formatMillis(OptimizatorConfig.chunkUploadBudgetMicros)),
                (OptimizatorConfig.chunkUploadBudgetMicros - 250D) / 9750D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal(
                        "Chunk upload budget: "
                                + formatMillis(OptimizatorConfig.chunkUploadBudgetMicros)));
            }

            @Override
            protected void applyValue() {
                OptimizatorConfig.chunkUploadBudgetMicros =
                        250 + (int) Math.round(this.value * 9750D);
                OptimizatorConfig.save();
            }
        });
        y += 32;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Reset defaults"),
                        button -> {
                            OptimizatorConfig.resetDefaults();
                            OptimizatorConfig.save();
                            clearAndInit();
                        })
                .dimensions(left, y, buttonWidth, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Done"),
                        button -> {
                            OptimizatorConfig.save();
                            this.client.setScreen(this.parent);
                        })
                .dimensions(left, y + 25, buttonWidth, 20)
                .build());

        updateToggleLabels();
    }

    private ButtonWidget toggleButton(
            int x,
            int y,
            String label,
            java.util.function.BooleanSupplier getter,
            java.util.function.Consumer<Boolean> setter
    ) {
        return ButtonWidget.builder(
                        Text.literal(label + ": " + (getter.getAsBoolean() ? "ON" : "OFF")),
                        button -> {
                            boolean next = !getter.getAsBoolean();
                            setter.accept(next);
                            button.setMessage(Text.literal(
                                    label + ": " + (next ? "ON" : "OFF")));
                            OptimizatorConfig.save();
                        })
                .dimensions(x, y, 220, 20)
                .build();
    }

    private void updateToggleLabels() {
        this.enabledButton.setMessage(
                Text.literal("Optimizator: " + onOff(OptimizatorConfig.enabled)));
        this.adaptiveButton.setMessage(
                Text.literal("Adaptive controller: " + onOff(OptimizatorConfig.adaptive)));
        this.entityButton.setMessage(
                Text.literal("Deep entity culling: " + onOff(OptimizatorConfig.deepEntityCulling)));
        this.shadowButton.setMessage(
                Text.literal("Fast entity shadows: " + onOff(OptimizatorConfig.fastEntityShadows)));
        this.uploadButton.setMessage(
                Text.literal("Chunk upload budget: " + onOff(OptimizatorConfig.chunkUploadBudget)));
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String formatMillis(int micros) {
        return String.format(java.util.Locale.ROOT, "%.2fms", micros / 1000.0D);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(270, this.width - 10);
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = 48;
        int panelBottom = Math.min(this.height - 16, panelTop + 350);

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xD0101010);
        context.fill(panelLeft + 1, panelTop + 1,
                panelLeft + panelWidth - 1, panelTop + 2, 0xFF3F3F3F);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                58,
                0xFFFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Deep client rendering optimizations"),
                this.width / 2,
                panelBottom - 30,
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
