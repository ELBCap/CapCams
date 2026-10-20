package dev.elbgg.capcams.client;

import dev.elbgg.capcams.CapCams;
import dev.elbgg.capcams.client.store.SceneStore;
import dev.elbgg.capcams.client.store.ShotStore;
import dev.elbgg.capcams.client.studio.Gizmos;
import dev.elbgg.capcams.client.studio.Studio;
import dev.elbgg.capcams.client.studio.StudioUi;
import foundry.imgui.fabric.api.event.RenderImGuiEventsFabric;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import dev.elbgg.capcams.client.command.CapCamsCommand;
import dev.elbgg.capcams.client.split.SplitManager;
import dev.elbgg.capcams.client.split.SplitRenderer;
import dev.elbgg.capcams.network.CapPayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CapCamsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientConfig.load();
        SplitRenderer.init();

        if (ClientConfig.studioAllowed()) {
            KeyBinding studioKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.capcams.studio", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.capcams"));
            ClientTickEvents.END_CLIENT_TICK.register(mc -> {
                while (studioKey.wasPressed()) {
                    if (Studio.isOpen()) Studio.close();
                    else if (mc.currentScreen == null) Studio.open();
                }
            });
        }
        WorldRenderEvents.AFTER_TRANSLUCENT.register(Gizmos::render);
        RenderImGuiEventsFabric.POST.register(StudioUi::draw);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
            if (ClientConfig.commandsAllowed()) CapCamsCommand.register(dispatcher);
        });

        ClientPlayNetworking.registerGlobalReceiver(CapPayloads.Split.ID, (p, ctx) -> ctx.client().execute(() -> {
            if (!ClientConfig.acceptServerSplits()) return;
            try {
                Control.split(p.layout(), p.seconds());
            } catch (RuntimeException e) {
                CapCams.LOGGER.warn("capcams_split '{}': {}", p.layout(), e.getMessage());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(CapPayloads.Panel.ID, (p, ctx) -> ctx.client().execute(() -> {
            if (!ClientConfig.acceptServerSplits()) return;
            try {
                Control.panel(p.index(), p.source());
            } catch (RuntimeException e) {
                CapCams.LOGGER.warn("capcams_panel {} '{}': {}", p.index(), p.source(), e.getMessage());
            }
        }));
        ClientPlayNetworking.registerGlobalReceiver(CapPayloads.Sync.ID, (p, ctx) -> ctx.client().execute(() -> {
            ShotStore.applySync(p.shots());
            SceneStore.applySync(p.scenes());
        }));
        ClientPlayNetworking.registerGlobalReceiver(CapPayloads.Notice.ID, (p, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().player != null) ctx.client().player.sendMessage(net.minecraft.text.Text.literal("[CapCams] " + p.message()), false);
        }));
        ClientPlayNetworking.registerGlobalReceiver(CapPayloads.Clear.ID, (p, ctx) -> ctx.client().execute(() -> Control.clear(p.seconds())));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SplitManager.INSTANCE.clearNow();
            ShotStore.clear();
            SceneStore.clear();
        });
    }
}
