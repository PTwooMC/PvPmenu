package dev.pvpmenu.feature;

import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.Locale;

/**
 * Food information on top of the vanilla hunger bar: saturation, a preview of what the food in your hand
 * restores, and the numbers. Drawn with plain shapes so it does not depend on any game textures.
 */
public final class AppleSkin {
    private AppleSkin() {}

    public static FoodComponent foodOf(ItemStack st) {
        return st == null || st.isEmpty() ? null : st.get(DataComponentTypes.FOOD);
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (!Modules.APPLESKIN.isEnabled() || mc.player == null || mc.options.hudHidden) return;
        if (mc.interactionManager == null || !mc.interactionManager.hasStatusBars()) return;
        if (mc.player.hasVehicle()) return;

        HungerManager hm = mc.player.getHungerManager();
        int food = hm.getFoodLevel();
        float sat = hm.getSaturationLevel();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int right = w / 2 + 91;
        int top = h - 39;

        // what the held food would give you
        FoodComponent held = foodOf(mc.player.getMainHandStack());
        if (held == null) held = foodOf(mc.player.getOffHandStack());
        int gain = held == null ? 0 : held.nutrition();
        float satGain = held == null ? 0f : held.saturation();
        int newFood = Math.min(20, food + gain);

        if (Modules.APPLESKIN.flagOf("saturation", true)) {
            // saturation line above the icons, with the preview in a lighter tone
            int full = 81;
            int x0 = right - full;
            ctx.fill(x0, top - 4, right, top - 2, 0x55000000);
            int s1 = Math.round(full * Math.min(1f, sat / 20f));
            ctx.fill(right - s1, top - 4, right, top - 2, 0xFFFACC15);
            if (gain > 0) {
                float newSat = Math.min(newFood, sat + satGain);
                int s2 = Math.round(full * Math.min(1f, newSat / 20f));
                if (s2 > s1) ctx.fill(right - s2, top - 4, right - s1, top - 2, 0x88FDE68A);
            }
        }

        if (gain > 0 && newFood > food && Modules.APPLESKIN.flagOf("preview", true)) {
            float pulse = (float) (Math.sin(Util.getMeasuringTimeMs() / 160.0) * 0.5 + 0.5);
            int alpha = 70 + Math.round(90 * pulse);
            int color = (alpha << 24) | 0xFFB347;
            for (int icon = food / 2; icon * 2 < newFood; icon++) {
                int x = right - icon * 8 - 9;
                boolean whole = icon * 2 >= food && icon * 2 + 2 <= newFood;
                ctx.fill(whole ? x : x + 4, top, x + 9, top + 9, color);
            }
        }

        if (Modules.APPLESKIN.flagOf("numbers", true)) {
            String s = String.format(Locale.ROOT, "Food %d   Sat %.1f", food, sat);
            if (gain > 0) s += String.format(Locale.ROOT, "   +%d / +%.1f", gain, satGain);
            int tw = mc.textRenderer.getWidth(s);
            ctx.drawText(mc.textRenderer, s, right - tw, top - 14, gain > 0 ? 0xFFFDE68A : 0xFFE5E7EB, true);
        }
    }

    /** Adds "Hunger +6, Saturation +7.2" to the tooltip of food items. */
    public static void tooltip(ItemStack stack, java.util.List<Text> lines) {
        if (!Modules.APPLESKIN.isEnabled() || !Modules.APPLESKIN.flagOf("tooltip", true)) return;
        FoodComponent f = foodOf(stack);
        if (f == null) return;
        lines.add(Text.literal(String.format(Locale.ROOT, "Hunger +%d   Saturation +%.1f", f.nutrition(), f.saturation()))
                .formatted(Formatting.GOLD));
    }
}
