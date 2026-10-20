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
        keyword = "capcams_panel",
        description = "Asigna la camara de un panel de la pantalla dividida.",
        syntax = "capcams_panel <index:int> <source:string>",
        side = Side.SERVER
)
public class PanelInk extends InkAction {
    private int index;
    private String source = "main";

    @Override
    protected InkActionResult doValidate(ParsedCommand command, IScene scene) {
        index = command.getInt("index");
        source = command.getString("source");
        if (index < 1 || index > 4) return InkActionResult.error("capcams_panel: el panel va de 1 a 4");
        String err = dev.elbgg.capcams.server.Checks.source(source);
        if (err != null) return InkActionResult.error("capcams_panel: " + err);
        return InkActionResult.singleOk();
    }

    @Override
    protected InkActionResult doExecute(IPlayerSession session) {
        ServerPlayNetworking.send(session.getPlayer(), new CapPayloads.Panel(index, source));
        return InkActionResult.singleOk();
    }
}
