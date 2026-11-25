package dev.pvpmenu.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/** All built-in modules. Every one of them only displays information or changes how things are drawn. */
public final class Modules {
    public static final Module LOW_SHIELD = new LowShieldModule();
    public static final Module LOW_FIRE = new LowFireModule();
    public static final Module SMALL_TOTEM = new SmallTotemModule();
    public static final Module CROSSHAIR = new CrosshairModule();

    // visuals
    public static final Module FULLBRIGHT = ExtraModules.fullbright();
    public static final Module LAVA = ExtraModules.lava();
    public static final Module CLEARWATER = ExtraModules.clearWater();
    public static final Module NOFOG = ExtraModules.noFog();
    public static final Module QUIET_EXPLOSIONS = ExtraModules.quietExplosions();
    public static final Module SMALL_EXPLOSIONS = ExtraModules.smallExplosions();
    public static final Module FEW_TOTEM = ExtraModules.fewTotemParticles();
    public static final Module HIDE_PARTICLES = ExtraModules.hideParticles();
    public static final Module HANDPOS = ExtraModules.handPosition();
    public static final Module NOEQUIP = ExtraModules.noEquip();
    public static final Module FASTSWING = ExtraModules.fastSwing();

    // rebuilt mods and tools
    public static final Module APPLESKIN = ExtraModules.appleSkin();
    public static final Module SHULKER = ExtraModules.shulker();
    public static final Module TNT = ExtraModules.tnt();
    public static final Module SHIELDFIX = ExtraModules.shieldFix();
    public static final Module SHIELDSTATUS = ExtraModules.shieldStatus();
    public static final Module HOTBARKEYS = ExtraModules.hotbarKeys();
    public static final Module FREELOOK = ExtraModules.freelook();
    public static final Module ZOOM = ExtraModules.zoom();
    public static final Module TOGGLES = ExtraModules.toggles();

    public static final List<Module> ALL = List.of(
            new CooldownModule(),
            new ArmorModule(),
            new TotemModule(),
            new InfoModules.Cps(),
            new InfoModules.Keystrokes(),
            new InfoModules.Combo(),
            new InfoModules.Target(),
            new InfoModules.Info(),
            new InfoModules.Items2(),
            new InfoModules.CustomText(),
            new InfoModules.Potions(),
            LOW_SHIELD,
            LOW_FIRE,
            SMALL_TOTEM,
            FULLBRIGHT, LAVA, CLEARWATER, NOFOG, QUIET_EXPLOSIONS, SMALL_EXPLOSIONS, FEW_TOTEM, HIDE_PARTICLES,
            HANDPOS, NOEQUIP, FASTSWING,
            CROSSHAIR,
            APPLESKIN, SHULKER, TNT, SHIELDFIX, SHIELDSTATUS, HOTBARKEYS, FREELOOK, ZOOM, TOGGLES
    );

    private Modules() {}

