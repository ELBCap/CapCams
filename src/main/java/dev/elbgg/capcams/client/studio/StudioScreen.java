package dev.elbgg.capcams.client.studio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class StudioScreen extends Screen {
    public StudioScreen() {
        super(Text.literal("CapCams Studio"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        EditorCamera.update(!Studio.imguiWantsKeyboard());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (Gizmos.beginDrag(mx, my)) return true;
            String hit = Gizmos.pickPoint(mx, my);
            if (hit != null) {
                Studio.select(hit);
                return true;
            }
            return false;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 1) {
            EditorCamera.look(dx, dy);
            return true;
        }
        if (button == 0 && Gizmos.dragging()) {
            Gizmos.drag(mx, my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) Gizmos.endDrag();
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        EditorCamera.scrollSpeed(vAmount);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Studio.imguiWantsKeyboard()) return super.keyPressed(keyCode, scanCode, modifiers);
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                Studio.close();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                Studio.addAtEditorCamera();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                String sel = Studio.selected();
                if (sel != null) Studio.delete(sel);
                return true;
            }
            default -> {
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        Studio.closed();
    }

    static boolean isActive() {
        return MinecraftClient.getInstance().currentScreen instanceof StudioScreen;
    }
}
