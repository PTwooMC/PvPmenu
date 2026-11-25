package dev.pvpmenu.module;

import dev.pvpmenu.Config;
import dev.pvpmenu.Stats;
import dev.pvpmenu.gui.Draw;
import dev.pvpmenu.gui.Themes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.potion.Potion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Information cards. They read what the game already knows and never act for you. */
public final class InfoModules {
    private InfoModules() {}

    static void text(DrawContext ctx, MinecraftClient mc, String s, int x, int y, int color) {
        ctx.drawText(mc.textRenderer, s, x, y, color, true);
    }

    static void centered(DrawContext ctx, MinecraftClient mc, String s, int cx, int y, int color) {
        ctx.drawText(mc.textRenderer, s, cx - mc.textRenderer.getWidth(s) / 2, y, color, true);
    }

    // ------------------------------------------------------------------ CPS

    static final class Cps extends Module {
        Cps() {
            super("cps", "CPS Counter", "Clicks per second, with your average and peak.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            return List.of(
                    Opt.choice("Buttons", "buttons", 2, "Left", "Right", "Both"),
                    Opt.onOff("Average and peak", "avg", true));
        }

        @Override public int contentHeight(MinecraftClient mc) { return 14 + (flag("avg", true) ? 12 : 0); }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int b = opt("buttons", 2);
            int accent = Themes.accA();
            if (b == 2) {
                text(ctx, mc, "L", x, y + 2, 0xFF8A8FA3);
                text(ctx, mc, String.valueOf(Stats.cps(true)), x + 10, y + 2, 0xFFFFFFFF);
                text(ctx, mc, "R", x + w / 2, y + 2, 0xFF8A8FA3);
                text(ctx, mc, String.valueOf(Stats.cps(false)), x + w / 2 + 10, y + 2, 0xFFFFFFFF);
            } else {
                boolean left = b == 0;
                text(ctx, mc, (left ? "Left " : "Right ") + Stats.cps(left) + " CPS", x, y + 2, 0xFFFFFFFF);
            }
            if (flag("avg", true)) {
                String s = String.format(Locale.ROOT, "avg %.1f   peak %.0f", Stats.averageCps(), Stats.peakCps());
                text(ctx, mc, s, x, y + 14, accent);
            }
        }
    }

    // ------------------------------------------------------------ keystrokes

    static final class Keystrokes extends Module {
        Keystrokes() {
            super("keystrokes", "Keystrokes", "Shows which movement keys and mouse buttons you are pressing.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            return List.of(
                    Opt.onOff("Mouse buttons", "mouse", true),
                    Opt.onOff("Space bar", "space", true),
                    Opt.onOff("CPS inside mouse buttons", "cps", true),
                    Opt.color("Pressed color", "press", 0xFFFFFFFF));
        }

        @Override
        public int contentHeight(MinecraftClient mc) {
            int h = 2 * 25;
            if (flag("mouse", true)) h += 25;
            if (flag("space", true)) h += 17;
            return h;
        }

        private void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h, String label, boolean down) {
            int press = opt("press", 0xFFFFFFFF) | 0xFF000000;
            Draw.rrect(ctx, x, y, x + w, y + h, 3, down ? Draw.withAlpha(press, 0xC0) : 0x66000000);
            centered(ctx, mc, label, x + w / 2, y + (h - 8) / 2, down ? 0xFF111111 : 0xFFFFFFFF);
        }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int k = 24;
            int gap = 2;
            int total = k * 3 + gap * 2;
            int x0 = x + (w - total) / 2;
            var o = mc.options;
            key(ctx, mc, x0 + k + gap, y, k, k, "W", o.forwardKey.isPressed());
            int y2 = y + k + gap;
            key(ctx, mc, x0, y2, k, k, "A", o.leftKey.isPressed());
            key(ctx, mc, x0 + k + gap, y2, k, k, "S", o.backKey.isPressed());
            key(ctx, mc, x0 + 2 * (k + gap), y2, k, k, "D", o.rightKey.isPressed());
            int y3 = y2 + k + gap;
            if (flag("mouse", true)) {
                int bw = (total - gap) / 2;
                boolean c = flag("cps", true);
                key(ctx, mc, x0, y3, bw, k, c ? "LMB " + Stats.cps(true) : "LMB", Stats.leftDown());
                key(ctx, mc, x0 + bw + gap, y3, bw, k, c ? "RMB " + Stats.cps(false) : "RMB", Stats.rightDown());
                y3 += k + gap;
            }
            if (flag("space", true)) {
                key(ctx, mc, x0, y3, total, 14, "SPACE", o.jumpKey.isPressed());
            }
        }
    }

    // ----------------------------------------------------------------- combo

    static final class Combo extends Module {
        Combo() {
            super("combo", "Combo Counter", "Hits in a row without being hit back.", "HUD", false);
        }

        @Override public int contentHeight(MinecraftClient mc) { return 14; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int c = Stats.combo();
            text(ctx, mc, "Combo " + c, x, y + 2, c > 0 ? 0xFFFFFFFF : 0xFF8A8FA3);
            String best = "best " + Stats.bestCombo();
            text(ctx, mc, best, x + w - mc.textRenderer.getWidth(best), y + 2, Themes.accA());
        }
    }

    // ---------------------------------------------------------------- tokens

    /** Replaces {fps}, {ping}, {cps} and friends in the user's own text. */
    static String tokens(String in, MinecraftClient mc) {
        if (in == null || in.indexOf('{') < 0 || mc.player == null) return in == null ? "" : in;
        String out = in;
        out = out.replace("{fps}", String.valueOf(mc.getCurrentFps()));
        out = out.replace("{ping}", ping(mc));
        out = out.replace("{cps}", Stats.cps(true) + "|" + Stats.cps(false));
        out = out.replace("{lcps}", String.valueOf(Stats.cps(true)));
        out = out.replace("{rcps}", String.valueOf(Stats.cps(false)));
        out = out.replace("{x}", String.format(Locale.ROOT, "%.0f", mc.player.getX()));
        out = out.replace("{y}", String.format(Locale.ROOT, "%.0f", mc.player.getY()));
        out = out.replace("{z}", String.format(Locale.ROOT, "%.0f", mc.player.getZ()));
        out = out.replace("{health}", String.format(Locale.ROOT, "%.1f", mc.player.getHealth()));
        out = out.replace("{food}", String.valueOf(mc.player.getHungerManager().getFoodLevel()));
        out = out.replace("{armor}", String.valueOf(mc.player.getArmor()));
        out = out.replace("{speed}", String.format(Locale.ROOT, "%.1f", Stats.speed()));
        out = out.replace("{combo}", String.valueOf(Stats.combo()));
        out = out.replace("{totems}", String.valueOf(count(mc, Items.TOTEM_OF_UNDYING)));
        out = out.replace("{time}", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        LivingEntity t = Stats.target(mc);
        out = out.replace("{target}", t == null ? "-" : t.getName().getString());
        return out;
    }

    static String ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() != null && mc.player != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (e != null) return e.getLatency() + " ms";
        }
        return "-";
    }

    // ------------------------------------------------------------ custom text

    static final class CustomText extends Module {
        CustomText() {
            super("customtext", "Custom Text", "Your own text lines. Use {fps} {ping} {cps} {x} {y} {z} {health} {food} {armor} {speed} {combo} {totems} {time} {target}.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            return List.of(
                    Opt.text("Line 1", "l1", "FPS {fps}  Ping {ping}"),
                    Opt.text("Line 2", "l2", ""),
                    Opt.text("Line 3", "l3", ""),
                    Opt.text("Line 4", "l4", ""),
                    Opt.color("Text color", "color", 0xFFFFFFFF),
                    Opt.onOff("Center text", "center", false));
        }

        private List<String> lines(MinecraftClient mc) {
            List<String> l = new ArrayList<>();
            for (String k : new String[]{"l1", "l2", "l3", "l4"}) {
                String t = str(k, k.equals("l1") ? "FPS {fps}  Ping {ping}" : "");
                if (!t.isBlank()) l.add(tokens(t, mc));
            }
            return l;
        }

        @Override public int contentHeight(MinecraftClient mc) { return Math.max(1, lines(mc).size()) * 10; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int col = opt("color", 0xFFFFFFFF) | 0xFF000000;
            int yy = y;
            for (String s : lines(mc)) {
                String t = mc.textRenderer.trimToWidth(s, w);
                if (flag("center", false)) centered(ctx, mc, t, x + w / 2, yy, col);
                else text(ctx, mc, t, x, yy, col);
                yy += 10;
            }
        }
    }

    // ---------------------------------------------------------------- target

    static final class Target extends Module {
        Target() {
            super("target", "Target Info", "Name, health, armor and more about whoever you are fighting.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            return List.of(
                    Opt.onOff("Hide when nobody is targeted", "hide", true),
                    Opt.onOff("Name", "name", true),
                    Opt.color("Name color", "namecol", 0xFFFFFFFF),
                    Opt.onOff("Health bar", "bar", true),
                    Opt.slider("Bar height", "barh", 3, 12, 6, " px"),
                    Opt.choice("Bar color", "barmode", 0, "By health", "Custom"),
                    Opt.color("Custom bar color", "barcol", 0xFF4ADE80),
                    Opt.choice("Health text", "htext", 0, "Number", "Hearts", "Percent", "Off"),
                    Opt.onOff("Armor points", "armor", true),
                    Opt.onOff("Absorption (gold hearts)", "abs", true),
                    Opt.onOff("Shield raised warning", "shield", true),
                    Opt.onOff("Distance", "dist", true),
                    Opt.onOff("Held item", "held", false),
                    Opt.color("Text color", "textcol", 0xFFE5E7EB));
        }

        @Override
        public boolean visible(MinecraftClient mc) {
            return !flag("hide", true) || Stats.target(mc) != null;
        }

        @Override
        public int contentHeight(MinecraftClient mc) {
            int h = 0;
            if (flag("name", true)) h += 12;
            if (flag("bar", true)) h += opt("barh", 6) + 3;
            if (flag("abs", true)) h += 0;
            h += 11;
            if (flag("held", false)) h += 11;
            return Math.max(12, h);
        }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            LivingEntity t = Stats.target(mc);
            if (t == null) {
                text(ctx, mc, "No target", x, y + 2, 0xFF8A8FA3);
                return;
            }
            int tc = opt("textcol", 0xFFE5E7EB) | 0xFF000000;
            int yy = y;
            if (flag("name", true)) {
                String name = mc.textRenderer.trimToWidth(t.getName().getString(), w);
                text(ctx, mc, name, x, yy, opt("namecol", 0xFFFFFFFF) | 0xFF000000);
                yy += 12;
            }
            float hp = Math.max(0f, t.getHealth());
            float max = Math.max(1f, t.getMaxHealth());
            float f = Math.min(1f, hp / max);
            if (flag("bar", true)) {
                int bh = opt("barh", 6);
                int col = opt("barmode", 0) == 0 ? healthColor(f) : (opt("barcol", 0xFF4ADE80) | 0xFF000000);
                ctx.fill(x, yy, x + w, yy + bh, 0x66000000);
                ctx.fill(x, yy, x + Math.round(w * f), yy + bh, col);
                float abs = t.getAbsorptionAmount();
                if (abs > 0f && flag("abs", true)) {
                    ctx.fill(x, yy + bh, x + Math.min(w, Math.round(w * abs / max)), yy + bh + 2, 0xFFFACC15);
                }
                yy += bh + 3;
            }
            String hs = switch (opt("htext", 0)) {
                case 0 -> String.format(Locale.ROOT, "%.1f / %.0f", hp, max);
                case 1 -> String.format(Locale.ROOT, "%.1f hearts", hp / 2f);
                case 2 -> Math.round(f * 100f) + "%";
                default -> "";
            };
            if (flag("armor", true)) hs += (hs.isEmpty() ? "" : "  ") + "AR " + t.getArmor();
            if (!hs.isEmpty()) text(ctx, mc, hs, x, yy, tc);
            StringBuilder right = new StringBuilder();
            if (flag("shield", true) && t.isBlocking()) right.append("SHIELD ");
            if (flag("dist", true) && mc.player != null) {
                right.append(String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(t)));
            }
            String r = right.toString().trim();
            if (!r.isEmpty()) {
                text(ctx, mc, r, x + w - mc.textRenderer.getWidth(r), yy, t.isBlocking() && flag("shield", true) ? 0xFFFACC15 : 0xFF8A8FA3);
            }
            yy += 11;
            if (flag("held", false)) {
                ItemStack st = t.getMainHandStack();
                String hn = st.isEmpty() ? "empty hand" : st.getName().getString();
                text(ctx, mc, mc.textRenderer.trimToWidth(hn, w), x, yy, 0xFF8A8FA3);
            }
        }
    }

    // ------------------------------------------------------------------ info

    private static final String[][] INFO_LINES = {
            {"fps", "FPS", "FPS", "1"},
            {"ping", "Ping", "Ping", "1"},
            {"xyz", "Coordinates", "XYZ", "1"},
            {"dir", "Direction", "Facing", "1"},
            {"speed", "Speed", "Speed", "0"},
            {"clock", "Clock", "Time", "0"},
            {"mem", "Memory use", "RAM", "0"}
    };

    static final class Info extends Module {
        Info() {
            super("info", "Info", "FPS, ping, coordinates, direction, speed, clock and memory.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            List<Opt> l = new ArrayList<>();
            for (String[] r : INFO_LINES) {
                l.add(Opt.onOff(r[1], r[0], "1".equals(r[3])));
                l.add(Opt.text(r[1] + " label", r[0] + "lbl", r[2]));
            }
            l.add(Opt.choice("Coordinate decimals", "dec", 0, "0", "1", "2"));
            l.add(Opt.onOff("Show labels", "labels", true));
            l.add(Opt.color("Label color", "lblcol", 0xFF8A8FA3));
            l.add(Opt.color("Value color", "valcol", 0xFFE5E7EB));
            return l;
        }

        private String value(MinecraftClient mc, String id) {
            if (mc.player == null) return "";
            switch (id) {
                case "fps": return String.valueOf(mc.getCurrentFps());
                case "ping": {
                    if (mc.getNetworkHandler() != null) {
                        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                        if (e != null) return e.getLatency() + " ms";
                    }
                    return "-";
                }
                case "xyz": {
                    String f = "%." + opt("dec", 0) + "f";
                    return String.format(Locale.ROOT, f + " " + f + " " + f, mc.player.getX(), mc.player.getY(), mc.player.getZ());
                }
                case "dir": {
                    float yaw = mc.player.getYaw();
                    String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
                    int i = Math.floorMod(Math.round(yaw / 45f), 8);
                    float shown = ((yaw % 360f) + 360f) % 360f;
                    return String.format(Locale.ROOT, "%s %.0f", names[i], shown);
                }
                case "speed": return String.format(Locale.ROOT, "%.1f b/s", Stats.speed());
                case "clock": return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                default: {
                    Runtime rt = Runtime.getRuntime();
                    return ((rt.totalMemory() - rt.freeMemory()) / 1048576L) + " MB";
                }
            }
        }

        private List<String[]> lines(MinecraftClient mc) {
            List<String[]> l = new ArrayList<>();
            for (String[] r : INFO_LINES) {
                if (flag(r[0], "1".equals(r[3]))) l.add(new String[]{strOf(r[0] + "lbl", r[2]), value(mc, r[0])});
            }
            return l;
        }

        @Override public int contentHeight(MinecraftClient mc) { return Math.max(1, lines(mc).size()) * 10; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            int lc = opt("lblcol", 0xFF8A8FA3) | 0xFF000000;
            int vc = opt("valcol", 0xFFE5E7EB) | 0xFF000000;
            boolean labels = flag("labels", true);
            int yy = y;
            for (String[] r : lines(mc)) {
                int vx = x;
                if (labels && !r[0].isEmpty()) {
                    text(ctx, mc, r[0], x, yy, lc);
                    vx = x + mc.textRenderer.getWidth(r[0]) + 5;
                }
                text(ctx, mc, r[1], vx, yy, vc);
                yy += 10;
            }
        }
    }

    // ----------------------------------------------------------------- items

    public static final String DEFAULT_ITEMS = "minecraft:golden_apple,minecraft:ender_pearl,minecraft:end_crystal";

    public static List<String> csv(String s) {
        List<String> l = new ArrayList<>();
        if (s == null) return l;
        for (String p : s.split(",")) if (!p.isBlank()) l.add(p.trim());
        return l;
    }

    static int count(MinecraftClient mc, Item item) {
        int n = 0;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < PlayerInventory.MAIN_SIZE; i++) {
            ItemStack st = inv.getStack(i);
            if (st.isOf(item)) n += st.getCount();
        }
        ItemStack off = mc.player.getOffHandStack();
        if (off.isOf(item)) n += off.getCount();
        return n;
    }

    /** The id of the potion type inside a potion item (for example "swiftness"), or null. */
    public static String potionId(ItemStack st) {
        if (st == null || st.isEmpty()) return null;
        if (!(st.isOf(Items.POTION) || st.isOf(Items.SPLASH_POTION) || st.isOf(Items.LINGERING_POTION))) return null;
        PotionContentsComponent pc = st.get(DataComponentTypes.POTION_CONTENTS);
        if (pc == null) return null;
        return pc.potion().flatMap(RegistryEntry::getKey).map(k -> k.getValue().getPath()).orElse(null);
    }

    static int potionCount(MinecraftClient mc, String id, int kind) {
        int n = 0;
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i <= PlayerInventory.MAIN_SIZE; i++) {
            ItemStack st = i < PlayerInventory.MAIN_SIZE ? inv.getStack(i) : mc.player.getOffHandStack();
            if (!id.equals(potionId(st))) continue;
            boolean ok = kind == 0
                    || (kind == 1 && st.isOf(Items.POTION))
                    || (kind == 2 && st.isOf(Items.SPLASH_POTION))
                    || (kind == 3 && st.isOf(Items.LINGERING_POTION));
            if (ok) n += st.getCount();
        }
        return n;
    }

    static final class Items2 extends Module {
        Items2() {
            super("itemcount", "Item Counters", "How many of the items and potions you pick are in your inventory.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            return List.of(
                    Opt.choice("Columns", "cols", 2, "2", "3", "4", "5", "6"),
                    Opt.choice("Potion type", "ptype", 0, "Any", "Drinkable", "Splash", "Lingering"),
                    Opt.onOff("Hide items you have none of", "hide0", false),
                    Opt.color("Count color", "col", 0xFFFFFFFF));
        }

        private int cols() { return opt("cols", 2) + 2; }

        private List<ItemStack> stacks(MinecraftClient mc, List<Integer> counts) {
            List<ItemStack> out = new ArrayList<>();
            boolean hide0 = flag("hide0", false);
            for (String id : csv(Config.getStr(optKey("items"), DEFAULT_ITEMS))) {
                try {
                    Item it = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of(id));
                    if (it == Items.AIR) continue;
                    int n = count(mc, it);
                    if (hide0 && n == 0) continue;
                    out.add(new ItemStack(it));
                    counts.add(n);
                } catch (Exception ignored) {
                }
            }
            for (String id : csv(Config.getStr(optKey("potions"), ""))) {
                int n = potionCount(mc, id, opt("ptype", 0));
                if (hide0 && n == 0) continue;
                ItemStack st = Pick.potionStack(id);
                if (st == null) continue;
                out.add(st);
                counts.add(n);
            }
            return out;
        }

        @Override
        public int contentHeight(MinecraftClient mc) {
            int n = csv(Config.getStr(optKey("items"), DEFAULT_ITEMS)).size() + csv(Config.getStr(optKey("potions"), "")).size();
            return Math.max(1, (n + cols() - 1) / cols()) * 28;
        }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            List<Integer> counts = new ArrayList<>();
            List<ItemStack> l = stacks(mc, counts);
            int cols = cols();
            int step = Math.min(26, w / cols);
            int c = opt("col", 0xFFFFFFFF) | 0xFF000000;
            for (int i = 0; i < l.size(); i++) {
                int cx = x + (i % cols) * step;
                int cy = y + (i / cols) * 28;
                ctx.drawItem(l.get(i), cx, cy);
                int n = counts.get(i);
                centered(ctx, mc, String.valueOf(n), cx + 8, cy + 18, n == 0 ? 0xFFF87171 : c);
            }
        }
    }

    /** Helper for building potion icons by potion id. */
    public static final class Pick {
        private Pick() {}

        public static ItemStack potionStack(String id) {
            try {
                var reg = net.minecraft.registry.Registries.POTION;
                var entry = reg.getEntry(net.minecraft.util.Identifier.of(id.contains(":") ? id : "minecraft:" + id));
                if (entry.isEmpty()) return null;
                return PotionContentsComponent.createStack(Items.POTION, entry.get());
            } catch (Exception e) {
                return null;
            }
        }
    }

    // --------------------------------------------------------------- potions

    static final class Potions extends Module {
        Potions() {
            super("potions", "Potion Timers", "Time left on each potion effect, and the potions you carry.", "HUD", false);
        }

        @Override
        public List<Opt> options() {
            return List.of(
                    Opt.onOff("Potions in inventory", "inv", true),
                    Opt.onOff("Other active effects", "other", true),
                    Opt.onOff("Show icons", "icons", true),
                    Opt.slider("Warn under (seconds)", "warn", 3, 60, 10, " s"));
        }

        private static String pretty(String id) {
            StringBuilder sb = new StringBuilder();
            for (String p : id.split("_")) {
                if (p.isEmpty()) continue;
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
            return sb.toString();
        }

        /** One row: label, count (or -1) and remaining ticks (or -1). */
        private record Row(ItemStack icon, String label, int count, int ticks, boolean inf) {}

        private List<Row> rows(MinecraftClient mc) {
            List<Row> out = new ArrayList<>();
            java.util.Set<String> seenEffects = new java.util.HashSet<>();
            if (flag("inv", true)) {
                java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
                java.util.Map<String, ItemStack> icons = new java.util.LinkedHashMap<>();
                PlayerInventory inv = mc.player.getInventory();
                for (int i = 0; i <= PlayerInventory.MAIN_SIZE; i++) {
                    ItemStack st = i < PlayerInventory.MAIN_SIZE ? inv.getStack(i) : mc.player.getOffHandStack();
                    String id = potionId(st);
                    if (id == null) continue;
                    counts.merge(id, st.getCount(), Integer::sum);
                    icons.putIfAbsent(id, st);
                }
                for (var e : counts.entrySet()) {
                    int ticks = -1;
                    boolean inf = false;
                    var entry = net.minecraft.registry.Registries.POTION.getEntry(net.minecraft.util.Identifier.of("minecraft", e.getKey()));
                    if (entry.isPresent()) {
                        for (StatusEffectInstance eff : entry.get().value().getEffects()) {
                            StatusEffectInstance active = mc.player.getStatusEffect(eff.getEffectType());
                            if (active != null) {
                                ticks = Math.max(ticks, active.getDuration());
                                inf |= active.isInfinite();
                                seenEffects.add(eff.getEffectType().getIdAsString());
                            }
                        }
                    }
                    out.add(new Row(icons.get(e.getKey()), pretty(e.getKey()), e.getValue(), ticks, inf));
                }
            }
            if (flag("other", true)) {
                for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                    if (seenEffects.contains(e.getEffectType().getIdAsString())) continue;
                    String name = e.getEffectType().value().getName().getString();
                    int amp = e.getAmplifier() + 1;
                    if (amp > 1) name += " " + amp;
                    out.add(new Row(null, name, -1, e.getDuration(), e.isInfinite()));
                }
            }
            if (out.size() > 8) return out.subList(0, 8);
            return out;
        }

        @Override public int contentHeight(MinecraftClient mc) { return Math.max(1, rows(mc).size()) * 12; }

        @Override
        public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {
            List<Row> l = rows(mc);
            if (l.isEmpty()) {
                text(ctx, mc, "No potions", x, y, 0xFF8A8FA3);
                return;
            }
            boolean icons = flag("icons", true);
            int warn = opt("warn", 10);
            int yy = y;
            for (Row r : l) {
                int tx = x;
                if (icons && r.icon != null) {
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().translate((float) x, (float) (yy - 2));
                    ctx.getMatrices().scale(0.75f, 0.75f);
                    ctx.drawItem(r.icon, 0, 0);
                    ctx.getMatrices().popMatrix();
                    tx = x + 13;
                } else if (icons) {
                    tx = x + 13;
                }
                String right;
                int color = 0xFFFFFFFF;
                if (r.inf) {
                    right = "inf";
                } else if (r.ticks >= 0) {
                    int secs = r.ticks / 20;
                    right = String.format(Locale.ROOT, "%d:%02d", secs / 60, secs % 60);
                    if (secs < warn) color = 0xFFF87171;
                } else {
                    right = "";
                }
                if (r.count >= 0) right = (right.isEmpty() ? "" : right + "  ") + "x" + r.count;
                int rw = mc.textRenderer.getWidth(right);
                text(ctx, mc, mc.textRenderer.trimToWidth(r.label, Math.max(10, w - (tx - x) - rw - 4)), tx, yy, 0xFFE5E7EB);
                text(ctx, mc, right, x + w - rw, yy, color);
                yy += 12;
            }
        }
    }
}