    public static Module byId(String id) {
        for (Module m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    // ------------------------------------------------------------------ HUD

    static final class CooldownModule extends Module {
        CooldownModule() {
            super("cooldown", "Attack Cooldown", "A bar that shows when your weapon is fully charged.", "HUD", true);
        }

        @Override public int contentHeight(MinecraftClient mc) { return 14; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            float p = Math.max(0f, Math.min(1f, mc.player.getAttackCooldownProgress(0f)));
            ctx.fill(x, y + 2, x + w, y + 10, 0x55FFFFFF);
            ctx.fill(x, y + 2, x + Math.round(w * p), y + 10, p >= 1f ? 0xFF4ADE80 : 0xFFFACC15);
            String s = Math.round(p * 100) + "%";
            ctx.drawText(mc.textRenderer, s, x + (w - mc.textRenderer.getWidth(s)) / 2, y + 2, 0xFFFFFFFF, true);
        }
    }

    static final class ArmorModule extends Module {
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        private static final String[] LEVELS = {"Percent", "Number", "Bar"};

        ArmorModule() {
            super("armor", "Armor Durability", "Icons and remaining durability for each armor piece.", "HUD", true);
        }

        @Override public String[] levels() { return LEVELS; }
        @Override public String levelLabel() { return "Show"; }
        @Override public int defaultLevel() { return 0; }

        @Override public int contentHeight(MinecraftClient mc) { return 27; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int mode = level();
            for (int i = 0; i < SLOTS.length; i++) {
                ItemStack st = mc.player.getEquippedStack(SLOTS[i]);
                int ix = x + i * 26;
                if (st.isEmpty()) {
                    ctx.drawText(mc.textRenderer, "-", ix + 6, y + 4, 0xFF555A6E, true);
                    continue;
                }
                ctx.drawItem(st, ix, y);
                if (!st.isDamageable()) {
                    ctx.drawText(mc.textRenderer, "--", ix + 3, y + 18, 0xFF8A8FA3, true);
                    continue;
                }
                int max = Math.max(1, st.getMaxDamage());
                int left = max - st.getDamage();
                float f = left / (float) max;
                int color = healthColor(f);

                if (mode == 2) {
                    ctx.fill(ix, y + 19, ix + 16, y + 22, 0x66000000);
                    ctx.fill(ix, y + 19, ix + Math.round(16 * f), y + 22, color);
                } else {
                    String txt = mode == 1 ? Integer.toString(left) : Math.round(f * 100) + "%";
                    ctx.drawText(mc.textRenderer, txt, ix + (16 - mc.textRenderer.getWidth(txt)) / 2, y + 18, color, true);
                }
            }
        }
    }

    static final class TotemModule extends Module {
        private static final String[] LEVELS = {"At 0", "<=1", "<=2", "<=3"};

        TotemModule() {
            super("totems", "Totem Count", "How many Totems of Undying you are carrying.", "HUD", true);
        }

        @Override public String[] levels() { return LEVELS; }
        @Override public String levelLabel() { return "Warn"; }
        @Override public int defaultLevel() { return 0; }

        @Override public int contentHeight(MinecraftClient mc) { return 16; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int count = 0;
            PlayerInventory inv = mc.player.getInventory();
            for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
                ItemStack st = inv.getStack(i);
                if (st.isOf(Items.TOTEM_OF_UNDYING)) count += st.getCount();
            }
            ItemStack off = mc.player.getOffHandStack();
            if (off.isOf(Items.TOTEM_OF_UNDYING)) count += off.getCount();

            ctx.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x, y);
            ctx.drawText(mc.textRenderer, "x" + count, x + 20, y + 4, count <= level() ? 0xFFF87171 : 0xFFFFFFFF, true);
        }
    }

    // --------------------------------------------------------------- Visuals

    static final class LowShieldModule extends Module {
        private static final String[] LEVELS = {"Slight", "Medium", "Low", "Lowest"};

        LowShieldModule() {
            super("lowshield", "Lower Shield", "Moves your held shield down so more of the screen is visible.", "Visuals", true);
        }

        @Override public String[] levels() { return LEVELS; }
        @Override public String levelLabel() { return "Drop"; }
    }

    static final class LowFireModule extends Module {
        private static final String[] LEVELS = {"Slight", "Medium", "Low", "Lowest"};

        LowFireModule() {
            super("lowfire", "Lower Fire", "Moves the fire overlay down when you are burning.", "Visuals", true);
        }

        @Override public String[] levels() { return LEVELS; }
        @Override public String levelLabel() { return "Drop"; }
    }

    static final class SmallTotemModule extends Module {
        private static final String[] LEVELS = {"Tiny", "Small", "Medium", "Large"};

        SmallTotemModule() {
            super("smalltotem", "Small Totem Pop", "Replaces the huge totem animation with a smaller one.", "Visuals", true);
        }

        @Override public String[] levels() { return LEVELS; }
        @Override public String levelLabel() { return "Size"; }
    }

    // -------------------------------------------------------------- Crosshair

    static final class CrosshairModule extends Module {
        CrosshairModule() {
            super("crosshair", "Custom Crosshair", "Replaces the vanilla crosshair with your own design.", "Crosshair", false);
        }
    }
}
