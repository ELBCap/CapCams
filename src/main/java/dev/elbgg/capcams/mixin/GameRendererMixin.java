package dev.elbgg.capcams.mixin;

import dev.elbgg.capcams.client.split.SplitManager;
import dev.elbgg.capcams.client.studio.EditorCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void capcams$hideHand(Camera camera, float tickDelta, Matrix4f matrix, CallbackInfo ci) {
        if (SplitManager.INSTANCE.active() || EditorCamera.active()) ci.cancel();
    }
}
