package dev.elbgg.capcams.server;

import dev.elbgg.capcams.CapCams;
import dev.elbgg.capcams.network.CapPayloads;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerSync {
    private ServerSync() {}

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(s -> ServerStore.reload());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(CapPayloads.Edit.ID, (p, ctx) -> ctx.server().execute(() -> handle(ctx.server(), ctx.player(), p)));
    }

    public static void sendTo(ServerPlayerEntity p) {
        if (ServerPlayNetworking.canSend(p, CapPayloads.Sync.ID)) {
            ServerPlayNetworking.send(p, new CapPayloads.Sync(ServerStore.shotsJson(), ServerStore.scenesJson()));
        }
    }

    public static void broadcast(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendTo(p);
    }

    private static void notice(ServerPlayerEntity p, String msg) {
        if (ServerPlayNetworking.canSend(p, CapPayloads.Notice.ID)) ServerPlayNetworking.send(p, new CapPayloads.Notice(msg));
    }

    private static void handle(MinecraftServer server, ServerPlayerEntity player, CapPayloads.Edit e) {
        if (!player.hasPermissionLevel(2)) {
            notice(player, "No tienes permiso para editar camaras (hace falta ser operador).");
            sendTo(player);
            return;
        }
        try {
            switch (e.op()) {
                case "shot_put" -> {
                    requireName(e.name());
                    var shot = ServerStore.parseShot(e.json());
                    if (shot == null) throw new IllegalArgumentException("plano vacio");
                    ServerStore.putShot(e.name(), shot);
                }
                case "shot_remove" -> ServerStore.removeShot(e.name());
                case "shot_rename" -> {
                    requireName(e.arg());
                    if (!ServerStore.renameShot(e.name(), e.arg())) throw new IllegalArgumentException("no se pudo renombrar (nombre repetido o inexistente)");
                }
                case "scene_put" -> {
                    requireName(e.name());
                    var scene = ServerStore.parseScene(e.json());
                    if (scene == null || scene.spec() == null) throw new IllegalArgumentException("escena vacia");
                    ServerStore.putScene(e.name(), scene);
                }
                case "scene_remove" -> ServerStore.removeScene(e.name());
                default -> throw new IllegalArgumentException("operacion desconocida: " + e.op());
            }
            broadcast(server);
        } catch (Exception ex) {
            CapCams.LOGGER.warn("Edicion '{}' de {} rechazada: {}", e.op(), player.getGameProfile().getName(), ex.toString());
            notice(player, "No se pudo aplicar el cambio: " + ex.getMessage());
            sendTo(player);
        }
    }

    private static void requireName(String n) {
        if (!ServerStore.validName(n)) throw new IllegalArgumentException("nombre no valido (a-z, 0-9, _ y -)");
    }
}
