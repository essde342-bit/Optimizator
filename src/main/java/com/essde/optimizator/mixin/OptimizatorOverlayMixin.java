package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorConfig;
import com.essde.optimizator.PerformanceProfiler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class OptimizatorOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void optimizator$renderOverlay(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo callbackInfo
    ) {
        if (!OptimizatorConfig.profilerEnabled || !OptimizatorConfig.profilerOverlay) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int x = 6;
        int y = 6;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("Optimizator"),
                x,
                y,
                0xFFFFFFFF
        );
        y += 11;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("FPS: " + client.getCurrentFps()
                        + "  Frame: " + format(PerformanceProfiler.frameMs()) + " ms"),
                x,
                y,
                0xFFE0E0E0
        );
        y += 11;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("World: " + format(PerformanceProfiler.worldMs())
                        + "  Entities: " + format(PerformanceProfiler.entitiesMs())
                        + "  Particles: " + format(PerformanceProfiler.particlesMs())),
                x,
                y,
                0xFFE0E0E0
        );
        y += 11;

        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal("Top: " + PerformanceProfiler.bottleneckName(client)),
                x,
                y,
                0xFFE0E0E0
        );
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
