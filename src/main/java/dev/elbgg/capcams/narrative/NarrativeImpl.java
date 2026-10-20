package dev.elbgg.capcams.narrative;

import dev.elbgg.capcams.CapCams;
import fr.loudo.narrativecraft.api.AddonContext;
import fr.loudo.narrativecraft.api.NarrativeCraftAPI;

final class NarrativeImpl {
    private NarrativeImpl() {}

    static boolean register() {
        NarrativeCraftAPI api;
        try {
            api = NarrativeCraftAPI.getInstance();
        } catch (IllegalStateException e) {
            return false;
        }
        AddonContext addon = api.createAddon(CapCams.MOD_ID, "CapCams", "Divisiones de camara (pantalla dividida) con Veil", "ELB_GG", "",
                NarrativeCraftAPI.VERSION);
        if (addon.isDisabled()) {
            CapCams.LOGGER.error("NarrativeCraft dejo el addon desactivado (API {})", NarrativeCraftAPI.VERSION);
            return true;
        }
        addon.registerInkAction(SplitInk.class, SplitInk::new);
        addon.registerInkAction(PanelInk.class, PanelInk::new);
        addon.registerInkAction(ClearInk.class, ClearInk::new);
        CapCams.LOGGER.info("Acciones de guion registradas en NarrativeCraft (API {})", NarrativeCraftAPI.VERSION);
        return true;
    }

    static float seconds(fr.loudo.narrativecraft.api.inkAction.syntax.ParsedCommand cmd, float def) {
        try {
            Object v = cmd.get("seconds");
            if (v instanceof Number n) return n.floatValue();
            if (v != null) return Float.parseFloat(v.toString());
        } catch (Exception ignored) {
        }
        return def;
    }
}
