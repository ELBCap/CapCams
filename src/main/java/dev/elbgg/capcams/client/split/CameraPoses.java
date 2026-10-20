package dev.elbgg.capcams.client.split;

import dev.elbgg.capcams.data.Pose;
import net.minecraft.client.render.Camera;

public final class CameraPoses {
    private CameraPoses() {}

    public static Pose of(Camera camera, float fov) {
        var p = camera.getPos();
        return new Pose(p.x, p.y, p.z, camera.getYaw(), camera.getPitch(), 0f, fov);
    }
}
