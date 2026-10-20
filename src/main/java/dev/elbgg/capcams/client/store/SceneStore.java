package dev.elbgg.capcams.client.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.elbgg.capcams.data.SceneData;
import dev.elbgg.capcams.network.CapPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class SceneStore {
    private static final Gson GSON = new Gson();
    private static final Map<String, SceneData> SCENES = new TreeMap<>();

    private SceneStore() {}

    public static void applySync(String json) {
        Map<String, SceneData> m = GSON.fromJson(json, new TypeToken<Map<String, SceneData>>() {
        }.getType());
        SCENES.clear();
        if (m != null) m.forEach((k, v) -> SCENES.put(k.toLowerCase(Locale.ROOT), v));
    }

    public static void clear() {
        SCENES.clear();
    }

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_\\-]{1,48}");
    }

    public static SceneData get(String name) {
        return validName(name) ? SCENES.get(name.toLowerCase(Locale.ROOT)) : null;
    }

    public static void put(String name, SceneData scene) {
        String n = name.toLowerCase(Locale.ROOT);
        SCENES.put(n, scene);
        if (ClientPlayNetworking.canSend(CapPayloads.Edit.ID)) {
            ClientPlayNetworking.send(new CapPayloads.Edit("scene_put", n, "", GSON.toJson(scene)));
        }
    }

    public static boolean remove(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        boolean r = SCENES.remove(n) != null;
        if (r && ClientPlayNetworking.canSend(CapPayloads.Edit.ID)) {
            ClientPlayNetworking.send(new CapPayloads.Edit("scene_remove", n, "", ""));
        }
        return r;
    }

    public static List<String> names() {
        return new ArrayList<>(SCENES.keySet());
    }
}
