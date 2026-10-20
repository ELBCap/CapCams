package dev.elbgg.capcams.client.studio;

import dev.elbgg.capcams.data.Pose;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class EditorCamera {
    private static boolean active;
    private static double x, y, z;
    private static float yaw, pitch, fov = 70f;
    private static float speed = 6f;
    private static long lastNanos;

    private EditorCamera() {}

    public static boolean active() {
        return active;
    }

    public static void start(Pose from) {
        x = from.x();
        y = from.y();
        z = from.z();
        yaw = from.yaw();
        pitch = from.pitch();
        fov = from.fov();
        active = true;
        lastNanos = System.nanoTime();
    }

    public static void stop() {
        active = false;
    }

    public static Pose pose() {
        return new Pose(x, y, z, yaw, pitch, 0f, fov);
    }

    public static void setPose(Pose p) {
        x = p.x();
        y = p.y();
        z = p.z();
        yaw = p.yaw();
        pitch = p.pitch();
        fov = p.fov();
    }

    public static float speed() {
        return speed;
    }

    public static void scrollSpeed(double amount) {
        speed = MathHelper.clamp(speed * (float) Math.pow(1.2, amount), 0.5f, 80f);
    }

    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -89.9f, 89.9f);
    }

    public static Vec3d forward() {
        return forward(yaw, pitch);
    }

    public static Vec3d forward(float yawDeg, float pitchDeg) {
        double ry = Math.toRadians(yawDeg), rp = Math.toRadians(pitchDeg);
        return new Vec3d(-Math.sin(ry) * Math.cos(rp), -Math.sin(rp), Math.cos(ry) * Math.cos(rp));
    }

    public static Vec3d right(float yawDeg) {
        double ry = Math.toRadians(yawDeg);
        return new Vec3d(-Math.cos(ry), 0, -Math.sin(ry));
    }

    public static Vec3d up(float yawDeg, float pitchDeg) {
        return right(yawDeg).crossProduct(forward(yawDeg, pitchDeg)).normalize();
    }

    public static void update(boolean keyboardFree) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1.0e9f);
        lastNanos = now;
        if (!active || !keyboardFree) return;
        long w = MinecraftClient.getInstance().getWindow().getHandle();
        double fwd = axis(w, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_S);
        double side = axis(w, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_A);
        double vert = axis(w, GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT);
        if (fwd == 0 && side == 0 && vert == 0) return;
        float v = speed * dt * (InputUtil.isKeyPressed(w, GLFW.GLFW_KEY_LEFT_CONTROL) ? 3f : 1f);
        Vec3d move = forward().multiply(fwd).add(right(yaw).multiply(side)).add(0, vert, 0);
        x += move.x * v;
        y += move.y * v;
        z += move.z * v;
    }

    private static double axis(long w, int plus, int minus) {
        return (InputUtil.isKeyPressed(w, plus) ? 1 : 0) - (InputUtil.isKeyPressed(w, minus) ? 1 : 0);
    }
}
