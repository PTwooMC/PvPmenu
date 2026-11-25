package dev.pvpmenu.feature;

import dev.pvpmenu.gui.Draw;
import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;

/**
 * Shield State Indicator: the shield in your hotbar and off-hand slot is tinted by its state
 * (grey = ready, green = raised, red = disabled on cooldown). Optionally also a small text under the crosshair.
 */
public final class ShieldStatus {
    private ShieldStatus() {}

    private static int stateColor(MinecraftClient mc, ItemStack shield) {
        float cd = mc.player.getItemCooldownManager().getCooldownProgress(shield, 0f);
        if (cd > 0f) return Modules.SHIELDSTATUS.optOf("cdcol", 0xFFF87171) | 0xFF000000;
        if (mc.player.isBlocking() && mc.player.getActiveItem().isOf(Items.SHIELD))
            return Modules.SHIELDSTATUS.optOf("upcol", 0xFF4ADE80) | 0xFF000000;
        return Modules.SHIELDSTATUS.optOf("rdycol", 0xFF9CA3AF) | 0xFF000000;
    }

    private static String stateText(MinecraftClient mc, ItemStack shield) {
        float cd = mc.player.getItemCooldownManager().getCooldownProgress(shield, 0f);
        if (cd > 0f) return "SHIELD DISABLED";
        if (mc.player.isBlocking()) return "BLOCKING";
        return "SHIELD READY";
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.SHIELDSTATUS.isEnabled() || mc.player == null || mc.options.hudHidden) return;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int alpha = Math.round(2.55f * Modules.SHIELDSTATUS.optOf("tint", 45));
        boolean tintSlots = Modules.SHIELDSTATUS.flagOf("slots", true);
        ItemStack first = null;

        {
            for (int i = 0; i < 9; i++) {
                ItemStack st = mc.player.getInventory().getStack(i);
                if (!st.isOf(Items.SHIELD)) continue;
                if (first == null) first = st;
                if (tintSlots) tint(ctx, w / 2 - 90 + i * 20 + 2, h - 19, stateColor(mc, st), alpha);
            }
            ItemStack off = mc.player.getOffHandStack();
            if (off.isOf(Items.SHIELD)) {
                if (first == null || mc.player.getOffHandStack() == off) first = off;
                boolean left = mc.options.getMainArm().getValue() == Arm.LEFT;
                int ox = left ? w / 2 + 91 + 10 : w / 2 - 91 - 26;
                if (tintSlots) tint(ctx, ox, h - 19, stateColor(mc, off), alpha);
            }
        }

        if (Modules.SHIELDSTATUS.flagOf("text", false) && first != null) {
            String text = stateText(mc, first);
            int color = stateColor(mc, first);
            int tw = mc.textRenderer.getWidth(text);
            int bw = tw + 14;
            int x = w / 2 - bw / 2;
            int y = h / 2 + Modules.SHIELDSTATUS.optOf("dy", 26);
            Draw.rrect(ctx, x, y, x + bw, y + 14, 5, 0x99000000);
            ctx.drawText(mc.textRenderer, text, x + 7, y + 3, color, true);
        }
    }

    private static void tint(DrawContext ctx, int x, int y, int rgb, int alpha) {
        ctx.fill(x, y, x + 16, y + 16, (alpha << 24) | (rgb & 0xFFFFFF));
        // thin bar under the icon so the state is readable even with a faint tint
        ctx.fill(x, y + 14, x + 16, y + 16, 0xFF000000 | (rgb & 0xFFFFFF));
    }
}
