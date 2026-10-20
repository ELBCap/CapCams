package dev.elbgg.capcams.client;

import dev.elbgg.capcams.CapCams;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class ClientConfig {
    private static final String FILE = "capcams-client.toml";

    private static String modpackMode = "auto";
    private static boolean blockCommandsInModpack = true;
    private static boolean blockStudioInModpack = true;
    private static boolean acceptServerSplits = true;

    private static Boolean modpack;
    private static String modpackReason = "";

    private ClientConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    public static void load() {
        Path f = file();
        if (!Files.isRegularFile(f)) {
            write(f);
        } else {
            try {
                parse(Files.readAllLines(f, StandardCharsets.UTF_8));
            } catch (IOException e) {
                CapCams.LOGGER.warn("No se pudo leer {} ({}): se usan los valores por defecto", f, e.toString());
            }
        }
        modpack = null;
        CapCams.LOGGER.info("CapCams cliente: modpack={} ({}); comandos locales {}; Studio {}; ordenes del servidor {}",
                isModpack(), modpackReason, commandsAllowed() ? "activos" : "BLOQUEADOS", studioAllowed() ? "activo" : "BLOQUEADO",
                acceptServerSplits ? "aceptadas" : "ignoradas");
    }

    public static boolean commandsAllowed() {
        return !(isModpack() && blockCommandsInModpack);
    }

    public static boolean studioAllowed() {
        return !(isModpack() && blockStudioInModpack);
    }

    public static boolean acceptServerSplits() {
        return acceptServerSplits;
    }

    public static boolean isModpack() {
        if (modpack == null) modpack = detect();
        return modpack;
    }

    private static boolean detect() {
        switch (modpackMode.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on" -> {
                modpackReason = "forzado en la config";
                return true;
            }
            case "false", "no", "off" -> {
                modpackReason = "forzado en la config";
                return false;
            }
            default -> {
            }
        }
        if (Boolean.getBoolean("capcams.modpack")) {
            modpackReason = "propiedad -Dcapcams.modpack=true";
            return true;
        }
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            modpackReason = "entorno de desarrollo";
            return false;
        }
        Path game = FabricLoader.getInstance().getGameDir();
        Path parent = game.toAbsolutePath().normalize().getParent();

        List<String> markers = List.of(
                "manifest.json",
                "minecraftinstance.json",
                "modrinth.index.json",
                "instance.cfg", "mmc-pack.json",
                "pack.toml", "index.toml",
                "instance.json",
                "modpack.json", "profile.json");
        for (Path dir : new Path[]{game, parent}) {
            if (dir == null) continue;
            for (String m : markers) {
                if (Files.exists(dir.resolve(m))) {
                    modpackReason = "encontrado " + m + " en " + dir.getFileName();
                    return true;
                }
            }
        }
        if (Files.isDirectory(game.resolve("bin")) && Files.exists(game.resolve("bin").resolve("modpack.jar"))) {
            modpackReason = "Technic (bin/modpack.jar)";
            return true;
        }
        modpackReason = "sin marcas de modpack";
        return false;
    }

    private static void parse(List<String> lines) {
        for (String raw : lines) {
            String line = stripComment(raw).trim();
            if (line.isEmpty() || line.startsWith("[")) continue;
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String val = line.substring(eq + 1).trim();
            if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
            switch (key) {
                case "modpack" -> modpackMode = val;
                case "block_commands_in_modpack" -> blockCommandsInModpack = bool(val, true);
                case "block_studio_in_modpack" -> blockStudioInModpack = bool(val, true);
                case "accept_server_splits" -> acceptServerSplits = bool(val, true);
                default -> CapCams.LOGGER.warn("{}: clave desconocida '{}'", FILE, key);
            }
        }
    }

    private static boolean bool(String v, boolean def) {
        if (v.equalsIgnoreCase("true")) return true;
        if (v.equalsIgnoreCase("false")) return false;
        CapCams.LOGGER.warn("{}: '{}' no es true/false, se usa {}", FILE, v, def);
        return def;
    }

    private static String stripComment(String s) {
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (c == '#' && !quoted) return s.substring(0, i);
        }
        return s;
    }

    private static void write(Path f) {
        String text = """
                # CapCams - configuracion del cliente
                #
                # Las herramientas de autoria locales (los comandos /capcams y el Studio) estan pensadas para quien crea
                # contenido. Dentro de un modpack se bloquean: no se registran, asi que tampoco salen en el autocompletado.
                # Lo que ordena el SERVIDOR (etiquetas de Ink de NarrativeCraft y /capsplit) sigue funcionando.

                # Como saber si esto es un modpack:
                #   "auto"  detecta las marcas de CurseForge, Modrinth, Prism/MultiMC, ATLauncher, FTB, packwiz o Technic
                #           (en el entorno de desarrollo nunca se considera modpack)
                #   "true"  se considera siempre un modpack
                #   "false" nunca es un modpack
                modpack = "auto"

                # Si es un modpack, bloquear todos los comandos /capcams del cliente.
                block_commands_in_modpack = true

                # Si es un modpack, bloquear tambien el Studio (tecla K y editor).
                block_studio_in_modpack = true

                # Aceptar las divisiones de pantalla que ordene el servidor. Pon false para ignorarlas.
                accept_server_splits = true
                """;
        try {
            Files.createDirectories(f.getParent());
            Files.writeString(f, text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CapCams.LOGGER.warn("No se pudo crear {}: {}", f, e.toString());
        }
    }
}
