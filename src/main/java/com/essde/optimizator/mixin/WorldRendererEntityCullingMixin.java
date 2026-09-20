package com.essde.optimizator.mixin;

import com.essde.optimizator.OptimizatorRuntime;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import java.util.Iterator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererEntityCullingMixin {
    @Inject(method = "renderEntities", at = @At("HEAD"))
    private void optimizator$cullCheapFarEntities(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate immediate,
            Camera camera,
            RenderTickCounter tickCounter,
            List<Entity> entities,
            CallbackInfo callbackInfo
    ) {
        if (entities.isEmpty()) {
            return;
        }

        Vec3d cameraPosition = camera.getPos();
        Iterator<Entity> iterator = entities.iterator();

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (OptimizatorRuntime.shouldCullFarEntity(entity, cameraPosition)) {
                iterator.remove();
            }
        }
    }
}
