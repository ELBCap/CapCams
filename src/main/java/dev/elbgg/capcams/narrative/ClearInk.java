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
        keyword = "capcams_clear",
        description = "Quita la pantalla dividida.",
        syntax = "capcams_clear (seconds:float)",
        side = Side.SERVER
)
public class ClearInk extends InkAction {
    private float seconds = 0.8f;

    @Override
    protected InkActionResult doValidate(ParsedCommand command, IScene scene) {
        seconds = NarrativeImpl.seconds(command, 0.8f);
        return InkActionResult.singleOk();
    }

    @Override
    protected InkActionResult doExecute(IPlayerSession session) {
        ServerPlayNetworking.send(session.getPlayer(), new CapPayloads.Clear(seconds));
        return InkActionResult.singleOk();
    }
}
