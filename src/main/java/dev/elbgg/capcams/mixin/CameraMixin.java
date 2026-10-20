package dev.elbgg.capcams.mixin;

import dev.elbgg.capcams.client.studio.EditorCamera;
import dev.elbgg.capcams.data.Pose;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("RETURN"))
    private void capcams$editorCamera(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!EditorCamera.active()) return;
        Pose p = EditorCamera.pose();
        setPos(p.x(), p.y(), p.z());
        setRotation(p.yaw(), p.pitch());
        this.thirdPerson = true;
    }
}
