package dev.elbgg.capcams;

import dev.elbgg.capcams.command.CapSplitCommand;
import dev.elbgg.capcams.narrative.NarrativeBridge;
import dev.elbgg.capcams.server.ServerSync;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import dev.elbgg.capcams.network.CapPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CapCams implements ModInitializer {
    public static final String MOD_ID = "capcams";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(CapPayloads.Split.ID, CapPayloads.Split.CODEC);
        PayloadTypeRegistry.playS2C().register(CapPayloads.Panel.ID, CapPayloads.Panel.CODEC);
        PayloadTypeRegistry.playS2C().register(CapPayloads.Clear.ID, CapPayloads.Clear.CODEC);
        PayloadTypeRegistry.playS2C().register(CapPayloads.Sync.ID, CapPayloads.Sync.CODEC);
        PayloadTypeRegistry.playS2C().register(CapPayloads.Notice.ID, CapPayloads.Notice.CODEC);
        PayloadTypeRegistry.playC2S().register(CapPayloads.Edit.ID, CapPayloads.Edit.CODEC);
        ServerSync.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> CapSplitCommand.register(dispatcher));
        NarrativeBridge.init();
    }
}
