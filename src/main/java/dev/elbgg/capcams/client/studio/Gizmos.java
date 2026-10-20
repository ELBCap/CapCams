package dev.elbgg.capcams.client.studio;

import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.data.Shot;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public final class Gizmos {
    private static final float[][] PANEL_COLORS = {
            {0.35f, 0.85f, 1.0f}, {1.0f, 0.55f, 0.35f}, {0.55f, 1.0f, 0.45f}, {0.95f, 0.45f, 1.0f}};
    private static final float[] DEFAULT = {0.7f, 0.75f, 0.85f};
    private static final float[] SELECTED = {1.0f, 0.9f, 0.2f};
    private static final double PICK_RADIUS = 0.4;
    public static final double ARROW_LENGTH = 1.4;

    private static int dragAxis = -1;
    private static double dragOffset;
    private static Vec3d dragOrigin;

    private Gizmos() {}

    public static void render(WorldRenderContext ctx) {
        if (!Studio.isOpen() || VeilLevelPerspectiveRenderer.isRenderingPerspective()) return;
        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (ms == null || vcp == null) return;
        Vec3d cam = ctx.camera().getPos();
        VertexConsumer lines = vcp.getBuffer(RenderLayer.getLines());
        String selected = Studio.selected();

        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        MatrixStack.Entry e = ms.peek();
        for (String name : Studio.points()) {
            Shot shot = Studio.shot(name);
            if (shot == null) continue;
            boolean sel = name.equals(selected);
            float[] col = sel ? SELECTED : colorOf(name);
            boolean editingEnd = sel && Studio.editEnd && shot.end() != null;
            drawCamera(e, lines, shot.base(), col, sel && !editingEnd, 1f);
            if (shot.end() != null) {
                float[] ghost = editingEnd ? SELECTED : new float[]{col[0] * 0.8f, col[1] * 0.8f, col[2] * 0.8f};
                drawCamera(e, lines, shot.end(), ghost, editingEnd, editingEnd ? 1f : 0.55f);
                line(e, lines, new Vec3d(shot.x(), shot.y(), shot.z()), new Vec3d(shot.end().x(), shot.end().y(), shot.end().z()), col, 0.8f);
            }
            if (sel) drawArrows(e, lines, Studio.editPose(name));
        }
        ms.pop();

        for (String name : Studio.points()) {
            Pose p = Studio.pose(name);
            if (p == null) continue;
            String label = name + panelTag(name);
            DebugRenderer.drawString(ms, vcp, label, p.x(), p.y() + 0.45, p.z(),
                    name.equals(selected) ? 0xFFFFE033 : 0xFFFFFFFF, 0.025f, true, 0f, true);
        }
    }

    private static float[] colorOf(String name) {
        for (int i = 0; i < Studio.panels.length; i++) if (Studio.panels[i].equals(name)) return PANEL_COLORS[i];
        return DEFAULT;
    }

    private static String panelTag(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Studio.panels.length; i++) if (Studio.panels[i].equals(name)) sb.append(" [P").append(i + 1).append(']');
        return sb.toString();
    }

    private static void drawCamera(MatrixStack.Entry e, VertexConsumer vc, Pose p, float[] c, boolean sel, float a) {
        Vec3d pos = new Vec3d(p.x(), p.y(), p.z());
        Vec3d f = EditorCamera.forward(p.yaw(), p.pitch());
        Vec3d r = EditorCamera.right(p.yaw());
        Vec3d u = EditorCamera.up(p.yaw(), p.pitch());
        double d = sel ? 1.1 : 0.85;
        double hh = d * Math.tan(Math.toRadians(p.fov()) / 2.0), hw = hh * (16.0 / 9.0);
        Vec3d center = pos.add(f.multiply(d));
        Vec3d[] k = {
                center.add(r.multiply(hw)).add(u.multiply(hh)), center.add(r.multiply(-hw)).add(u.multiply(hh)),
                center.add(r.multiply(-hw)).add(u.multiply(-hh)), center.add(r.multiply(hw)).add(u.multiply(-hh))};
        for (int i = 0; i < 4; i++) {
            line(e, vc, pos, k[i], c, a);
            line(e, vc, k[i], k[(i + 1) % 4], c, a);
        }
        Vec3d topMid = center.add(u.multiply(hh));
        Vec3d tip = topMid.add(u.multiply(hh * 0.35));
        line(e, vc, k[0].lerp(k[1], 0.38), tip, c, a);
        line(e, vc, k[0].lerp(k[1], 0.62), tip, c, a);
        double s = 0.08;
        line(e, vc, pos.add(s, 0, 0), pos.add(-s, 0, 0), c, a);
        line(e, vc, pos.add(0, s, 0), pos.add(0, -s, 0), c, a);
        line(e, vc, pos.add(0, 0, s), pos.add(0, 0, -s), c, a);
    }

    private static final float[][] AXIS_COLOR = {{1f, 0.3f, 0.3f}, {0.35f, 1f, 0.35f}, {0.4f, 0.55f, 1f}};
    private static final Vec3d[] AXIS = {new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 1)};

    private static void drawArrows(MatrixStack.Entry e, VertexConsumer vc, Pose p) {
        Vec3d pos = new Vec3d(p.x(), p.y(), p.z());
        for (int a = 0; a < 3; a++) {
            float[] c = (a == dragAxis) ? new float[]{1f, 1f, 1f} : AXIS_COLOR[a];
            Vec3d end = pos.add(AXIS[a].multiply(ARROW_LENGTH));
            line(e, vc, pos, end, c, 1f);
            Vec3d side1 = AXIS[(a + 1) % 3].multiply(0.07), side2 = AXIS[(a + 2) % 3].multiply(0.07);
            Vec3d back = end.subtract(AXIS[a].multiply(0.2));
            line(e, vc, end, back.add(side1), c, 1f);
            line(e, vc, end, back.subtract(side1), c, 1f);
            line(e, vc, end, back.add(side2), c, 1f);
            line(e, vc, end, back.subtract(side2), c, 1f);
        }
    }

    private static void line(MatrixStack.Entry e, VertexConsumer vc, Vec3d a, Vec3d b, float[] c, float alpha) {
        Vec3d n = b.subtract(a);
        double len = n.length();
        if (len < 1e-6) return;
        n = n.multiply(1.0 / len);
        vc.vertex(e, (float) a.x, (float) a.y, (float) a.z).color(c[0], c[1], c[2], alpha).normal(e, (float) n.x, (float) n.y, (float) n.z);
        vc.vertex(e, (float) b.x, (float) b.y, (float) b.z).color(c[0], c[1], c[2], alpha).normal(e, (float) n.x, (float) n.y, (float) n.z);
    }

    public static Vec3d[] mouseRay(double mx, double my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        double w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        Pose c = EditorCamera.pose();
        double ndcX = 2.0 * mx / w - 1.0, ndcY = 1.0 - 2.0 * my / h;
        double t = Math.tan(Math.toRadians(c.fov()) / 2.0), aspect = w / h;
        Vec3d f = EditorCamera.forward(), r = EditorCamera.right(c.yaw()), u = EditorCamera.up(c.yaw(), c.pitch());
        Vec3d dir = f.add(r.multiply(ndcX * t * aspect)).add(u.multiply(ndcY * t)).normalize();
        return new Vec3d[]{new Vec3d(c.x(), c.y(), c.z()), dir};
    }

    public static String pickPoint(double mx, double my) {
        Vec3d[] ray = mouseRay(mx, my);
        String cur = Studio.selected();
        if (cur != null) {
            Shot sh = Studio.shot(cur);
            if (sh != null && sh.end() != null && hitSphere(ray, sh.end().x(), sh.end().y(), sh.end().z()) >= 0) {
                Studio.editEnd = true;
                return cur;
            }
        }
        String best = null;
        double bt = Double.MAX_VALUE;
        for (String name : Studio.points()) {
            Pose p = Studio.pose(name);
            if (p == null) continue;
            Vec3d c = new Vec3d(p.x(), p.y(), p.z());
            Vec3d oc = c.subtract(ray[0]);
            double tca = oc.dotProduct(ray[1]);
            if (tca < 0) continue;
            double d2 = oc.lengthSquared() - tca * tca;
            if (d2 > PICK_RADIUS * PICK_RADIUS) continue;
            if (tca < bt) {
                bt = tca;
                best = name;
            }
        }
        if (best != null) Studio.editEnd = false;
        return best;
    }

    private static double hitSphere(Vec3d[] ray, double x, double y, double z) {
        Vec3d oc = new Vec3d(x, y, z).subtract(ray[0]);
        double tca = oc.dotProduct(ray[1]);
        if (tca < 0) return -1;
        return (oc.lengthSquared() - tca * tca <= PICK_RADIUS * PICK_RADIUS) ? tca : -1;
    }

    public static boolean beginDrag(double mx, double my) {
        String sel = Studio.selected();
        if (sel == null) return false;
        Pose p = Studio.editPose(sel);
        if (p == null) return false;
        Vec3d pos = new Vec3d(p.x(), p.y(), p.z());
        Vec3d[] ray = mouseRay(mx, my);
        int best = -1;
        double bestDist = 0.14;
        for (int a = 0; a < 3; a++) {
            double[] cp = closestOnAxis(pos, AXIS[a], ray[0], ray[1]);
            if (cp[0] < 0 || cp[0] > ARROW_LENGTH + 0.1) continue;
            if (cp[1] < bestDist) {
                bestDist = cp[1];
                best = a;
            }
        }
        if (best < 0) return false;
        dragAxis = best;
        dragOrigin = pos;
        dragOffset = closestOnAxis(pos, AXIS[best], ray[0], ray[1])[0];
        return true;
    }

    public static boolean dragging() {
        return dragAxis >= 0;
    }

    public static void drag(double mx, double my) {
        String sel = Studio.selected();
        if (dragAxis < 0 || sel == null) return;
        Pose p = Studio.editPose(sel);
        if (p == null) return;
        Vec3d[] ray = mouseRay(mx, my);
        double t = closestOnAxis(dragOrigin, AXIS[dragAxis], ray[0], ray[1])[0];
        Vec3d np = dragOrigin.add(AXIS[dragAxis].multiply(t - dragOffset));
        Studio.setPose(sel, new Pose(np.x, np.y, np.z, p.yaw(), p.pitch(), p.roll(), p.fov()));
    }

    public static void endDrag() {
        if (dragAxis >= 0) Studio.commitPoints();
        dragAxis = -1;
    }

    private static double[] closestOnAxis(Vec3d p0, Vec3d d, Vec3d ro, Vec3d rd) {
        Vec3d w0 = p0.subtract(ro);
        double a = d.dotProduct(d), b = d.dotProduct(rd), c = rd.dotProduct(rd);
        double dd = d.dotProduct(w0), e = rd.dotProduct(w0);
        double den = a * c - b * b;
        if (Math.abs(den) < 1e-9) return new double[]{-1, 1e9};
        double s = (b * e - c * dd) / den;
        double t = (a * e - b * dd) / den;
        Vec3d onAxis = p0.add(d.multiply(s)), onRay = ro.add(rd.multiply(t));
        return new double[]{s, onAxis.distanceTo(onRay)};
    }
}
