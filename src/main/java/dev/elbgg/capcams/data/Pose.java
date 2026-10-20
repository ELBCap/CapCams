package dev.elbgg.capcams.data;

public record Pose(double x, double y, double z, float yaw, float pitch, float roll, float fov) {
    public static final float DEFAULT_FOV = 70f;

    public static Pose lookAt(double fx, double fy, double fz, double tx, double ty, double tz, float fov) {
        double dx = tx - fx, dy = ty - fy, dz = tz - fz;
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);
        return new Pose(fx, fy, fz, yaw, pitch, 0f, fov);
    }

    public String toText() {
        return String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f,%.2f,%.2f,%.2f,%.1f", x, y, z, yaw, pitch, roll, fov);
    }
}
