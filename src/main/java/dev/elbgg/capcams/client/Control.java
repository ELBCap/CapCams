package dev.elbgg.capcams.client;

import dev.elbgg.capcams.client.split.PanelSource;
import dev.elbgg.capcams.client.split.Spec;
import dev.elbgg.capcams.client.split.SplitManager;
import dev.elbgg.capcams.client.store.SceneStore;
import dev.elbgg.capcams.data.SceneData;

public final class Control {
    private Control() {}

    public static String split(String layout, float seconds) {
        SceneData scene = SceneStore.get(layout);
        SplitManager m = SplitManager.INSTANCE;
        if (scene != null) {
            m.setSources(scene.panels());
            m.begin(Spec.parse(scene.spec()), seconds);
            return "escena " + layout;
        }
        Spec spec = Spec.parse(layout);
        m.begin(spec, seconds);
        return spec.toString();
    }

    public static void panel(int index1based, String source) {
        if (index1based < 1 || index1based > 4) throw new IllegalArgumentException("El panel va de 1 a 4.");
        SplitManager.INSTANCE.setSource(index1based - 1, PanelSource.parse(source));
    }

    public static void clear(float seconds) {
        SplitManager.INSTANCE.clear(seconds);
    }
}
