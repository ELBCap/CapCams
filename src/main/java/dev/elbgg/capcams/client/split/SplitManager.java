package dev.elbgg.capcams.client.split;

import dev.elbgg.capcams.data.Pose;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public final class SplitManager {
    public static final SplitManager INSTANCE = new SplitManager();

    public static final class Frame {
        public Geometry.Built shape;
        public final int[] texture = new int[Geometry.MAX_PANELS];
        public final Pose[] pose = new Pose[Geometry.MAX_PANELS + 1];
        public int panelCount;
    }

    private Spec spec;
    private final PanelSource[] sources = {new PanelSource.Main(), new PanelSource.Main(), new PanelSource.Main(), new PanelSource.Main()};
    private boolean active;
    private long startNanos;
    private float seconds = 1f;
    private int direction = 1;
    private Frame last;
    private long clockNanos = System.nanoTime();
    private float loopSeconds;

    private SplitManager() {}

    public boolean active() {
        return active;
    }

    public Spec spec() {
        return spec;
    }

    public Frame lastFrame() {
        return last;
    }

    public PanelSource source(int panel) {
        return sources[panel];
    }

    public List<String> sourceTexts() {
        List<String> out = new ArrayList<>();
        for (PanelSource s : sources) out.add(s.text());
        return out;
    }

    public void begin(Spec newSpec, float seconds) {
        if (!Geometry.known(newSpec.type())) throw new IllegalArgumentException("Division desconocida: " + newSpec.type());
        boolean changed = spec == null || !spec.type().equals(newSpec.type());
        this.spec = newSpec;
        this.seconds = Math.max(0f, seconds);
        this.direction = 1;
        if (!active || changed) clockNanos = System.nanoTime();
        this.startNanos = (active && !changed) ? System.nanoTime() - (long) (this.seconds * 1e9f) : System.nanoTime();
        this.active = true;
    }

    public void clear(float seconds) {
        if (!active) return;
        this.seconds = Math.max(0f, seconds);
        this.direction = -1;
        this.startNanos = System.nanoTime();
    }

    public float elapsed() {
        float t = (System.nanoTime() - clockNanos) / 1.0e9f;
        return loopSeconds > 0f ? t % loopSeconds : t;
    }

    public void restartClock() {
        clockNanos = System.nanoTime();
    }

    public void setLoopSeconds(float seconds) {
        this.loopSeconds = seconds;
    }

    public void clearNow() {
        active = false;
        last = null;
    }

    public void setSource(int panel, PanelSource source) {
        sources[panel] = source;
    }

    public void setSources(List<String> texts) {
        for (int i = 0; i < sources.length; i++) {
            sources[i] = i < texts.size() ? PanelSource.parse(texts.get(i)) : new PanelSource.Main();
        }
    }

    private float progress() {
        if (seconds <= 0f) return direction > 0 ? 1f : 0f;
        float p = (System.nanoTime() - startNanos) / (seconds * 1.0e9f);
        p = Math.max(0f, Math.min(1f, p));
        return direction > 0 ? p : 1f - p;
    }

    public Frame frame(MinecraftClient mc, float partial, float aspect) {
        if (!active || spec == null) {
            last = null;
            return null;
        }
        float t = progress();
        if (direction < 0 && t <= 0f && seconds >= 0f && System.nanoTime() - startNanos >= (long) (seconds * 1e9f)) {
            clearNow();
            return null;
        }
        Frame f = new Frame();
        f.shape = Geometry.build(spec, t, aspect);
        f.panelCount = Math.min(f.shape.panels.size(), Geometry.MAX_PANELS);
        int next = 1;
        for (int i = 0; i < f.panelCount; i++) {
            PanelSource src = sources[i];
            Pose pose = (src instanceof PanelSource.Main) ? null : src.resolve(mc, partial);
            if (pose == null || next > Geometry.MAX_PANELS) {
                f.texture[i] = 0;
            } else {
                f.texture[i] = next;
                f.pose[next] = pose;
                next++;
            }
        }
        last = f;
        return f;
    }
}
