package dev.elbgg.capcams.client.split;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record Spec(String type, Map<String, String> params) {
    public static Spec parse(String text) {
        String t = text.trim();
        int c = t.indexOf(':');
        String type = (c < 0 ? t : t.substring(0, c)).toLowerCase(Locale.ROOT);
        Map<String, String> p = new LinkedHashMap<>();
        if (c >= 0) {
            for (String kv : t.substring(c + 1).split(",")) {
                int e = kv.indexOf('=');
                if (e > 0) p.put(kv.substring(0, e).trim().toLowerCase(Locale.ROOT), kv.substring(e + 1).trim());
            }
        }
        return new Spec(type, p);
    }

    public float f(String key, float def) {
        String v = params.get(key);
        if (v == null) return def;
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public String s(String key, String def) {
        return params.getOrDefault(key, def);
    }

    public float[] color(String key, float[] def) {
        String v = params.get(key);
        if (v == null) return def;
        v = v.startsWith("#") ? v.substring(1) : v;
        try {
            long n = Long.parseLong(v, 16);
            if (v.length() == 6) return new float[]{((n >> 16) & 255) / 255f, ((n >> 8) & 255) / 255f, (n & 255) / 255f, 1f};
            if (v.length() == 8) return new float[]{((n >> 24) & 255) / 255f, ((n >> 16) & 255) / 255f, ((n >> 8) & 255) / 255f, (n & 255) / 255f};
        } catch (NumberFormatException ignored) {
        }
        return def;
    }

    @Override
    public String toString() {
        if (params.isEmpty()) return type;
        StringBuilder sb = new StringBuilder(type).append(':');
        params.forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
