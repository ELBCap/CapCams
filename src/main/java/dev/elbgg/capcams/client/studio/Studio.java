package dev.elbgg.capcams.client.studio;

import dev.elbgg.capcams.client.split.CameraPoses;
import dev.elbgg.capcams.client.split.PanelSource;
import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.data.Shot;
import dev.elbgg.capcams.client.split.Spec;
import dev.elbgg.capcams.client.split.SplitManager;
import dev.elbgg.capcams.client.store.SceneStore;
import dev.elbgg.capcams.data.SceneData;
import dev.elbgg.capcams.client.store.ShotStore;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Studio {
    public static final String[] PRESETS = {"vertical", "horizontal", "diagonal", "diagonal_r", "triple", "triple_diagonal", "quad", "pip"};
    public static final String[] EASES = {"inout", "out", "in", "linear", "sine", "back"};
    public static final String[] ENTER = {"right", "left"};

    private static boolean open;
    private static String selected;
    private static boolean prevHud;
    private static boolean imguiWantsKeyboard;

    public static int preset = 2;
    public static float angle = 12f, gap = 0.006f, border = 0.004f, pivot = 0.5f, size = 0.42f, seconds = 1.0f;
    public static float[] color = {1f, 1f, 1f, 1f};
    public static int ease = 0, enter = 0;
    public static final String[] panels = {"main", "main", "main", "main"};
    public static boolean live = true;
    public static boolean editEnd;
    public static boolean loopPreview = true;
    public static String sceneName = "";
    public static String status = "";

    private Studio() {}

    public static boolean isOpen() {
        return open;
    }

    public static void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (open || mc.world == null || mc.player == null || !dev.elbgg.capcams.client.ClientConfig.studioAllowed()) return;
        open = true;
        EditorCamera.start(CameraPoses.of(mc.gameRenderer.getCamera(), mc.options.getFov().getValue().floatValue()));
        prevHud = mc.options.hudHidden;
        mc.options.hudHidden = true;
        mc.setScreen(new StudioScreen());
        applyLive();
    }

    static void closed() {
        if (!open) return;
        open = false;
        EditorCamera.stop();
        SplitManager.INSTANCE.setLoopSeconds(0f);
        ShotStore.flush();
        MinecraftClient.getInstance().options.hudHidden = prevHud;
        SplitManager.INSTANCE.clearNow();
    }

    public static void close() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof StudioScreen) mc.setScreen(null);
        else closed();
    }

    public static boolean imguiWantsKeyboard() {
        return imguiWantsKeyboard;
    }

    public static void setImguiWantsKeyboard(boolean v) {
        imguiWantsKeyboard = v;
    }

    public static List<String> points() {
        return new ArrayList<>(ShotStore.all().keySet());
    }

    public static String selected() {
        if (selected != null && ShotStore.get(selected) == null) selected = null;
        return selected;
    }

    public static void select(String name) {
        selected = name;
    }

    public static Shot shot(String name) {
        return ShotStore.get(name);
    }

    public static Pose pose(String name) {
        Shot s = ShotStore.get(name);
        return s == null ? null : s.base();
    }

    public static Pose editPose(String name) {
        Shot s = ShotStore.get(name);
        if (s == null) return null;
        return (editEnd && s.end() != null) ? s.end() : s.base();
    }

    public static void setShot(String name, Shot shot) {
        ShotStore.putTransient(name, shot);
    }

    public static float loopLength() {
        float len = 0f;
        for (String n : points()) {
            Shot s = ShotStore.get(n);
            if (s != null && s.moves()) len = Math.max(len, s.cycle());
        }
        return len > 0f ? len + 1.0f : 0f;
    }

    public static String addAtEditorCamera() {
        String name = ShotStore.freeName("cam");
        ShotStore.put(name, EditorCamera.pose());
        selected = name;
        return name;
    }

    public static void duplicate(String name) {
        Shot p = ShotStore.get(name);
        if (p == null) return;
        String n = ShotStore.freeName(name.replaceAll("\\d+$", ""));
        ShotStore.put(n, p);
        selected = n;
    }

    public static void delete(String name) {
        ShotStore.remove(name);
        for (int i = 0; i < panels.length; i++) if (panels[i].equals(name)) panels[i] = "main";
        if (name.equals(selected)) selected = null;
        applyLive();
    }

    public static boolean rename(String from, String to) {
        String t = to.trim().toLowerCase(Locale.ROOT);
        if (!SceneStore.validName(t) || !ShotStore.rename(from, t)) return false;
        for (int i = 0; i < panels.length; i++) if (panels[i].equals(from)) panels[i] = t;
        if (from.equals(selected)) selected = t;
        applyLive();
        return true;
    }

    public static void setPose(String name, Pose p) {
        Shot s = ShotStore.get(name);
        if (s == null) return;
        ShotStore.putTransient(name, (editEnd && s.end() != null) ? s.withEnd(p) : s.withBase(p));
    }

    public static void commitPoints() {
        ShotStore.flush();
    }

    public static Spec spec() {
        String type = PRESETS[preset];
        StringBuilder sb = new StringBuilder(type).append(':');
        sb.append("gap=").append(gap).append(",border=").append(border).append(",pivot=").append(pivot);
        if (type.startsWith("diagonal") || type.equals("triple_diagonal")) sb.append(",angle=").append(angle);
        if (type.equals("pip")) sb.append(",size=").append(size);
        sb.append(",ease=").append(EASES[ease]).append(",enter=").append(ENTER[enter]);
        sb.append(",color=").append(hex(color));
        return Spec.parse(sb.toString());
    }

    private static String hex(float[] c) {
        return String.format("%02x%02x%02x%02x", Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255), Math.round(c[3] * 255));
    }

    private static void pushSources() {
        for (int i = 0; i < panels.length; i++) {
            String n = panels[i];
            SplitManager.INSTANCE.setSource(i, n.equals("main") ? new PanelSource.Main() : new PanelSource.ShotRef(n));
        }
    }

    public static void applyLive() {
        if (!open) return;
        if (!live) {
            SplitManager.INSTANCE.clearNow();
            return;
        }
        pushSources();
        SplitManager.INSTANCE.begin(spec(), 0f);
    }

    public static void playTransition() {
        pushSources();
        SplitManager.INSTANCE.clearNow();
        SplitManager.INSTANCE.begin(spec(), seconds);
        live = true;
    }

    public static boolean saveScene(String name) {
        if (!SceneStore.validName(name)) {
            status = "Nombre no valido (a-z, 0-9, _ y -).";
            return false;
        }
        List<String> srcs = new ArrayList<>();
        for (String p : panels) srcs.add(p.equals("main") ? "main" : "shot:" + p);
        SceneStore.put(name, new SceneData(spec().toString(), srcs));
        ShotStore.flush();
        status = "Escena '" + name + "' guardada. En Ink:  # capcams_split " + name;
        return true;
    }

    public static boolean loadScene(String name) {
        SceneData sc = SceneStore.get(name);
        if (sc == null) {
            status = "No existe la escena '" + name + "'.";
            return false;
        }
        Spec spec = Spec.parse(sc.spec());
        for (int i = 0; i < PRESETS.length; i++) if (PRESETS[i].equals(spec.type())) preset = i;
        angle = spec.f("angle", 12f);
        gap = spec.f("gap", 0.006f);
        border = spec.f("border", 0.004f);
        pivot = spec.f("pivot", 0.5f);
        size = spec.f("size", 0.42f);
        color = spec.color("color", new float[]{1f, 1f, 1f, 1f}).clone();
        for (int i = 0; i < EASES.length; i++) if (EASES[i].equals(spec.s("ease", "inout"))) ease = i;
        for (int i = 0; i < ENTER.length; i++) if (ENTER[i].equals(spec.s("enter", "right"))) enter = i;
        for (int i = 0; i < panels.length; i++) {
            String src = i < sc.panels().size() ? sc.panels().get(i) : "main";
            panels[i] = src.startsWith("shot:") ? src.substring(5) : "main";
        }
        sceneName = name;
        status = "Escena '" + name + "' cargada.";
        applyLive();
        return true;
    }
}
