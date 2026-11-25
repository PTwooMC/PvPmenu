package dev.pvpmenu.feature;

import dev.pvpmenu.Guard;
import dev.pvpmenu.gui.Draw;
import dev.pvpmenu.gui.Themes;
import dev.pvpmenu.mixin.HandledScreenAccessor;
import dev.pvpmenu.module.Modules;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Everything that draws on top of inventory-style screens: shulker/ender chest preview and hotbar key labels. */
public final class ScreenFeatures {
    private static int frame;

    private ScreenFeatures() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof HandledScreen<?> hs)) return;
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) ->
                    Guard.run("screens", () -> render(client, hs, ctx, mx, my)));
        });
    }

    private static void render(MinecraftClient mc, HandledScreen<?> hs, DrawContext ctx, int mx, int my) {
        frame++;
        HandledScreenAccessor acc = (HandledScreenAccessor) hs;

        // remember the ender chest while it is open
        if (hs instanceof GenericContainerScreen gcs && frame % 10 == 0 && isEnderChest(gcs)) {
            GenericContainerScreenHandler handler = gcs.getScreenHandler();
            if (handler.getRows() == 3) {
                List<ItemStack> l = new ArrayList<>();
                for (int i = 0; i < 27; i++) l.add(handler.getSlot(i).getStack());
                EnderCache.store(mc, l);
            }
        }

        if (Modules.HOTBARKEYS.isEnabled() && Modules.HOTBARKEYS.flagOf("inventory", true)) {
            drawHotbarLabels(mc, hs, ctx, acc);
        }
        if (Modules.SHULKER.isEnabled()) {
            drawPreview(mc, hs, ctx, mx, my, acc);
        }
    }

    private static boolean isEnderChest(Screen s) {
        return s.getTitle().getString().equals(Blocks.ENDER_CHEST.getName().getString());
    }

    // ------------------------------------------------------------ hotbar labels

    private static void drawHotbarLabels(MinecraftClient mc, HandledScreen<?> hs, DrawContext ctx, HandledScreenAccessor acc) {
        int color = Modules.HOTBARKEYS.optOf("color", 0xFFFFFFFF) | 0xFF000000;
        for (Slot s : hs.getScreenHandler().slots) {
            if (!(s.inventory instanceof net.minecraft.entity.player.PlayerInventory)) continue;
            int idx = s.getIndex();
            if (idx < 0 || idx > 8) continue;
            String label = mc.options.hotbarKeys[idx].getBoundKeyLocalizedText().getString();
            HotbarKeys.drawLabel(ctx, mc, label, acc.pvpmenu$getX() + s.x, acc.pvpmenu$getY() + s.y, color);
        }
    }

    // ------------------------------------------------------------------ preview

    private static List<ItemStack> shulkerContents(ItemStack st) {
        if (!(st.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof ShulkerBoxBlock)) return null;
        DefaultedList<ItemStack> l = DefaultedList.ofSize(27, ItemStack.EMPTY);
        ContainerComponent c = st.get(DataComponentTypes.CONTAINER);
        if (c != null) c.copyTo(l);
        return l;
    }

    private static void drawPreview(MinecraftClient mc, HandledScreen<?> hs, DrawContext ctx, int mx, int my, HandledScreenAccessor acc) {
        Slot slot = acc.pvpmenu$getFocusedSlot();
        if (slot == null || !slot.hasStack()) return;
        if (Modules.SHULKER.flagOf("shift", false)
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_PRESS
                && GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) != GLFW.GLFW_PRESS) {
            return;
        }
        ItemStack st = slot.getStack();
        List<ItemStack> contents = shulkerContents(st);
        String title = st.getName().getString();
        boolean ender = false;
        if (contents == null && st.isOf(Items.ENDER_CHEST) && Modules.SHULKER.flagOf("ender", true)) {
            ender = true;
            contents = EnderCache.get(mc);
            title = "Ender Chest (last opened)";
        }
        if (contents == null && !ender) return;

        int cell = 18;
        int pw = cell * 9 + 10;
        int ph = cell * 3 + 22;
        int sw = hs.width;
        int sh = hs.height;
        int px = mx - pw - 10;
        if (px < 4) px = Math.min(sw - pw - 4, mx + 14);
        int py = Math.max(4, Math.min(sh - ph - 4, my - ph / 2));

        int accent = Themes.accA();
        Draw.rrect(ctx, px, py, px + pw, py + ph, 5, 0xF0101218);
        ctx.fill(px + 5, py + 1, px + pw - 5, py + 2, Draw.withAlpha(accent, 0xA0));
        ctx.drawText(mc.textRenderer, mc.textRenderer.trimToWidth(title, pw - 10), px + 5, py + 5, 0xFFE5E7EB, true);

        if (contents == null) {
            ctx.drawText(mc.textRenderer, "Open your ender chest once to fill this.", px + 5, py + 24, 0xFF8A8FA3, true);
            return;
        }
        for (int i = 0; i < 27 && i < contents.size(); i++) {
            int cx = px + 5 + (i % 9) * cell;
            int cy = py + 17 + (i / 9) * cell;
            ctx.fill(cx, cy, cx + cell - 1, cy + cell - 1, 0x33FFFFFF);
            ItemStack it = contents.get(i);
            if (it.isEmpty()) continue;
            ctx.drawItem(it, cx + 1, cy + 1);
            if (it.getCount() > 1) {
                String n = String.valueOf(it.getCount());
                ctx.drawText(mc.textRenderer, n, cx + cell - 1 - mc.textRenderer.getWidth(n), cy + 9, 0xFFFFFFFF, true);
            }
        }
    }
}
