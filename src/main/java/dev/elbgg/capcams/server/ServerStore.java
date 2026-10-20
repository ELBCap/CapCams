package dev.elbgg.capcams.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.elbgg.capcams.CapCams;
import dev.elbgg.capcams.data.SceneData;
import dev.elbgg.capcams.data.Shot;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public final class ServerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Shot> SHOTS = new TreeMap<>();
    private static final Map<String, SceneData> SCENES = new TreeMap<>();
    private static boolean loaded;

    private ServerStore() {}

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("capcams");
    }

    private static Path shotsFile() {
        return dir().resolve("shots.json");
    }

    private static Path sceneFile(String name) {
        return dir().resolve("scenes").resolve(name + ".json");
    }

    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_\\-]{1,48}");
    }

    public static void reload() {
        loaded = false;
        load();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        SHOTS.clear();
        SCENES.clear();
        Path f = shotsFile();
        if (Files.isRegularFile(f)) {
            try {
                Map<String, Shot> m = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), new TypeToken<Map<String, Shot>>() {
                }.getType());
                if (m != null) m.forEach((k, v) -> SHOTS.put(k.toLowerCase(Locale.ROOT), v));
            } catch (Exception e) {
                CapCams.LOGGER.error("No se pudo leer {}: {}", f, e.toString());
            }
        }
        Path sd = dir().resolve("scenes");
        if (Files.isDirectory(sd)) {
            try (Stream<Path> s = Files.list(sd)) {
                s.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
                    String n = p.getFileName().toString();
                    n = n.substring(0, n.length() - 5).toLowerCase(Locale.ROOT);
                    try {
                        SceneData sc = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), SceneData.class);
                        if (sc != null && sc.spec() != null) SCENES.put(n, sc);
                    } catch (Exception e) {
                        CapCams.LOGGER.error("Escena invalida {}: {}", p, e.toString());
                    }
                });
            } catch (IOException e) {
                CapCams.LOGGER.error("No se pudo leer {}: {}", sd, e.toString());
            }
        }
        CapCams.LOGGER.info("CapCams servidor: {} planos y {} escenas", SHOTS.size(), SCENES.size());
    }

    private static void saveShots() throws IOException {
        Files.createDirectories(dir());
        Files.writeString(shotsFile(), GSON.toJson(SHOTS), StandardCharsets.UTF_8);
    }

    public static Map<String, Shot> shots() {
        load();
        return Collections.unmodifiableMap(SHOTS);
    }

    public static Map<String, SceneData> scenes() {
        load();
        return Collections.unmodifiableMap(SCENES);
    }

    public static Shot shot(String name) {
        load();
        return SHOTS.get(name.toLowerCase(Locale.ROOT));
    }

    public static SceneData scene(String name) {
        load();
        return SCENES.get(name.toLowerCase(Locale.ROOT));
    }

    public static void putShot(String name, Shot shot) throws IOException {
        load();
        SHOTS.put(name.toLowerCase(Locale.ROOT), shot);
        saveShots();
    }

    public static boolean removeShot(String name) throws IOException {
        load();
        boolean r = SHOTS.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (r) saveShots();
        return r;
    }

    public static boolean renameShot(String from, String to) throws IOException {
        load();
        String f = from.toLowerCase(Locale.ROOT), t = to.toLowerCase(Locale.ROOT);
        if (!SHOTS.containsKey(f) || SHOTS.containsKey(t)) return false;
        SHOTS.put(t, SHOTS.remove(f));
        saveShots();
        return true;
    }

    public static void putScene(String name, SceneData scene) throws IOException {
        load();
        String n = name.toLowerCase(Locale.ROOT);
        SCENES.put(n, scene);
        Files.createDirectories(dir().resolve("scenes"));
        Files.writeString(sceneFile(n), GSON.toJson(scene), StandardCharsets.UTF_8);
    }

    public static boolean removeScene(String name) throws IOException {
        load();
        String n = name.toLowerCase(Locale.ROOT);
        boolean r = SCENES.remove(n) != null;
        Files.deleteIfExists(sceneFile(n));
        return r;
    }

    public static String shotsJson() {
        load();
        return GSON.toJson(SHOTS);
    }

    public static String scenesJson() {
        load();
        return GSON.toJson(SCENES);
    }

    public static Shot parseShot(String json) {
        return GSON.fromJson(json, Shot.class);
    }

    public static SceneData parseScene(String json) {
        return GSON.fromJson(json, SceneData.class);
    }
}
