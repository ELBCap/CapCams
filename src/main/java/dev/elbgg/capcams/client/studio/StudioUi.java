package dev.elbgg.capcams.client.studio;

import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.data.Shot;
import dev.elbgg.capcams.client.split.SplitManager;
import dev.elbgg.capcams.client.store.SceneStore;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class StudioUi {
    public static final Identifier THUMB = Identifier.of("capcams", "thumb");

    private static final ImString nameBuf = new ImString(64);
    private static final ImString sceneBuf = new ImString(64);
    private static String nameFor = "";
    private static final ImInt presetIdx = new ImInt(), easeIdx = new ImInt(), enterIdx = new ImInt();
    private static final ImInt[] panelIdx = {new ImInt(), new ImInt(), new ImInt(), new ImInt()};

    private StudioUi() {}

    public static void draw() {
        if (!Studio.isOpen()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        SplitManager.INSTANCE.setLoopSeconds(Studio.loopPreview ? Studio.loopLength() : 0f);
        float h = mc.getWindow().getHeight();
        ImGui.setNextWindowPos(12, 12, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(360, Math.min(h - 24, 760), ImGuiCond.FirstUseEver);
        if (ImGui.begin("CapCams Studio")) {
            ImGui.textWrapped("Clic derecho + arrastrar: mirar   WASD/Espacio/Mayus: volar   Rueda: velocidad (Ctrl = x3)\n"
                    + "Clic en una camara: seleccionar y arrastrar sus flechas   Enter: nueva camara   Supr: borrar   Esc: salir");
            ImGui.separator();
            cameras();
            division();
            scenes();
            ImGui.separator();
            if (!Studio.status.isEmpty()) ImGui.textWrapped(Studio.status);
        }
        Studio.setImguiWantsKeyboard(ImGui.getIO().getWantTextInput());
        ImGui.end();
        preview(mc);
    }

    private static void cameras() {
        if (!ImGui.collapsingHeader("Camaras", ImGuiTreeNodeFlags.DefaultOpen)) return;
        if (ImGui.button("+ Anadir en la vista del editor")) {
            Studio.addAtEditorCamera();
            Studio.applyLive();
        }
        List<String> pts = Studio.points();
        ImGui.beginChild("##camlist", 0, Math.min(150, 24 + pts.size() * 20), true);
        for (String n : pts) {
            if (ImGui.selectable(n, n.equals(Studio.selected()))) Studio.select(n);
        }
        if (pts.isEmpty()) ImGui.textDisabled("Sin camaras: pulsa Enter o el boton.");
        ImGui.endChild();

        String sel = Studio.selected();
        if (sel == null) return;
        Shot shot = Studio.shot(sel);
        Pose p = Studio.editPose(sel);
        if (shot == null || p == null) return;

        if (!sel.equals(nameFor)) {
            nameFor = sel;
            nameBuf.set(sel);
        }
        ImGui.text("Nombre");
        if (ImGui.inputText("##name", nameBuf, ImGuiInputTextFlags.EnterReturnsTrue)) {
            if (!Studio.rename(sel, nameBuf.get())) Studio.status = "No se pudo renombrar (nombre repetido o no valido).";
        }
        boolean editingEnd = Studio.editEnd && shot.end() != null;
        ImGui.textColored(1f, 0.9f, 0.3f, 1f, editingEnd ? "Editando la SALIDA" : "Editando la ENTRADA");
        float[] pos = {(float) p.x(), (float) p.y(), (float) p.z()};
        boolean changed = ImGui.dragFloat3("Posicion", pos, 0.02f);
        float[] rot = {p.yaw(), p.pitch(), p.roll()};
        changed |= ImGui.dragFloat3("Giro (yaw/pitch/roll)", rot, 0.2f);
        float[] fov = {p.fov()};
        changed |= ImGui.sliderFloat("FOV", fov, 10f, 120f);
        if (changed) {
            Studio.setPose(sel, new Pose(pos[0], pos[1], pos[2], rot[0], MathUtil.clampPitch(rot[1]), rot[2], fov[0]));
        }
        if (ImGui.isItemDeactivatedAfterEdit()) Studio.commitPoints();

        if (ImGui.button("Poner en mi vista")) {
            Studio.setPose(sel, EditorCamera.pose());
            Studio.commitPoints();
        }
        ImGui.sameLine();
        if (ImGui.button("Ir a esta camara")) EditorCamera.setPose(p);
        if (ImGui.button("Duplicar")) Studio.duplicate(sel);
        ImGui.sameLine();
        if (ImGui.button("Borrar")) Studio.delete(sel);
        extras(sel, shot);
    }

    private static final ImInt modeIdx = new ImInt(), shotEaseIdx = new ImInt();

    private static void extras(String sel, Shot shot) {
        if (!ImGui.collapsingHeader("Movimiento y extras", ImGuiTreeNodeFlags.DefaultOpen)) return;
        boolean hasEnd = shot.end() != null;
        if (ImGui.checkbox("Movimiento de entrada a salida", hasEnd)) {
            if (hasEnd) {
                Studio.setShot(sel, shot.withEnd(null));
                Studio.editEnd = false;
            } else {

                var f = EditorCamera.forward(shot.yaw(), shot.pitch()).multiply(2.0);
                Studio.setShot(sel, shot.withEnd(new Pose(shot.x() + f.x, shot.y() + f.y, shot.z() + f.z, shot.yaw(), shot.pitch(), shot.roll(), shot.fov())));
                Studio.editEnd = true;
            }
            Studio.commitPoints();
            SplitManager.INSTANCE.restartClock();
            return;
        }
        if (hasEnd) {
            if (ImGui.radioButton("Entrada", !Studio.editEnd)) Studio.editEnd = false;
            ImGui.sameLine();
            if (ImGui.radioButton("Salida", Studio.editEnd)) Studio.editEnd = true;
            if (ImGui.button("Salida = mi vista")) {
                Studio.setShot(sel, shot.withEnd(EditorCamera.pose()));
                Studio.commitPoints();
            }
            ImGui.sameLine();
            if (ImGui.button("Salida = entrada")) {
                Studio.setShot(sel, shot.withEnd(shot.base()));
                Studio.commitPoints();
            }
            float[] sec = {shot.seconds()}, del = {shot.delay()};
            boolean ch = ImGui.sliderFloat("Duracion (s)##mv", sec, 0.1f, 20f);
            ch |= ImGui.sliderFloat("Retraso (s)", del, 0f, 10f);
            shotEaseIdx.set(Math.max(0, java.util.Arrays.asList(Studio.EASES).indexOf(shot.ease() == null ? "inout" : shot.ease())));
            boolean ce = ImGui.combo("Curva##mv", shotEaseIdx, Studio.EASES);
            modeIdx.set(Math.max(0, java.util.Arrays.asList(Shot.MODES).indexOf(shot.mode() == null ? "once" : shot.mode())));
            boolean cm = ImGui.combo("Repeticion", modeIdx, new String[]{"Una vez", "Ida y vuelta", "Bucle"}) ;
            if (ch || ce || cm) {
                Shot cur = Studio.shot(sel);
                Studio.setShot(sel, cur.with(sec[0], Studio.EASES[shotEaseIdx.get()], Shot.MODES[modeIdx.get()], del[0], cur.shake(), cur.shakeHz()));
            }
            if (ImGui.isItemDeactivatedAfterEdit()) Studio.commitPoints();
            if (ImGui.button("Reproducir movimiento")) SplitManager.INSTANCE.restartClock();
            ImGui.sameLine();
            if (ImGui.checkbox("Bucle de previsualizacion", Studio.loopPreview)) Studio.loopPreview = !Studio.loopPreview;
        }
        float[] sh = {shot.shake()}, hz = {shot.shakeHz() <= 0 ? 1.5f : shot.shakeHz()};
        boolean cs = ImGui.sliderFloat("Temblor (grados)", sh, 0f, 5f);
        cs |= ImGui.sliderFloat("Frecuencia del temblor (Hz)", hz, 0.2f, 8f);
        if (cs) {
            Shot cur = Studio.shot(sel);
            Studio.setShot(sel, cur.with(cur.seconds(), cur.ease(), cur.mode(), cur.delay(), sh[0], hz[0]));
        }
        if (ImGui.isItemDeactivatedAfterEdit()) Studio.commitPoints();
    }

    private static void division() {
        if (!ImGui.collapsingHeader("Division de pantalla", ImGuiTreeNodeFlags.DefaultOpen)) return;
        boolean dirty = false;
        if (ImGui.checkbox("Vista previa en vivo", Studio.live)) {
            Studio.live = !Studio.live;
            dirty = true;
        }
        presetIdx.set(Studio.preset);
        if (ImGui.combo("Forma", presetIdx, Studio.PRESETS)) {
            Studio.preset = presetIdx.get();
            dirty = true;
        }
        String type = Studio.PRESETS[Studio.preset];
        float[] v = new float[1];
        if (type.startsWith("diagonal") || type.equals("triple_diagonal")) {
            v[0] = Studio.angle;
            if (ImGui.sliderFloat("Angulo", v, 0f, 45f)) {
                Studio.angle = v[0];
                dirty = true;
            }
        }
        if (!type.equals("pip") && !type.equals("quad") && !type.startsWith("triple")) {
            v[0] = Studio.pivot;
            if (ImGui.sliderFloat("Posicion del corte", v, 0.1f, 0.9f)) {
                Studio.pivot = v[0];
                dirty = true;
            }
        }
        if (type.equals("pip")) {
            v[0] = Studio.size;
            if (ImGui.sliderFloat("Tamano", v, 0.15f, 0.7f)) {
                Studio.size = v[0];
                dirty = true;
            }
        }
        v[0] = Studio.gap;
        if (ImGui.sliderFloat("Hueco", v, 0f, 0.05f, "%.4f")) {
            Studio.gap = v[0];
            dirty = true;
        }
        v[0] = Studio.border;
        if (ImGui.sliderFloat("Borde", v, 0f, 0.03f, "%.4f")) {
            Studio.border = v[0];
            dirty = true;
        }
        if (ImGui.colorEdit4("Color del borde", Studio.color)) dirty = true;
        enterIdx.set(Studio.enter);
        if (ImGui.combo("Entra por", enterIdx, Studio.ENTER)) {
            Studio.enter = enterIdx.get();
            dirty = true;
        }

        ImGui.separator();
        ImGui.text("Camara de cada panel");
        List<String> items = new ArrayList<>();
        items.add("main (vista del editor)");
        items.addAll(Studio.points());
        String[] arr = items.toArray(new String[0]);
        int panels = switch (type) {
            case "triple", "triple_diagonal" -> 3;
            case "quad" -> 4;
            default -> 2;
        };
        for (int i = 0; i < panels; i++) {
            int cur = 0;
            for (int k = 1; k < arr.length; k++) if (arr[k].equals(Studio.panels[i])) cur = k;
            panelIdx[i].set(cur);
            if (ImGui.combo("Panel " + (i + 1), panelIdx[i], arr)) {
                Studio.panels[i] = panelIdx[i].get() == 0 ? "main" : arr[panelIdx[i].get()];
                dirty = true;
            }
        }

        ImGui.separator();
        easeIdx.set(Studio.ease);
        if (ImGui.combo("Curva", easeIdx, Studio.EASES)) Studio.ease = easeIdx.get();
        v[0] = Studio.seconds;
        if (ImGui.sliderFloat("Duracion (s)", v, 0f, 5f)) Studio.seconds = v[0];
        if (ImGui.button("Reproducir transicion")) Studio.playTransition();
        ImGui.sameLine();
        if (ImGui.button("Quitar division")) {
            Studio.live = false;
            SplitManager.INSTANCE.clearNow();
        }
        if (dirty) Studio.applyLive();
    }

    private static void scenes() {
        if (!ImGui.collapsingHeader("Escenas guardadas")) return;
        ImGui.inputText("Nombre##scene", sceneBuf);
        if (ImGui.button("Guardar")) Studio.saveScene(sceneBuf.get().trim());
        ImGui.sameLine();
        if (ImGui.button("Cargar")) Studio.loadScene(sceneBuf.get().trim());
        ImGui.sameLine();
        if (ImGui.button("Copiar etiqueta Ink")) {
            ImGui.setClipboardText("# capcams_split " + sceneBuf.get().trim());
            Studio.status = "Copiado: # capcams_split " + sceneBuf.get().trim();
        }
        ImGui.separator();
        for (String n : SceneStore.names()) {
            if (ImGui.selectable(n)) {
                sceneBuf.set(n);
                Studio.loadScene(n);
            }
        }
    }

    private static void preview(MinecraftClient mc) {
        String sel = Studio.selected();
        if (sel == null) return;
        AdvancedFbo fbo = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(THUMB);
        if (fbo == null || !fbo.hasColorAttachment(0)) return;
        float w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        ImGui.setNextWindowPos(w - 352, h - 236, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(340, 222, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Vista previa: " + sel)) {
            float avail = ImGui.getContentRegionAvailX();
            ImGui.image(fbo.getColorTextureAttachment(0).getGlId(), avail, avail * 9f / 16f, 0f, 1f, 1f, 0f);
        }
        ImGui.end();
    }

    private static final class MathUtil {
        static float clampPitch(float p) {
            return Math.max(-89.9f, Math.min(89.9f, p));
        }
    }
}
