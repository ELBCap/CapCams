package dev.elbgg.capcams.data;

public final class Easing {
    private Easing() {}

    public static float apply(String name, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return switch (name) {
            case "linear" -> t;
            case "out" -> 1f - (float) Math.pow(1f - t, 3);
            case "in" -> t * t * t;
            case "sine" -> (float) (-(Math.cos(Math.PI * t) - 1.0) / 2.0);
            case "back" -> {
                float c1 = 1.70158f, c3 = c1 + 1f;
                float u = t - 1f;
                yield 1f + c3 * u * u * u + c1 * u * u;
            }
            default -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        };
    }
}
