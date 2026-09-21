package com.essde.optimizator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class ParticleSettingsScreen extends Screen {
    private final Screen parent;
    private int page = 0;
    private int contentWidth;

    private TextFieldWidget disabledField;
    private TextFieldWidget reducedField;

    public ParticleSettingsScreen(Screen parent) {
        super(Text.translatable("screen.particles.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearHelpFields();

        int width = Math.min(310, this.width - 18);
        int left = (this.width - width) / 2;
        this.contentWidth = width;

        if (page == 0) {
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("particles.option.quality",
                            qualityLabel()),
                    button -> {
                        OptimizatorConfig.particleQuality =
                                (OptimizatorConfig.particleQuality + 1) % 3;
                        if (OptimizatorConfig.particleQuality != 0) {
                            OptimizatorConfig.enabled = true;
                        }
                        button.setMessage(Text.translatable(
                                "particles.option.quality", qualityLabel()));
                        OptimizatorConfig.save();
                    }).dimensions(left, rowY(0), width, 20).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("particles.option.limiter",
                            status(OptimizatorConfig.particleLimiter)),
                    button -> {
                        OptimizatorConfig.particleLimiter =
                                !OptimizatorConfig.particleLimiter;
                        if (OptimizatorConfig.particleLimiter) {
                            OptimizatorConfig.enabled = true;
                        }
                        button.setMessage(Text.translatable(
                                "particles.option.limiter",
                                status(OptimizatorConfig.particleLimiter)));
                        OptimizatorConfig.save();
                    }).dimensions(left, rowY(1), width, 20).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("particles.option.culling",
                            status(OptimizatorConfig.particleCulling)),
                    button -> {
                        OptimizatorConfig.particleCulling =
                                !OptimizatorConfig.particleCulling;
                        if (OptimizatorConfig.particleCulling) {
                            OptimizatorConfig.enabled = true;
                        }
                        button.setMessage(Text.translatable(
                                "particles.option.culling",
                                status(OptimizatorConfig.particleCulling)));
                        OptimizatorConfig.save();
                    }).dimensions(left, rowY(2), width, 20).build());

            addDrawableChild(new SliderWidget(
                    left, rowY(3), width, 20,
                    Text.translatable("particles.option.budget",
                            OptimizatorConfig.particleBudget),
                    (OptimizatorConfig.particleBudget - 100D) / 9900D
            ) {
                @Override
                protected void updateMessage() {
                    setMessage(Text.translatable(
                            "particles.option.budget",
                            OptimizatorConfig.particleBudget));
                }

                @Override
                protected void applyValue() {
                    OptimizatorConfig.particleBudget =
                            100 + (int) Math.round(this.value * 9900D);
                    OptimizatorConfig.save();
                }
            });

            addDrawableChild(new SliderWidget(
                    left, rowY(4), width, 20,
                    Text.translatable("particles.option.distance",
                            OptimizatorConfig.particleCullDistance),
                    (OptimizatorConfig.particleCullDistance - 16D) / 240D
            ) {
                @Override
                protected void updateMessage() {
                    setMessage(Text.translatable(
                            "particles.option.distance",
                            OptimizatorConfig.particleCullDistance));
                }

                @Override
                protected void applyValue() {
                    OptimizatorConfig.particleCullDistance =
                            16 + (int) Math.round(this.value * 240D);
                    OptimizatorConfig.save();
                }
            });
        } else {
            this.disabledField = new TextFieldWidget(
                    this.textRenderer, left, rowY(0), width, 20,
                    Text.translatable("particles.option.disabled"));
            disabledField.setMaxLength(4000);
            disabledField.setText(
                    String.join(",", OptimizatorConfig.particleDisabledTypes));
            disabledField.setPlaceholder(
                    Text.translatable("particles.placeholder.disabled"));
            addDrawableChild(disabledField);

            this.reducedField = new TextFieldWidget(
                    this.textRenderer, left, rowY(1), width, 20,
                    Text.translatable("particles.option.reduced"));
            reducedField.setMaxLength(4000);
            reducedField.setText(
                    String.join(",", OptimizatorConfig.particleReducedTypes));
            reducedField.setPlaceholder(
                    Text.translatable("particles.placeholder.reduced"));
            addDrawableChild(reducedField);

            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("particles.action.apply"),
                    button -> {
                        applyFields();
                        this.client.setScreen(this.parent);
                    }).dimensions(left, rowY(3), width, 20).build());

            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("particles.action.reset"),
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
                    }).dimensions(left, rowY(4), width, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("particles.navigation.back"),
                button -> this.client.setScreen(this.parent)
        ).dimensions(left, this.height - 27, width / 3 - 4, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("particles.navigation.page", page + 1, 2),
                button -> {
                    applyFieldsIfNeeded();
                    page = (page + 1) % 2;
                    clearAndInit();
                }
        ).dimensions(left + width / 3 + 2, this.height - 27,
                width / 3 - 4, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("particles.navigation.done"),
                button -> {
                    applyFieldsIfNeeded();
                    this.client.setScreen(this.parent);
                }
        ).dimensions(left + (width / 3) * 2 + 4, this.height - 27,
                width - (width / 3) * 2 - 4, 20).build());
    }

    private void clearHelpFields() {
        this.disabledField = null;
        this.reducedField = null;
    }

    private void applyFieldsIfNeeded() {
        if (page == 1) {
            applyFields();
        }
    }

    private void applyFields() {
        if (disabledField == null || reducedField == null) {
            return;
        }

        OptimizatorConfig.particleDisabledTypes.clear();
        OptimizatorConfig.particleReducedTypes.clear();

        parseField(disabledField.getText(), OptimizatorConfig.particleDisabledTypes);
        parseField(reducedField.getText(), OptimizatorConfig.particleReducedTypes);

        if (!OptimizatorConfig.particleDisabledTypes.isEmpty()
                || !OptimizatorConfig.particleReducedTypes.isEmpty()) {
            OptimizatorConfig.enabled = true;
        }

        OptimizatorConfig.save();
    }

    private static void parseField(
            String value, java.util.Set<String> target
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        for (String item : value.split(",")) {
            String normalized = item.trim().toLowerCase(java.util.Locale.ROOT);
            if (!normalized.isEmpty()) {
                target.add(normalized);
            }
        }
    }

    private int rowY(int row) {
        int navigationY = this.height - 27;
        int startY = Math.max(70, Math.min(94, navigationY - (5 * 20) - 12));
        return startY + row * 20;
    }

    private String qualityLabel() {
        return switch (OptimizatorConfig.particleQuality) {
            case 2 -> getString("particles.quality.minimal");
            case 1 -> getString("particles.quality.reduced");
            default -> getString("particles.quality.all");
        };
    }

    private String status(boolean value) {
        return getString(value ? "particles.status.on" : "particles.status.off");
    }

    private String getString(String key) {
        return Text.translatable(key).getString();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = Math.min(330, this.width - 10);
        int panelLeft = (this.width - panelWidth) / 2;
        int panelTop = 28;
        int panelBottom = this.height - 6;

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, 0xD0101010);
        context.fill(panelLeft + 1, panelTop + 1,
                panelLeft + panelWidth - 1, panelTop + 2, 0xFF3F3F3F);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                35,
                0xFFFFFFFF
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable(page == 0
                        ? "screen.particles.page.basic"
                        : "screen.particles.page.rules"),
                this.width / 2,
                48,
                0xFFAAAAAA
        );

        Text help = Text.translatable(page == 0
                ? "particles.help.basic"
                : "particles.help.rules");
        for (int i = 0; i < Math.min(2, this.textRenderer.wrapLines(
                help, panelWidth - 16).size()); i++) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    this.textRenderer.wrapLines(help, panelWidth - 16).get(i),
                    panelLeft + 8,
                    61 + i * 9,
                    0xFFE0E0E0
            );
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("particles.footer"),
                this.width / 2,
                panelBottom - 38,
                0xFF777777
        );

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        applyFieldsIfNeeded();
        this.client.setScreen(this.parent);
    }
}
