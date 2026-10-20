package dev.elbgg.capcams.client.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.data.Shot;
import dev.elbgg.capcams.network.CapPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ShotStore {
    private static final Gson GSON = new Gson();
    private static final Map<String, Shot> SHOTS = new TreeMap<>();
    private static final Set<String> DIRTY = new HashSet<>();

    private ShotStore() {}

    public static void applySync(String json) {
        Map<String, Shot> m = GSON.fromJson(json, new TypeToken<Map<String, Shot>>() {
        }.getType());
        SHOTS.clear();
        if (m != null) m.forEach((k, v) -> SHOTS.put(k.toLowerCase(Locale.ROOT), v));
        DIRTY.clear();
    }

    public static void clear() {
        SHOTS.clear();
        DIRTY.clear();
    }

    private static void send(String op, String name, String arg, String json) {
        if (ClientPlayNetworking.canSend(CapPayloads.Edit.ID)) ClientPlayNetworking.send(new CapPayloads.Edit(op, name, arg, json));
    }

    public static Shot get(String name) {
        return SHOTS.get(name.toLowerCase(Locale.ROOT));
    }

    public static void put(String name, Pose pose) {
        put(name, Shot.of(pose));
    }

    public static void put(String name, Shot shot) {
        String n = name.toLowerCase(Locale.ROOT);
        SHOTS.put(n, shot);
        DIRTY.remove(n);
        send("shot_put", n, "", GSON.toJson(shot));
    }

    public static void putTransient(String name, Shot shot) {
        String n = name.toLowerCase(Locale.ROOT);
        SHOTS.put(n, shot);
        DIRTY.add(n);
    }

    public static void flush() {
        for (String n : DIRTY) {
            Shot s = SHOTS.get(n);
            if (s != null) send("shot_put", n, "", GSON.toJson(s));
        }
        DIRTY.clear();
    }

    public static boolean rename(String from, String to) {
        String f = from.toLowerCase(Locale.ROOT), t = to.toLowerCase(Locale.ROOT);
        if (!SHOTS.containsKey(f) || SHOTS.containsKey(t)) return false;
        SHOTS.put(t, SHOTS.remove(f));
        send("shot_rename", f, t, "");
        return true;
    }

    public static String freeName(String base) {
        for (int i = 1; ; i++) {
            String n = base + i;
            if (!SHOTS.containsKey(n)) return n;
        }
    }

    public static boolean remove(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        boolean r = SHOTS.remove(n) != null;
        DIRTY.remove(n);
        if (r) send("shot_remove", n, "", "");
        return r;
    }

    public static Map<String, Shot> all() {
        return Collections.unmodifiableMap(SHOTS);
    }
}
