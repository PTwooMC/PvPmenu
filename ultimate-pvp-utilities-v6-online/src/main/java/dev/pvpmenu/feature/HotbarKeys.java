package dev.pvpmenu.feature;

import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Small key labels on the hotbar (and in inventories, see ScreenFeatures). */
public final class HotbarKeys {
    private HotbarKeys() {}

    /** Short text for a key name so it always fits a 16 pixel slot (mouse buttons become M1, M2, ...). */
    public static String shorten(String label) {
        if (label == null) return "";
        String l = label.trim();
        String low = l.toLowerCase(java.util.Locale.ROOT);
        if (low.contains("button")) {
            if (low.startsWith("left")) return "M1";
            if (low.startsWith("right")) return "M2";
            if (low.startsWith("middle")) return "M3";
            String digits = low.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? "M" : "M" + digits;
        }
        if (low.equals("left button")) return "M1";
        if (low.equals("right button")) return "M2";
        if (low.equals("middle button")) return "M3";
        if (low.equals("not bound")) return "";
        if (low.startsWith("left ")) l = "L" + l.substring(5);
        else if (low.startsWith("right ")) l = "R" + l.substring(6);
        if (low.startsWith("keypad ")) l = "N" + l.substring(7);
        return l;
    }

    public static void drawLabel(DrawContext ctx, MinecraftClient mc, String raw, int x, int y, int color) {
        String label = shorten(raw);
        if (label.isEmpty()) return;
        float s = Math.max(50, Modules.HOTBARKEYS.optOf("size", 75)) / 100f;
        int tw = mc.textRenderer.getWidth(label);
        if (tw * s > 13f) s = 13f / tw;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) x, (float) y);
        ctx.getMatrices().scale(s, s);
        ctx.drawText(mc.textRenderer, label, 1, 0, color, true);
        ctx.getMatrices().popMatrix();
    }

    public static void renderHud(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.HOTBARKEYS.isEnabled() || mc.player == null || mc.options.hudHidden) return;
        if (!Modules.HOTBARKEYS.flagOf("hotbar", true)) return;
        int color = Modules.HOTBARKEYS.optOf("color", 0xFFFFFFFF) | 0xFF000000;
        int left = ctx.getScaledWindowWidth() / 2 - 91 + 3;
        int top = ctx.getScaledWindowHeight() - 16 - 3;
        for (int i = 0; i < 9; i++) {
            String label = mc.options.hotbarKeys[i].getBoundKeyLocalizedText().getString();
            drawLabel(ctx, mc, label, left + i * 20, top, color);
        }
    }
}
