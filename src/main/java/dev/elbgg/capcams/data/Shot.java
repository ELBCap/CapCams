package dev.elbgg.capcams.data;

import net.minecraft.util.math.MathHelper;

import java.util.Locale;

public record Shot(double x, double y, double z, float yaw, float pitch, float roll, float fov,
                   Pose end, float seconds, String ease, String mode, float delay, float shake, float shakeHz) {
    public static final String[] MODES = {"once", "pingpong", "loop"};

    public static Shot of(Pose p) {
        return new Shot(p.x(), p.y(), p.z(), p.yaw(), p.pitch(), p.roll(), p.fov(), null, 2f, "inout", "once", 0f, 0f, 1.5f);
    }

    public Pose base() {
        return new Pose(x, y, z, yaw, pitch, roll, fov);
    }

    public Shot withBase(Pose p) {
        return new Shot(p.x(), p.y(), p.z(), p.yaw(), p.pitch(), p.roll(), p.fov(), end, seconds, ease, mode, delay, shake, shakeHz);
    }

    public Shot withEnd(Pose e) {
        return new Shot(x, y, z, yaw, pitch, roll, fov, e, seconds, ease, mode, delay, shake, shakeHz);
    }

    public Shot with(float seconds, String ease, String mode, float delay, float shake, float shakeHz) {
        return new Shot(x, y, z, yaw, pitch, roll, fov, end, seconds, ease, mode, delay, shake, shakeHz);
    }

    public boolean moves() {
        return end != null && seconds > 0f;
    }

    public float cycle() {
        return delay + Math.max(0f, seconds) * ("pingpong".equals(mode) ? 2f : 1f);
    }

    public Pose at(float t) {
        Pose p = base();
        if (moves()) {
            float cyc = (t - delay) / seconds;
            float u;
            switch (mode == null ? "once" : mode.toLowerCase(Locale.ROOT)) {
                case "pingpong" -> {
                    float tri = cyc - 2f * (float) Math.floor(cyc / 2f);
                    u = cyc < 0 ? 0f : (tri <= 1f ? tri : 2f - tri);
                }
                case "loop" -> u = cyc < 0 ? 0f : cyc - (float) Math.floor(cyc);
                default -> u = MathHelper.clamp(cyc, 0f, 1f);
            }
            float e = Easing.apply(ease == null ? "inout" : ease, u);
            p = new Pose(
                    p.x() + (end.x() - p.x()) * e, p.y() + (end.y() - p.y()) * e, p.z() + (end.z() - p.z()) * e,
                    p.yaw() + MathHelper.wrapDegrees(end.yaw() - p.yaw()) * e,
                    p.pitch() + (end.pitch() - p.pitch()) * e,
                    p.roll() + (end.roll() - p.roll()) * e,
                    p.fov() + (end.fov() - p.fov()) * e);
        }
        if (shake > 0f) {
            double w = (shakeHz <= 0 ? 1.5 : shakeHz) * 2.0 * Math.PI * t;
            float dy = (float) (Math.sin(w) + 0.5 * Math.sin(w * 2.7 + 1.3)) * shake / 1.5f;
            float dp = (float) (Math.sin(w * 1.3 + 0.7) + 0.5 * Math.sin(w * 3.1 + 2.1)) * shake / 1.5f;
            float dr = (float) (Math.sin(w * 0.8 + 2.4)) * shake * 0.4f;
            p = new Pose(p.x(), p.y(), p.z(), p.yaw() + dy, MathHelper.clamp(p.pitch() + dp, -89.9f, 89.9f), p.roll() + dr, p.fov());
        }
        return p;
    }
}
