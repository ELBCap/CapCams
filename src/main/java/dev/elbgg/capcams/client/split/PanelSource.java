package dev.elbgg.capcams.client.split;

import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.data.Shot;

import dev.elbgg.capcams.client.store.ShotStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.UUID;

public sealed interface PanelSource {
    Pose resolve(MinecraftClient mc, float partial);

    String text();

    record Main() implements PanelSource {
        @Override
        public Pose resolve(MinecraftClient mc, float partial) {
            return null;
        }

        @Override
        public String text() {
            return "main";
        }
    }

    record Fixed(Pose pose, String text) implements PanelSource {
        @Override
        public Pose resolve(MinecraftClient mc, float partial) {
            return pose;
        }
    }

    record ShotRef(String name) implements PanelSource {
        @Override
        public Pose resolve(MinecraftClient mc, float partial) {
            Shot shot = ShotStore.get(name);
            return shot == null ? null : shot.at(SplitManager.INSTANCE.elapsed());
        }

        @Override
        public String text() {
            return "shot:" + name;
        }
    }

    record Closeup(String key, double distance, double height, double sideDegrees) implements PanelSource {
        @Override
        public Pose resolve(MinecraftClient mc, float partial) {
            if (mc.world == null) return null;
            Entity e = find(mc, key);
            if (e == null) return null;
            Vec3d eye = e.getLerpedPos(partial).add(0, e.getStandingEyeHeight() + height, 0);
            double yaw = Math.toRadians(e.getBodyYaw() + sideDegrees);
            Vec3d cam = eye.add(-Math.sin(yaw) * distance, 0.0, Math.cos(yaw) * distance);
            return Pose.lookAt(cam.x, cam.y, cam.z, eye.x, eye.y, eye.z, 45f);
        }

        @Override
        public String text() {
            return "entity:" + key + ":" + distance + ":" + height + ":" + sideDegrees;
        }

        private static Entity find(MinecraftClient mc, String key) {
            try {
                UUID id = UUID.fromString(key);
                for (Entity e : mc.world.getEntities()) if (e.getUuid().equals(id)) return e;
            } catch (IllegalArgumentException ignored) {
            }
            String k = key.toLowerCase(Locale.ROOT).replace('_', ' ');
            Entity best = null;
            double bd = Double.MAX_VALUE;
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;
                String n = e.getName().getString().toLowerCase(Locale.ROOT);
                if (!n.equals(k)) continue;
                double d = e.squaredDistanceTo(mc.player);
                if (d < bd) {
                    bd = d;
                    best = e;
                }
            }
            return best;
        }
    }

    static PanelSource parse(String raw) {
        String s = raw.trim();
        String low = s.toLowerCase(Locale.ROOT);
        if (low.equals("main") || low.isEmpty()) return new Main();
        int c = s.indexOf(':');
        if (c < 0) throw new IllegalArgumentException("Camara desconocida: " + raw);
        String kind = low.substring(0, c);
        String rest = s.substring(c + 1);
        switch (kind) {
            case "shot" -> {
                Shot p = ShotStore.get(rest);
                if (p == null) throw new IllegalArgumentException("No existe el plano '" + rest + "' (usa /capcams shot list)");
                return new ShotRef(rest.toLowerCase(Locale.ROOT));
            }
            case "pos" -> {
                return new Fixed(parsePose(rest), s);
            }
            case "look" -> {
                String[] ft = rest.split(">");
                if (ft.length != 2) throw new IllegalArgumentException("look: x,y,z>tx,ty,tz[,fov]");
                double[] f = nums(ft[0], 3);
                String[] tparts = ft[1].split(",");
                double[] t = nums(String.join(",", tparts[0], tparts[1], tparts[2]), 3);
                float fov = tparts.length > 3 ? Float.parseFloat(tparts[3].trim()) : Pose.DEFAULT_FOV;
                return new Fixed(Pose.lookAt(f[0], f[1], f[2], t[0], t[1], t[2], fov), s);
            }
            case "entity" -> {
                String[] p = rest.split(":");
                double dist = p.length > 1 ? Double.parseDouble(p[1]) : 2.2;
                double h = p.length > 2 ? Double.parseDouble(p[2]) : 0.0;
                double side = p.length > 3 ? Double.parseDouble(p[3]) : 15.0;
                return new Closeup(p[0], dist, h, side);
            }
            default -> throw new IllegalArgumentException("Tipo de camara desconocido: " + kind);
        }
    }

    private static double[] nums(String csv, int n) {
        String[] p = csv.split(",");
        if (p.length < n) throw new IllegalArgumentException("Faltan numeros en '" + csv + "'");
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = Double.parseDouble(p[i].trim());
        return out;
    }

    private static Pose parsePose(String csv) {
        String[] p = csv.split(",");
        if (p.length < 5) throw new IllegalArgumentException("pos: x,y,z,yaw,pitch[,roll[,fov]]");
        return new Pose(Double.parseDouble(p[0].trim()), Double.parseDouble(p[1].trim()), Double.parseDouble(p[2].trim()),
                Float.parseFloat(p[3].trim()), Float.parseFloat(p[4].trim()),
                p.length > 5 ? Float.parseFloat(p[5].trim()) : 0f,
                p.length > 6 ? Float.parseFloat(p[6].trim()) : Pose.DEFAULT_FOV);
    }
}
