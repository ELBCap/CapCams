package dev.elbgg.capcams.narrative;

import dev.elbgg.capcams.CapCams;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class NarrativeBridge {
    private static boolean registered;

    private NarrativeBridge() {}

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("narrativecraft")) {
            CapCams.LOGGER.info("NarrativeCraft no esta instalado: CapCams funciona solo con sus comandos.");
            return;
        }
        if (tryRegister()) return;
        ServerLifecycleEvents.SERVER_STARTING.register(s -> {
            if (!registered && !tryRegister()) CapCams.LOGGER.error("NarrativeCraft no dejo registrar las acciones de CapCams");
        });
    }

    private static boolean tryRegister() {
        try {
            registered = NarrativeImpl.register();
        } catch (Throwable t) {
            registered = false;
        }
        return registered;
    }
}
