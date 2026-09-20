package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void optimizator$addButton(CallbackInfo callbackInfo) {
        GameMenuScreen screen = (GameMenuScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        int width = 200;
        int x = (client.getWindow().getScaledWidth() - width) / 2;
        int y = Math.min(client.getWindow().getScaledHeight() - 55,
                client.getWindow().getScaledHeight() / 4 + 220);

        screen.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Optimizator"),
                        button -> client.setScreen(new OptimizatorScreen(screen)))
                .dimensions(x, y, width, 20)
                .build());
    }
}
