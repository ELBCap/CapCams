package dev.elbgg.capcams.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.elbgg.capcams.network.CapPayloads;
import dev.elbgg.capcams.server.Checks;
import dev.elbgg.capcams.server.ServerStore;
import dev.elbgg.capcams.server.ServerSync;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class CapSplitCommand {
    private CapSplitCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("capsplit")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("start")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.argument("seconds", FloatArgumentType.floatArg(0f, 60f))
                                        .then(CommandManager.argument("layout", StringArgumentType.greedyString())
                                                .executes(c -> {
                                                    String layout = StringArgumentType.getString(c, "layout").trim();
                                                    String err = Checks.layout(layout);
                                                    if (err != null) return fail(c, err);
                                                    return send(c, p -> new CapPayloads.Split(layout, FloatArgumentType.getFloat(c, "seconds")));
                                                })))))
                .then(CommandManager.literal("panel")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.argument("index", IntegerArgumentType.integer(1, 4))
                                        .then(CommandManager.argument("source", StringArgumentType.greedyString())
                                                .executes(c -> {
                                                    String source = StringArgumentType.getString(c, "source").trim();
                                                    String err = Checks.source(source);
                                                    if (err != null) return fail(c, err);
                                                    return send(c, p -> new CapPayloads.Panel(IntegerArgumentType.getInteger(c, "index"), source));
                                                })))))
                .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(c -> send(c, p -> new CapPayloads.Clear(0.8f)))
                                .then(CommandManager.argument("seconds", FloatArgumentType.floatArg(0f, 60f))
                                        .executes(c -> send(c, p -> new CapPayloads.Clear(FloatArgumentType.getFloat(c, "seconds")))))))
                .then(CommandManager.literal("reload").executes(c -> {
                    ServerStore.reload();
                    ServerSync.broadcast(c.getSource().getServer());
                    int shots = ServerStore.shots().size(), scenes = ServerStore.scenes().size();
                    c.getSource().sendFeedback(() -> Text.literal("CapCams: recargados " + shots + " planos y " + scenes + " escenas desde disco y sincronizados con los clientes."), true);
                    return shots + scenes;
                }))
                .then(CommandManager.literal("list")
                        .then(CommandManager.literal("shots").executes(c -> list(c, "Planos", ServerStore.shots().keySet())))
                        .then(CommandManager.literal("scenes").executes(c -> list(c, "Escenas", ServerStore.scenes().keySet())))));
    }

    private static int fail(CommandContext<ServerCommandSource> c, String message) {
        c.getSource().sendError(Text.literal(message));
        return 0;
    }

    private static int list(CommandContext<ServerCommandSource> c, String title, java.util.Set<String> names) {
        c.getSource().sendFeedback(() -> Text.literal(title + " del servidor: " + (names.isEmpty() ? "(ninguno)" : String.join(", ", names))), false);
        return names.size();
    }

    private static int send(CommandContext<ServerCommandSource> c, Function<ServerPlayerEntity, net.minecraft.network.packet.CustomPayload> payload)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(c, "targets");
        int sent = 0;
        List<String> without = new java.util.ArrayList<>();
        for (ServerPlayerEntity p : targets) {
            var pl = payload.apply(p);
            if (ServerPlayNetworking.canSend(p, pl.getId())) {
                ServerPlayNetworking.send(p, pl);
                sent++;
            } else {
                without.add(p.getGameProfile().getName());
            }
        }
        final int n = sent;
        if (n > 0) c.getSource().sendFeedback(() -> Text.literal("CapCams: orden enviada a " + n + " jugador(es)."), true);
        if (!without.isEmpty()) {
            c.getSource().sendError(Text.literal("Sin CapCams en el cliente (no recibe la orden): " + String.join(", ", without)));
        }
        return n;
    }
}
