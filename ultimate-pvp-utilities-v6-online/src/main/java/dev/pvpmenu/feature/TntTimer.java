package dev.pvpmenu.feature;

import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

/**
 * Shows the time left on lit TNT as text floating over it. The text position is worked out with a simple
 * camera projection, so it needs no rendering hooks.
 */
public final class TntTimer {
    /** Field of view the game used for the world this frame (set by GameRendererMixin). */
    public static float worldFov = 70f;
    public static boolean fovSeen = false;
    /** Partial tick the world was drawn with this frame (set by CameraMixin). */
    public static float cameraDelta = 0f;
    public static boolean deltaSeen = false;

    private TntTimer() {}

    public static void noteFov(float f) {
        if (!fovSeen) {
            worldFov = f;
            fovSeen = true;
        }
    }

    /** Screen position of a world point, or null if it is behind the camera. */
    public static float[] project(MinecraftClient mc, Vec3d eye, double wx, double wy, double wz,
                                  float yawDeg, float pitchDeg, int sw, int sh) {
        double dx = wx - eye.x;
        double dy = wy - eye.y;
        double dz = wz - eye.z;
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);

        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, rz = -sy;
        double ux = -sy * sp, uy = cp, uz = cy * sp;

        double zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.05) return null;
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;

        double fovDeg = fovSeen ? worldFov : mc.options.getFov().getValue();
        double fov = Math.toRadians(fovDeg);
        double scale = (sh / 2.0) / Math.tan(fov / 2.0);
        float px = (float) (sw / 2.0 + xc / zc * scale);
        float py = (float) (sh / 2.0 - yc / zc * scale);
        return new float[]{px, py, (float) zc};
    }

    public static void render(DrawContext ctx, MinecraftClient mc, float delta) {
        try {
            renderInner(ctx, mc, delta);
        } finally {
            fovSeen = false;
        }
    }

    private static void renderInner(DrawContext ctx, MinecraftClient mc, float hudDelta) {
        float delta = deltaSeen ? cameraDelta : hudDelta;
        if (!Modules.TNT.isEnabled() || mc.player == null || mc.world == null || mc.options.hudHidden) return;
        if (!mc.options.getPerspective().isFirstPerson() && !Freelook.active()) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        Vec3d eye = mc.player.getCameraPosVec(delta);
        float yaw = Freelook.active() ? Freelook.yaw() : mc.player.getYaw(delta);
        float pitch = Freelook.active() ? Freelook.pitch() : mc.player.getPitch(delta);
        double max = Modules.TNT.optOf("range", 40);
        boolean dist = Modules.TNT.flagOf("dist", true);

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof TntEntity tnt)) continue;
            double d = Math.sqrt(tnt.squaredDistanceTo(mc.player));
            if (d > max) continue;
            Vec3d lp = tnt.getLerpedPos(delta);
            double ex = lp.x, ey = lp.y, ez = lp.z;
            float[] p = project(mc, eye, ex, ey + 1.1, ez, yaw, pitch, sw, sh);
            if (p == null) continue;

            float secs = tnt.getFuse() / 20f;
            int color = secs > 2f ? 0xFF4ADE80 : secs > 1f ? 0xFFFACC15 : 0xFFF87171;
            String s = String.format(Locale.ROOT, "%.1fs", secs);
            if (dist) s += String.format(Locale.ROOT, "  %.0fm", d);
            int tw = mc.textRenderer.getWidth(s);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(p[0], p[1] - 4f);
            ctx.fill(-tw / 2 - 3, -2, tw - tw / 2 + 3, 10, 0x88000000);
            ctx.drawText(mc.textRenderer, s, -tw / 2, 0, color, true);
            ctx.getMatrices().popMatrix();
        }
    }
}
