package dev.elbgg.capcams.narrative;

import dev.elbgg.capcams.network.CapPayloads;
import fr.loudo.narrativecraft.api.inkAction.InkAction;
import fr.loudo.narrativecraft.api.inkAction.InkActionResult;
import fr.loudo.narrativecraft.api.inkAction.InkCommand;
import fr.loudo.narrativecraft.api.inkAction.syntax.ParsedCommand;
import fr.loudo.narrativecraft.api.narrative.scene.IScene;
import fr.loudo.narrativecraft.api.session.IPlayerSession;
import fr.loudo.narrativecraft.api.utils.Side;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

@InkCommand(
        keyword = "capcams_split",
        description = "Divide la pantalla en paneles de camara (estilo anime/pelicula).",
        syntax = "capcams_split <layout:string> (seconds:float)",
        side = Side.SERVER
)
public class SplitInk extends InkAction {
    private String layout = "";
    private float seconds = 1.0f;

    @Override
    protected InkActionResult doValidate(ParsedCommand command, IScene scene) {
        layout = command.getString("layout");
        seconds = NarrativeImpl.seconds(command, 1.0f);
        String err = dev.elbgg.capcams.server.Checks.layout(layout);
        return err != null ? InkActionResult.error("capcams_split: " + err) : InkActionResult.singleOk();
    }

    @Override
    protected InkActionResult doExecute(IPlayerSession session) {
        ServerPlayNetworking.send(session.getPlayer(), new CapPayloads.Split(layout, seconds));
        return InkActionResult.singleOk();
    }
}
