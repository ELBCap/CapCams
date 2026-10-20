package dev.elbgg.capcams.client.split;

import dev.elbgg.capcams.data.Pose;
import dev.elbgg.capcams.data.Shot;

import dev.elbgg.capcams.CapCams;
import dev.elbgg.capcams.client.studio.Studio;
import dev.elbgg.capcams.client.studio.StudioUi;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.platform.VeilEventPlatform;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public final class SplitRenderer {
    private static final Identifier PIPELINE = Identifier.of(CapCams.MOD_ID, "split");
    private static final Identifier SHADER = Identifier.of(CapCams.MOD_ID, "split");
    private static final Identifier[] PANEL_FBO = {
            null,
            Identifier.of(CapCams.MOD_ID, "panel_1"), Identifier.of(CapCams.MOD_ID, "panel_2"),
            Identifier.of(CapCams.MOD_ID, "panel_3"), Identifier.of(CapCams.MOD_ID, "panel_4")
    };

    private static boolean warnedPipeline, warnedShader, warnedFbo;

    private SplitRenderer() {}

    public static void init() {
        VeilEventPlatform.INSTANCE.onVeilRenderLevelStage(
                (stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) ->
                        onStage(stage, deltaTracker));
        VeilEventPlatform.INSTANCE.preVeilPostProcessing(SplitRenderer::onPreProcessing);
        ClientTickEvents.END_CLIENT_TICK.register(SplitRenderer::tick);
    }

    private static void tick(MinecraftClient mc) {
        PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        if (SplitManager.INSTANCE.active()) {
            if (manager.getPipeline(PIPELINE) == null) {
                if (!warnedPipeline) {
                    warnedPipeline = true;
                    CapCams.LOGGER.warn("capcams:split no se cargo");
                }
                return;
            }
            manager.add(PIPELINE);
        } else {
            manager.remove(PIPELINE);
        }
    }

    private static void onStage(VeilRenderLevelStageEvent.Stage stage, RenderTickCounter tracker) {
        if (stage != VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        int w = mc.getWindow().getFramebufferWidth(), h = mc.getWindow().getFramebufferHeight();
        if (w <= 0 || h <= 0) return;
        if (Studio.isOpen() && Studio.selected() != null) {
            Shot sh = Studio.shot(Studio.selected());
            Pose sel = sh == null ? null : sh.at(SplitManager.INSTANCE.elapsed());
            AdvancedFbo thumb = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(StudioUi.THUMB);
            if (sel != null && thumb != null) renderPanel(mc, thumb, sel, tracker);
        }

        float partial = tracker.getTickDelta(true);
        SplitManager.Frame frame = SplitManager.INSTANCE.frame(mc, partial, (float) w / h);
        if (frame == null) return;

        for (int i = 1; i <= Geometry.MAX_PANELS; i++) {
            Pose pose = frame.pose[i];
            if (pose == null) continue;
            AdvancedFbo fbo = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(PANEL_FBO[i]);
            if (fbo == null) {
                if (!warnedFbo) {
                    warnedFbo = true;
                }
                continue;
            }
            renderPanel(mc, fbo, pose, tracker);
        }
    }

    private static void renderPanel(MinecraftClient mc, AdvancedFbo fbo, Pose pose, RenderTickCounter tracker) {
        float aspect = (float) fbo.getWidth() / Math.max(1, fbo.getHeight());
        float far = mc.gameRenderer.getFarPlaneDistance();
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(pose.fov()), aspect, 0.05f, far);
        Quaternionf orientation = new Quaternionf().rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(pose.yaw()), -(float) Math.toRadians(pose.pitch()), (float) Math.toRadians(pose.roll()));
        fbo.bind(false);
        fbo.clear();
        AdvancedFbo.unbind();
        VeilLevelPerspectiveRenderer.render(fbo, new Matrix4f(), projection, new Vector3d(pose.x(), pose.y(), pose.z()),
                orientation, mc.options.getClampedViewDistance(), tracker, false);
    }

   private static void onPreProcessing(Identifier name, PostPipeline pipeline, PostPipeline.Context context) {
        if (!PIPELINE.equals(name)) return;
        ShaderProgram program = VeilRenderSystem.renderer().getShaderManager().getShader(SHADER);
        if (program == null) {
            if (!warnedShader) {
                warnedShader = true;
                CapCams.LOGGER.warn("capcams:split no se cargo");
            }
            return;
        }
        ShaderUniformAccess count = program.getUniformSafe("PanelCount");
        SplitManager.Frame frame = SplitManager.INSTANCE.lastFrame();
        if (frame == null || VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            count.setInt(0);
            return;
        }
        count.setInt(frame.panelCount);

        int panels = Geometry.MAX_PANELS, edges = Geometry.MAX_EDGES;
        int[] tex = new int[panels];
        int[] edgeCount = new int[panels];
        float[] nx = new float[panels * edges], ny = new float[panels * edges], c = new float[panels * edges];
        for (int i = 0; i < frame.panelCount; i++) {
            tex[i] = frame.texture[i];
            var planes = frame.shape.panels.get(i);
            edgeCount[i] = Math.min(planes.size(), edges);
            for (int e = 0; e < edgeCount[i]; e++) {
                Geometry.Plane p = planes.get(e);
                nx[i * edges + e] = p.nx();
                ny[i * edges + e] = p.ny();
                c[i * edges + e] = p.c();
            }
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        program.getUniformSafe("PanelTex").setInts(tex);
        program.getUniformSafe("EdgeCount").setInts(edgeCount);
        program.getUniformSafe("EdgeNX").setFloats(nx);
        program.getUniformSafe("EdgeNY").setFloats(ny);
        program.getUniformSafe("EdgeC").setFloats(c);
        program.getUniformSafe("Aspect").setFloat((float) mc.getWindow().getFramebufferWidth() / Math.max(1, mc.getWindow().getFramebufferHeight()));
        program.getUniformSafe("BorderWidth").setFloat(frame.shape.border);
        float[] bc = frame.shape.borderColor, gc = frame.shape.gapColor;
        program.getUniformSafe("BorderColor").setVector(bc[0], bc[1], bc[2], bc[3]);
        program.getUniformSafe("GapColor").setVector(gc[0], gc[1], gc[2], gc[3]);
    }
}
