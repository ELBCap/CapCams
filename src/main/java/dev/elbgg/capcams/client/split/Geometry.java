package dev.elbgg.capcams.client.split;

import dev.elbgg.capcams.data.Easing;

import java.util.ArrayList;
import java.util.List;

public final class Geometry {
    public record Plane(float nx, float ny, float c) {
    }

    public static final class Built {
        public final List<List<Plane>> panels = new ArrayList<>();
        public float border = 0.004f;
        public float gap = 0.006f;
        public float[] borderColor = {1f, 1f, 1f, 1f};
        public float[] gapColor = {0f, 0f, 0f, 1f};
    }

    public static final int MAX_PANELS = 4;
    public static final int MAX_EDGES = 6;

    private Geometry() {}

    public static boolean known(String type) {
        return switch (type) {
            case "vertical", "horizontal", "diagonal", "diagonal_r", "triple", "triple_diagonal", "quad", "pip" -> true;
            default -> false;
        };
    }

    public static Built build(Spec spec, float t, float aspect) {
        Built b = new Built();
        float e = Easing.apply(spec.s("ease", "inout"), t);
        b.border = spec.f("border", spec.type().equals("pip") ? 0.006f : 0.004f);
        b.gap = spec.f("gap", spec.type().equals("pip") ? 0f : 0.006f);
        b.borderColor = spec.color("color", b.borderColor);
        b.gapColor = spec.color("bg", b.gapColor);
        float half = b.gap / 2f;

        switch (spec.type()) {
            case "vertical" -> twoPanels(b, spec, e, aspect, 0f, half);
            case "horizontal" -> twoPanels(b, spec, e, aspect, 90f, half);
            case "diagonal" -> twoPanels(b, spec, e, aspect, spec.f("angle", 12f), half);
            case "diagonal_r" -> twoPanels(b, spec, e, aspect, -spec.f("angle", 12f), half);
            case "triple" -> threePanels(b, spec, e, aspect, 0f, half);
            case "triple_diagonal" -> threePanels(b, spec, e, aspect, spec.f("angle", 12f), half);
            case "quad" -> quad(b, e, aspect, half);
            case "pip" -> pip(b, spec, e, aspect);
            default -> b.panels.add(List.of());
        }
        return b;
    }

    private static void twoPanels(Built b, Spec spec, float e, float aspect, float angleDeg, float half) {
        double a = Math.toRadians(angleDeg);
        float nx = (float) Math.cos(a), ny = (float) -Math.sin(a);
        float ext = aspect * Math.abs(nx) + Math.abs(ny);
        float pivot = spec.f("pivot", 0.5f);
        float finalOff = (pivot - 0.5f) * ext;
        float sign = spec.s("enter", "right").equals("left") ? -1f : 1f;
        float startOff = sign * (0.5f * ext + b.gap + 0.08f);
        float off = startOff + (finalOff - startOff) * e;
        float px = aspect / 2f + nx * off, py = 0.5f + ny * off;
        b.panels.add(List.of(half(nx, ny, px, py, -1f, half)));
        b.panels.add(List.of(half(nx, ny, px, py, +1f, half)));
    }

    private static void threePanels(Built b, Spec spec, float e, float aspect, float angleDeg, float half) {
        double a = Math.toRadians(angleDeg);
        float nx = (float) Math.cos(a), ny = (float) -Math.sin(a);
        float ext = aspect * Math.abs(nx) + Math.abs(ny);
        float w = ext / 6f * spec.f("spread", 1f);
        float far = 0.5f * ext + b.gap + 0.08f;
        float o1 = -far + (-w + far) * e;
        float o2 = far + (w - far) * e;
        float cx = aspect / 2f, cy = 0.5f;
        float p1x = cx + nx * o1, p1y = cy + ny * o1, p2x = cx + nx * o2, p2y = cy + ny * o2;
        b.panels.add(List.of(half(nx, ny, p1x, p1y, -1f, half)));
        b.panels.add(List.of(half(nx, ny, p1x, p1y, +1f, half), half(nx, ny, p2x, p2y, -1f, half)));
        b.panels.add(List.of(half(nx, ny, p2x, p2y, +1f, half)));
    }

    private static void quad(Built b, float e, float aspect, float half) {
        float farX = aspect / 2f + b.gap + 0.08f, farY = 0.5f + b.gap + 0.08f;
        float ox = farX * (1f - e), oy = -farY * (1f - e);
        float px = aspect / 2f + ox, py = 0.5f + oy;
        Plane left = half(1f, 0f, px, py, -1f, half), right = half(1f, 0f, px, py, +1f, half);
        Plane up = half(0f, 1f, px, py, +1f, half), down = half(0f, 1f, px, py, -1f, half);
        b.panels.add(List.of(left, up));
        b.panels.add(List.of(right, up));
        b.panels.add(List.of(left, down));
        b.panels.add(List.of(right, down));
    }

    private static void pip(Built b, Spec spec, float e, float aspect) {
        float h = spec.f("size", 0.42f), w = h * aspect;
        float margin = spec.f("margin", 0.03f);
        String corner = spec.s("corner", "br");
        boolean right = corner.endsWith("r"), top = corner.startsWith("t");
        float x1 = right ? aspect - margin * aspect : margin * aspect + w;
        float x0 = x1 - w;
        float y1 = top ? 1f - margin : margin + h;
        float y0 = y1 - h;
        float slide = (1f - e) * (w + margin * aspect + 0.1f) * (right ? 1f : -1f);
        x0 += slide;
        x1 += slide;
        b.panels.add(List.of());
        b.panels.add(List.of(new Plane(1f, 0f, -x0), new Plane(-1f, 0f, x1), new Plane(0f, 1f, -y0), new Plane(0f, -1f, y1)));
    }

    private static Plane half(float nx, float ny, float px, float py, float sign, float shift) {
        float sx = nx * sign, sy = ny * sign;
        return new Plane(sx, sy, -(sx * px + sy * py) - shift);
    }
}
