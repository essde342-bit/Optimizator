package com.essde.optimizator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class OptimizatorScreen extends Screen {
    private final Screen parent;
    private int page = 0;
    private int contentWidth;

    private final List<HelpEntry> helpEntries = new ArrayList<>();

    public OptimizatorScreen(Screen parent) {
        super(Text.translatable("screen.optimizator.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.helpEntries.clear();

        int buttonWidth = Math.min(310, this.width - 18);
        int left = (this.width - buttonWidth) / 2;
        this.contentWidth = buttonWidth;

        if (page == 0) {
            addToggle(left, 0, "optimizator.option.master", "optimizator.help.master",
                    () -> OptimizatorConfig.enabled,
                    value -> OptimizatorConfig.enabled = value);

            addToggle(left, 1, "optimizator.option.adaptive", "optimizator.help.adaptive",
                    () -> OptimizatorConfig.adaptive,
                    value -> OptimizatorConfig.adaptive = value);

            addToggle(left, 2, "optimizator.option.entities", "optimizator.help.entities",
                    () -> OptimizatorConfig.deepEntityCulling,
                    value -> {
                        OptimizatorConfig.deepEntityCulling = value;
                        if (value) {
                            OptimizatorConfig.cullItemEntities = true;
                            OptimizatorConfig.cullExperienceOrbs = true;
                            OptimizatorConfig.enabled = true;
                        }
                    });

            addAction(left, 3, "optimizator.option.particles", "optimizator.help.particles",
                    button -> this.client.setScreen(new ParticleSettingsScreen(this)));

            addToggle(left, 4, "optimizator.option.chunks", "optimizator.help.chunks",
                    () -> OptimizatorConfig.chunkUploadBudget,
                    value -> OptimizatorConfig.chunkUploadBudget = value);

            addAction(left, 5, "optimizator.option.profiler", "optimizator.help.profiler",
                    button -> this.client.setScreen(new ProfilerScreen(this)));
        } else if (page == 1) {
            addToggle(left, 0, "optimizator.option.item_culling", "optimizator.help.item_culling",
                    () -> OptimizatorConfig.cullItemEntities,
                    value -> {
                        OptimizatorConfig.cullItemEntities = value;
                        if (value) OptimizatorConfig.enabled = true;
                    });

            addToggle(left, 1, "optimizator.option.xp_culling", "optimizator.help.xp_culling",
                    () -> OptimizatorConfig.cullExperienceOrbs,
                    value -> {
                        OptimizatorConfig.cullExperienceOrbs = value;
                        if (value) OptimizatorConfig.enabled = true;
                    });

            addSlider(left, 2, "optimizator.option.entity_distance", "optimizator.help.entity_distance",
                    OptimizatorConfig.farEntityCullDistance, 16, 128, "m",
                    value -> OptimizatorConfig.farEntityCullDistance = value);

            addToggle(left, 3, "optimizator.option.shadow_culling", "optimizator.help.shadow_culling",
                    () -> OptimizatorConfig.fastEntityShadows,
                    value -> {
                        OptimizatorConfig.fastEntityShadows = value;
                        if (value) OptimizatorConfig.enabled = true;
                    });

            addSlider(left, 4, "optimizator.option.shadow_distance", "optimizator.help.shadow_distance",
                    OptimizatorConfig.entityShadowDistance, 8, 256, "m",
                    value -> OptimizatorConfig.entityShadowDistance = value);

            addToggle(left, 5, "optimizator.option.clouds", "optimizator.help.clouds",
                    () -> OptimizatorConfig.disableCloudsUnderLoad,
                    value -> {
                        OptimizatorConfig.disableCloudsUnderLoad = value;
                        if (value) OptimizatorConfig.enabled = true;
                    });
        } else {
            addSlider(left, 0, "optimizator.option.adaptive_render_distance", "optimizator.help.adaptive_render_distance",
                    OptimizatorConfig.minRenderDistance, 4, 32, " chunks",
                    value -> OptimizatorConfig.minRenderDistance = value);

            addDoubleSlider(left, 1, "optimizator.option.adaptive_entity_distance",
                    "optimizator.help.adaptive_entity_distance",
                    OptimizatorConfig.minEntityDistance, 0.15D, 1.0D,
                    value -> OptimizatorConfig.minEntityDistance = value);

            addSlider(left, 2, "optimizator.option.adaptive_fps", "optimizator.help.adaptive_fps",
                    OptimizatorConfig.fpsFloor, 20, 120, " FPS",
                    value -> OptimizatorConfig.fpsFloor = value);

            addSlider(left, 3, "optimizator.option.chunk_uploads", "optimizator.help.chunk_uploads",
                    OptimizatorConfig.maxChunkUploadsPerFrame, 1, 32, "",
                    value -> OptimizatorConfig.maxChunkUploadsPerFrame = value);

            addSlider(left, 4, "optimizator.option.chunk_budget", "optimizator.help.chunk_budget",
                    OptimizatorConfig.chunkUploadBudgetMicros, 250, 10000, " µs",
                    value -> OptimizatorConfig.chunkUploadBudgetMicros = value);

            addToggle(left, 5, "optimizator.option.overlay", "optimizator.help.overlay",
                    () -> OptimizatorConfig.profilerOverlay,
                    value -> {
                        OptimizatorConfig.profilerOverlay = value;
                        if (value) {
                            OptimizatorConfig.profilerEnabled = true;
                            OptimizatorConfig.enabled = true;
                        }
                    });
        }

        addNavigation(left);
    }

    private void addToggle(
            int left,
            int row,
            String labelKey,
            String helpKey,
            BooleanSupplier getter,
            Consumer<Boolean> setter
    ) {
        ButtonWidget button = addDrawableChild(ButtonWidget.builder(
                        toggleText(labelKey, getter.getAsBoolean()),
                        widget -> {
                            boolean next = !getter.getAsBoolean();
                            setter.accept(next);
                            if (next && !labelKey.equals("optimizator.option.master")) {
                                OptimizatorConfig.enabled = true;
                            }
                            widget.setMessage(toggleText(labelKey, next));
                            OptimizatorConfig.save();
                        })
                .dimensions(left, rowY(row), contentWidth, 20)
                .build());
        addHelp(button, helpKey);
    }

    private void addAction(
            int left,
            int row,
            String labelKey,
            String helpKey,
            java.util.function.Consumer<ButtonWidget> action
    ) {
        ButtonWidget button = addDrawableChild(ButtonWidget.builder(
                        Text.translatable(labelKey),
                        action)
                .dimensions(left, rowY(row), contentWidth, 20)
                .build());
        addHelp(button, helpKey);
    }

    private void addSlider(
            int left,
            int row,
            String labelKey,
            String helpKey,
            int current,
            int min,
            int max,
            String suffix,
            Consumer<Integer> setter
    ) {
        SliderWidget slider = new SliderWidget(
                left, rowY(row), contentWidth, 20,
                sliderText(labelKey, current + suffix),
                (current - (double) min) / (max - min)
        ) {
            @Override
            protected void updateMessage() {
                int value = min + (int) Math.round(this.value * (max - min));
                setMessage(sliderText(labelKey, value + suffix));
            }

            @Override
            protected void applyValue() {
                int value = min + (int) Math.round(this.value * (max - min));
                setter.accept(value);
                OptimizatorConfig.save();
            }
        };
        addDrawableChild(slider);
        addHelp(slider, helpKey);
    }

    private void addDoubleSlider(
            int left,
            int row,
            String labelKey,
            String helpKey,
            double current,
            double min,
            double max,
            Consumer<Double> setter
    ) {
        SliderWidget slider = new SliderWidget(
                left, rowY(row), contentWidth, 20,
                sliderText(labelKey, String.format(Locale.ROOT, "%.2f", current)),
                (current - min) / (max - min)
        ) {
            @Override
            protected void updateMessage() {
                double value = min + this.value * (max - min);
                setMessage(sliderText(labelKey, String.format(Locale.ROOT, "%.2f", value)));
            }

            @Override
            protected void applyValue() {
                double value = min + this.value * (max - min);
                setter.accept(value);
                OptimizatorConfig.save();
            }
        };
        addDrawableChild(slider);
        addHelp(slider, helpKey);
    }

    private void addNavigation(int left) {
        int y = this.height - 27;
        int arrowWidth = 34;
        int pageWidth = 82;
        int gap = 5;
        int doneWidth = contentWidth - arrowWidth * 2 - pageWidth - gap * 3;

        addDrawableChild(ButtonWidget.builder(Text.literal("‹"), button -> {
                    page = (page + 2) % 3;
                    clearAndInit();
                }).dimensions(left, y, arrowWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("optimizator.navigation.page", page + 1, 3),
                button -> {
                    page = (page + 1) % 3;
                    clearAndInit();
                }).dimensions(left + arrowWidth + gap, y, pageWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("›"), button -> {
                    page = (page + 1) % 3;
                    clearAndInit();
                }).dimensions(left + arrowWidth + gap + pageWidth + gap, y, arrowWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("optimizator.navigation.done"), button -> {
                    OptimizatorConfig.save();
                    this.client.setScreen(this.parent);
                }).dimensions(
                left + arrowWidth + gap + pageWidth + gap + arrowWidth + gap,
                y,
                doneWidth,
                20
        ).build());
    }

    private int rowY(int row) {
        int navigationY = this.height - 27;
        int startY = Math.max(69, Math.min(94, navigationY - (6 * 20) - 8));
        return startY + row * 20;
    }

    private void addHelp(ClickableWidget widget, String helpKey) {
        this.helpEntries.add(new HelpEntry(widget, helpKey));
    }

    private Text toggleText(String key, boolean value) {
        return Text.translatable(key)
                .copy()
                .append(Text.literal(": "))
                .append(Text.translatable(value
                        ? "optimizator.status.on"
                        : "optimizator.status.off"));
    }

    private Text sliderText(String key, String value) {
        return Text.translatable(key)
                .copy()
                .append(Text.literal(": "))
                .append(Text.literal(value));
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
                Text.translatable("screen.optimizator.title"),
                this.width / 2,
                35,
                0xFFFFFFFF
        );

        Text pageTitle = Text.translatable(switch (page) {
            case 0 -> "screen.optimizator.page.main";
            case 1 -> "screen.optimizator.page.entities";
            default -> "screen.optimizator.page.performance";
        });
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                pageTitle,
                this.width / 2,
                48,
                0xFFAAAAAA
        );

        Text helpText = getCurrentHelp(mouseX, mouseY);
        int helpLeft = panelLeft + 8;
        int helpWidth = panelWidth - 16;
        int helpY = 61;
        List<net.minecraft.text.OrderedText> lines = this.textRenderer.wrapLines(helpText, helpWidth);
        int maxLines = 2;
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            context.drawTextWithShadow(
                    this.textRenderer,
                    lines.get(i),
                    helpLeft,
                    helpY + i * 9,
                    0xFFE0E0E0
            );
        }

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.optimizator.footer"),
                this.width / 2,
                panelBottom - 38,
                0xFF777777
        );

        super.render(context, mouseX, mouseY, delta);
    }

    private Text getCurrentHelp(int mouseX, int mouseY) {
        for (HelpEntry entry : helpEntries) {
            ClickableWidget widget = entry.widget();
            boolean over = mouseX >= widget.getX()
                    && mouseX < widget.getX() + widget.getWidth()
                    && mouseY >= widget.getY()
                    && mouseY < widget.getY() + widget.getHeight();
            if (over || widget.isFocused()) {
                return Text.translatable(entry.helpKey());
            }
        }

        return Text.translatable(switch (page) {
            case 0 -> "optimizator.help.page_main";
            case 1 -> "optimizator.help.page_entities";
            default -> "optimizator.help.page_performance";
        });
    }

    @Override
    public void close() {
        OptimizatorConfig.save();
        this.client.setScreen(this.parent);
    }

    private record HelpEntry(ClickableWidget widget, String helpKey) {
    }
}
