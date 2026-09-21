package com.essde.optimizator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class ParticleSettingsScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget disabledField;
    private TextFieldWidget reducedField;

    public ParticleSettingsScreen(Screen parent) {
        super(Text.literal("Particle settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int width = Math.min(300, this.width - 20);
        int left = center - width / 2;
        int y = 55;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal(qualityLabel()),
                        button -> {
                            OptimizatorConfig.particleQuality =
                                    (OptimizatorConfig.particleQuality + 1) % 3;
                            if (OptimizatorConfig.particleQuality != 0) {
                                OptimizatorConfig.enabled = true;
                            }
                            button.setMessage(Text.literal(qualityLabel()));
                            OptimizatorConfig.save();
                        })
                .dimensions(left, y, width, 20)
                .build());
        y += 25;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Particle limiter: " + onOff(OptimizatorConfig.particleLimiter)),
                        button -> {
                            OptimizatorConfig.particleLimiter = !OptimizatorConfig.particleLimiter;
                            if (OptimizatorConfig.particleLimiter) {
                                OptimizatorConfig.enabled = true;
                            }
                            button.setMessage(Text.literal(
                                    "Particle limiter: " + onOff(OptimizatorConfig.particleLimiter)));
                            OptimizatorConfig.save();
                        })
                .dimensions(left, y, width, 20)
                .build());
        y += 25;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Distance culling: " + onOff(OptimizatorConfig.particleCulling)),
                        button -> {
                            OptimizatorConfig.particleCulling = !OptimizatorConfig.particleCulling;
                            if (OptimizatorConfig.particleCulling) {
                                OptimizatorConfig.enabled = true;
                            }
                            button.setMessage(Text.literal(
                                    "Distance culling: " + onOff(OptimizatorConfig.particleCulling)));
                            OptimizatorConfig.save();
                        })
                .dimensions(left, y, width, 20)
                .build());
        y += 30;

        addDrawableChild(new SliderWidget(
                left, y, width, 20,
                Text.literal("Particle budget: " + OptimizatorConfig.particleBudget + "/s"),
                (OptimizatorConfig.particleBudget - 100D) / 9900D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal(
                        "Particle budget: " + OptimizatorConfig.particleBudget + "/s"));
            }

            @Override
            protected void applyValue() {
                OptimizatorConfig.particleBudget =
                        100 + (int) Math.round(this.value * 9900D);
                OptimizatorConfig.enabled = true;
                OptimizatorConfig.save();
            }
        });
        y += 27;

        addDrawableChild(new SliderWidget(
                left, y, width, 20,
                Text.literal("Cull distance: " + OptimizatorConfig.particleCullDistance + "m"),
                (OptimizatorConfig.particleCullDistance - 16D) / 240D
        ) {
            @Override
            protected void updateMessage() {
                setMessage(Text.literal(
                        "Cull distance: " + OptimizatorConfig.particleCullDistance + "m"));
            }

            @Override
            protected void applyValue() {
                OptimizatorConfig.particleCullDistance =
                        16 + (int) Math.round(this.value * 240D);
                OptimizatorConfig.enabled = true;
                OptimizatorConfig.save();
            }
        });
        y += 35;

        disabledField = new TextFieldWidget(
                this.textRenderer, left, y, width, 20, Text.literal("Disabled particles"));
        disabledField.setMaxLength(4000);
        disabledField.setText(String.join(",", OptimizatorConfig.particleDisabledTypes));
        disabledField.setPlaceholder(Text.literal("minecraft:smoke,minecraft:campfire_cosy_smoke"));
        addDrawableChild(disabledField);
        y += 25;

        reducedField = new TextFieldWidget(
                this.textRenderer, left, y, width, 20, Text.literal("Reduced particles"));
        reducedField.setMaxLength(4000);
        reducedField.setText(String.join(",", OptimizatorConfig.particleReducedTypes));
        reducedField.setPlaceholder(Text.literal("minecraft:ash,minecraft:ambient_entity_effect"));
        addDrawableChild(reducedField);
        y += 35;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Apply and back"),
                        button -> {
                            applyFields();
                            this.client.setScreen(this.parent);
                        })
                .dimensions(left, y, width, 20)
                .build());
        y += 25;

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Reset particle defaults (OFF)"),
                        button -> {
                            OptimizatorConfig.particleLimiter = false;
                            OptimizatorConfig.particleCulling = false;
                            OptimizatorConfig.particleQuality = 0;
                            OptimizatorConfig.particleBudget = 10000;
                            OptimizatorConfig.particleCullDistance = 128;
                            OptimizatorConfig.particleDisabledTypes.clear();
                            OptimizatorConfig.particleReducedTypes.clear();
                            OptimizatorConfig.save();
                            clearAndInit();
                        })
                .dimensions(left, y, width, 20)
                .build());
    }

    private void applyFields() {
        OptimizatorConfig.particleDisabledTypes.clear();
        OptimizatorConfig.particleReducedTypes.clear();

        String disabled = disabledField.getText();
        if (!disabled.isBlank()) {
            for (String value : disabled.split(",")) {
                String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
                if (!normalized.isEmpty()) {
                    OptimizatorConfig.particleDisabledTypes.add(normalized);
                }
            }
        }

        String reduced = reducedField.getText();
        if (!reduced.isBlank()) {
            for (String value : reduced.split(",")) {
                String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
                if (!normalized.isEmpty()) {
                    OptimizatorConfig.particleReducedTypes.add(normalized);
                }
            }
        }

        if (!OptimizatorConfig.particleDisabledTypes.isEmpty()
                || !OptimizatorConfig.particleReducedTypes.isEmpty()) {
            OptimizatorConfig.enabled = true;
        }

        OptimizatorConfig.save();
    }

    private String qualityLabel() {
        return "Particle quality: " + switch (OptimizatorConfig.particleQuality) {
            case 2 -> "MINIMAL";
            case 1 -> "DECREASED";
            default -> "ALL";
        };
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(330, this.width - 10);
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = 35;
        int panelBottom = Math.min(this.height - 8, panelTop + 365);

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xD0101010);
        context.fill(panelLeft + 1, panelTop + 1,
                panelLeft + panelWidth - 1, panelTop + 2, 0xFF3F3F3F);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                43,
                0xFFFFFFFF
        );

        context.drawTextWithShadow(
                this.textRenderer,
                Text.literal("Use minecraft:particle_id; wildcards: minecraft:*"),
                panelLeft + 8,
                panelBottom - 27,
                0xFFAAAAAA
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        applyFields();
        this.client.setScreen(this.parent);
    }
}
