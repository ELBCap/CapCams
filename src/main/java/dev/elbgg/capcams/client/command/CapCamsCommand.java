package dev.elbgg.capcams.client.command;

import dev.elbgg.capcams.client.split.CameraPoses;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.elbgg.capcams.client.Control;
import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.client.split.SplitManager;
import dev.elbgg.capcams.client.store.SceneStore;
import dev.elbgg.capcams.data.SceneData;
import dev.elbgg.capcams.client.store.ShotStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class CapCamsCommand {
    private static final List<String> PRESETS = List.of("vertical", "horizontal", "diagonal", "diagonal_r", "triple",
            "triple_diagonal", "quad", "pip");

    private static final SuggestionProvider<FabricClientCommandSource> PRESET_SUGGEST = (c, b) -> CommandSource.suggestMatching(PRESETS, b);
    private static final SuggestionProvider<FabricClientCommandSource> SHOT_SUGGEST = (c, b) -> CommandSource.suggestMatching(ShotStore.all().keySet(), b);
    private static final SuggestionProvider<FabricClientCommandSource> SCENE_SUGGEST = (c, b) -> CommandSource.suggestMatching(SceneStore.names(), b);

    private CapCamsCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        d.register(literal("capcams")
                .then(literal("preview").then(argument("layout", StringArgumentType.string()).suggests(PRESET_SUGGEST)
                        .executes(c -> run(c, () -> "Division: " + Control.split(StringArgumentType.getString(c, "layout"), 1.0f)))
                        .then(argument("seconds", FloatArgumentType.floatArg(0f, 30f))
                                .executes(c -> run(c, () -> "Division: " + Control.split(StringArgumentType.getString(c, "layout"),
                                        FloatArgumentType.getFloat(c, "seconds")))))))
                .then(literal("clear")
                        .executes(c -> run(c, () -> {
                            Control.clear(0.8f);
                            return "Division quitada.";
                        }))
                        .then(argument("seconds", FloatArgumentType.floatArg(0f, 30f))
                                .executes(c -> run(c, () -> {
                                    Control.clear(FloatArgumentType.getFloat(c, "seconds"));
                                    return "Division quitada.";
                                }))))
                .then(literal("panel").then(argument("index", IntegerArgumentType.integer(1, 4))
                        .then(literal("here").executes(c -> run(c, () -> {
                            Pose p = currentPose();
                            Control.panel(IntegerArgumentType.getInteger(c, "index"), "pos:" + p.toText());
                            return "Panel " + IntegerArgumentType.getInteger(c, "index") + " = tu vista actual.";
                        })))
                        .then(argument("source", StringArgumentType.greedyString())
                                .executes(c -> run(c, () -> {
                                    Control.panel(IntegerArgumentType.getInteger(c, "index"), StringArgumentType.getString(c, "source"));
                                    return "Panel " + IntegerArgumentType.getInteger(c, "index") + " actualizado.";
                                })))))
                .then(literal("shot")
                        .then(literal("save").then(argument("name", StringArgumentType.word()).executes(c -> run(c, () -> {
                            String name = StringArgumentType.getString(c, "name");
                            ShotStore.put(name, currentPose());
                            return "Plano '" + name + "' guardado desde tu vista actual.";
                        }))))
                        .then(literal("delete").then(argument("name", StringArgumentType.word()).suggests(SHOT_SUGGEST).executes(c -> run(c, () ->
                                ShotStore.remove(StringArgumentType.getString(c, "name")) ? "Plano borrado." : "No existe ese plano."))))
                        .then(literal("list").executes(c -> run(c, () -> ShotStore.all().isEmpty()
                                ? "No hay planos guardados."
                                : "Planos: " + String.join(", ", ShotStore.all().keySet())))))
                .then(literal("scene")
                        .then(literal("save").then(argument("name", StringArgumentType.word()).executes(c -> run(c, () -> {
                            String name = StringArgumentType.getString(c, "name");
                            if (!SceneStore.validName(name)) throw new IllegalArgumentException("Nombre no valido (a-z, 0-9, _ y -).");
                            SplitManager m = SplitManager.INSTANCE;
                            if (!m.active()) throw new IllegalArgumentException("No hay ninguna division activa que guardar (usa /capcams preview).");
                            SceneStore.put(name, new SceneData(m.spec().toString(), m.sourceTexts()));
                            return "Escena '" + name + "' guardada. En Ink: # capcams_split " + name;
                        }))))
                        .then(literal("load").then(argument("name", StringArgumentType.word()).suggests(SCENE_SUGGEST).executes(c -> run(c, () -> {
                            String name = StringArgumentType.getString(c, "name");
                            if (SceneStore.get(name) == null) throw new IllegalArgumentException("No existe la escena '" + name + "'.");
                            return "Cargada " + Control.split(name, 1.0f);
                        }))))
                        .then(literal("delete").then(argument("name", StringArgumentType.word()).suggests(SCENE_SUGGEST).executes(c -> run(c, () -> {
                            return SceneStore.remove(StringArgumentType.getString(c, "name")) ? "Escena borrada." : "No existe esa escena.";
                        }))))
                        .then(literal("list").executes(c -> run(c, () -> SceneStore.names().isEmpty()
                                ? "No hay escenas guardadas." : "Escenas: " + String.join(", ", SceneStore.names())))))
                .then(literal("studio").executes(c -> {
                    MinecraftClient.getInstance().send(dev.elbgg.capcams.client.studio.Studio::open);
                    return 1;
                }))
                .then(literal("info").executes(c -> run(c, () -> {
                    SplitManager m = SplitManager.INSTANCE;
                    return m.active() ? "Division: " + m.spec() + "  paneles: " + m.sourceTexts() : "Sin division activa. Presets: " + String.join(", ", PRESETS);
                }))));
    }

    private interface Action {
        String run();
    }

    private static int run(CommandContext<FabricClientCommandSource> c, Action a) {
        try {
            c.getSource().sendFeedback(Text.literal(a.run()));
            return 1;
        } catch (RuntimeException e) {
            c.getSource().sendError(Text.literal(String.valueOf(e.getMessage())));
            return 0;
        }
    }

    private static Pose currentPose() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return CameraPoses.of(mc.gameRenderer.getCamera(), mc.options.getFov().getValue().floatValue());
    }
}
