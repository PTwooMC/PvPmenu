package dev.pvpmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;

/**
 * A smaller replacement for the full-screen totem pop animation.
 * Started from GameRendererMixin; drawn from the HUD callback.
 */
public final class TotemPop {
    private static final long DURATION_MS = 900;

    private static ItemStack stack = ItemStack.EMPTY;
    private static long startedAt;

    private TotemPop() {}

    public static void start(ItemStack s) {
        stack = s.copy();
        startedAt = Util.getMeasuringTimeMs();
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (stack.isEmpty()) return;

        long elapsed = Util.getMeasuringTimeMs() - startedAt;
        if (elapsed > DURATION_MS) {
            stack = ItemStack.EMPTY;
            return;
        }

        float t = elapsed / (float) DURATION_MS;
        float k;
        if (t < 0.18f) {
            k = ease(t / 0.18f);
        } else if (t > 0.75f) {
            k = 1f - ease((t - 0.75f) / 0.25f);
        } else {
            k = 1f;
        }

        float size = Visuals.totemScale() * k;
        if (size <= 0.01f) return;

        float cx = mc.getWindow().getScaledWidth() / 2f;
        float cy = mc.getWindow().getScaledHeight() / 2f;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(size, size);
        ctx.drawItem(stack, -8, -8);
        ctx.getMatrices().popMatrix();
    }
}
