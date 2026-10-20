package dev.elbgg.capcams.server;

import dev.elbgg.capcams.client.split.Geometry;
import dev.elbgg.capcams.client.split.Spec;

import java.util.Locale;

public final class Checks {
    private Checks() {}

    public static String layout(String layout) {
        if (layout == null || layout.isBlank()) return "layout vacio";
        if (ServerStore.scene(layout.trim()) != null) return null;
        String type = Spec.parse(layout).type();
        if (Geometry.known(type)) return null;
        return "'" + layout + "' no es una escena guardada ni un preset conocido (vertical, horizontal, diagonal, diagonal_r, triple, triple_diagonal, quad, pip)";
    }

    public static String source(String source) {
        if (source == null || source.isBlank()) return "fuente vacia";
        String s = source.trim();
        if (s.equalsIgnoreCase("main")) return null;
        int c = s.indexOf(':');
        if (c < 0) return "fuente desconocida: " + source;
        String kind = s.substring(0, c).toLowerCase(Locale.ROOT);
        switch (kind) {
            case "shot" -> {
                return ServerStore.shot(s.substring(c + 1)) == null ? "no existe el plano '" + s.substring(c + 1) + "' en el servidor" : null;
            }
            case "pos", "look", "entity" -> {
                return null;
            }
            default -> {
                return "tipo de camara desconocido: " + kind;
            }
        }
    }
}
