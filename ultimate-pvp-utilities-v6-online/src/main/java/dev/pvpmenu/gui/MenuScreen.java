package dev.pvpmenu.gui;

import dev.pvpmenu.Config;
import dev.pvpmenu.CrosshairRenderer;
import dev.pvpmenu.DupeDetect;
import dev.pvpmenu.Guard;
import dev.pvpmenu.HudRenderer;
import dev.pvpmenu.Presets;
import dev.pvpmenu.PvpMenuClient;
import dev.pvpmenu.module.Module;
import dev.pvpmenu.module.Opt;
import dev.pvpmenu.module.Modules;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * The overlay menu. A glowing panel fades in over a dimmed backdrop. The sidebar switches between tabs and
 * every tab is a scrollable list of entries: module cards, sections, option chips, sliders, colour pickers,
 * buttons, the crosshair previews and the pixel editor. A search box at the top finds any setting.
 */
public class MenuScreen extends Screen {
    private static final String[] TABS = {"Home", "HUD", "Visuals", "Crosshair", "Utilities", "Warm-up", "Presets", "Settings", "Guide"};
    private static final int TAB_STEP = 30;
    private static final net.minecraft.util.Identifier ICON = net.minecraft.util.Identifier.of("pvpmenu", "textures/gui/icon.png");
    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};
    private static final int[] SCALES = {75, 100, 125, 150};
    private static final String[] SCALE_LABELS = {"75%", "100%", "125%", "150%"};
    private static final int[] GRIDS = {0, 4, 8, 16};
    private static final String[] GRID_LABELS = {"Off", "4 px", "8 px", "16 px"};
    private static final String[] OFF_ON = {"Off", "On"};
    private static final String[] LOOK_HINTS = {
            "", "",
            "Shown for a moment after you hit something.",
            "Shown while your attack is still recharging.",
            "Shown while you are sprinting.",
            "Shown while a bow, crossbow or trident is drawn."
    };

    /** Everything the search box can find: title, tab, keywords. */
    private static final Object[][] INDEX = {
            {"Crosshair style", 3, "shape cross dot circle square pixel diamond"},
            {"Crosshair length (horizontal / vertical)", 3, "arm size length width height"},
            {"Crosshair gap", 3, "gap spacing"},
            {"Crosshair thickness", 3, "thick width"},
            {"Crosshair arms on/off", 3, "arm top bottom left right"},
            {"Crosshair rotation", 3, "rotate angle"},
            {"Crosshair offset (off-centre)", 3, "offset position center centre move"},
            {"Crosshair colour", 3, "color colour hex rainbow"},
            {"Crosshair outline", 3, "border outline"},
            {"Crosshair animation", 3, "pulse spin animate"},
            {"Entity in reach look", 3, "target range hit entity colour"},
            {"Hit marker", 3, "hitmarker hit marker pop"},
            {"Pixel crosshair editor", 3, "draw custom pixel"},
            {"Crosshair share code", 3, "export import share copy"},
            {"Look: on hit, low cooldown, sprinting, bow", 3, "look hit cooldown sprint bow"},
            {"Card background, accent and size", 1, "card color colour opacity size background"},
            {"Edit HUD layout", 1, "drag move position layout grid snap"},
            {"HUD scale and default corner", 1, "scale size corner"},
            {"Theme, accent colours and gradient", 7, "theme accent gradient colour color menu"},
            {"Backdrop dim and animations", 7, "dim background animation"},
            {"Module keybinds and Hide HUD key", 7, "key bind keybind toggle hide"},
            {"My presets (save, load, duplicate)", 6, "preset save load duplicate setup profile"},
            {"Settings share code", 6, "export import share code"},
            {"Crash protection", 7, "crash guard error safe"},
            {"Hand and item position", 2, "hand item position viewmodel offset size"},
            {"Fullbright, lava vision, clear water", 2, "fullbright gamma bright lava water see fog"},
            {"Explosion and totem particles, hide particles", 2, "explosion totem particle quiet loud"},
            {"No item switch animation, faster swing", 2, "animation swing equip 1.8 old"},
            {"AppleSkin food info", 4, "appleskin apple skin food hunger saturation"},
            {"Shulker box and ender chest preview", 4, "shulker ender chest tooltip preview"},
            {"TNT timer", 4, "tnt timer fuse countdown"},
            {"Shield status and shield fixes", 4, "shield status fixes block cooldown sound"},
            {"Hotbar keys labels", 4, "hotbar keys slot number labels"},
            {"Freelook and zoom", 4, "freelook perspective look zoom camera"},
            {"Toggle sprint and sneak", 4, "toggle sprint sneak hold"},
            {"Reaction test", 5, "reaction test warm up click speed"},
            {"Aim trainer", 5, "aim trainer warm up targets levels"},
            {"CPS counter and keystrokes", 1, "cps clicks keystrokes keys wasd mouse"},
            {"Combo, target info, item counters, potions, custom text", 1, "combo target info items count potion timer custom text fps ping coords"}
    };

    private static final int HEADER_H = 46;
    private static final int SIDE_W = 120;
    private static final int SEARCH_H = 18;
    private static final int GAP = 6;
    private static final int PICK_H = 142;

    private static final int TEXT_MAIN = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFF8A8FA3;
    private static final int TEXT_SOFT = 0xFFC7CADA;
    private static final int TEXT_BAD = 0xFFF87171;

    private interface Drag {
        void move(double mx, double my);
    }

    private static final class Hsv {
        final float h, s, v;
        final int rgb;

        Hsv(float h, float s, float v, int rgb) {
            this.h = h;
            this.s = s;
            this.v = v;
            this.rgb = rgb;
        }
    }

    private final long openedAt = Util.getMeasuringTimeMs();
    private long tabAt = openedAt;
    private long lastFrame = openedAt;
    private float dt;
    private float alpha;
    private float ea = 1f; // per-entry fade, used for the staggered tab animation

    private int tab = 1;
    private float indicatorY = -1f;
    private float scroll;
    private float scrollTarget;
    private final Map<String, Float> toggleAnim = new HashMap<>();
    private final Map<String, Float> hoverAnim = new HashMap<>();
    private final CrosshairRenderer.State previewA = new CrosshairRenderer.State();
    private final CrosshairRenderer.State previewB = new CrosshairRenderer.State();

    private Drag drag;
    private String dragKey;

    private String openPicker;
    private final Map<String, Hsv> hsvCache = new HashMap<>();
    private boolean hexSuppress;

    private int xhLook;
    private int cardSel;
    private String confirm;      // a two-click confirmation in progress (delete preset, reset all)
    private String toastText = "";
    private long toastAt = -100000;

    private TextFieldWidget searchField;
    private TextFieldWidget hexField;
    private TextFieldWidget nameField;
    private final TextFieldWidget[] strFields = new TextFieldWidget[6];
    private final String[] strKeys = new String[6];
    private boolean strLoading;
    private int strSlot;
    private static List<Object[]> itemPool;
    private static List<Object[]> potionPool;
    private final Set<TextFieldWidget> shown = new HashSet<>();
    private boolean focusCleared;

    // Layout, recomputed every frame
    private int px, py, pw, ph;   // panel
    private int sx, sy;           // search box
    private int cx, cy, cw, ch;   // content area

    public MenuScreen() {
        super(Text.literal(PvpMenuClient.MOD_NAME));
        for (Module m : Modules.ALL) {
            toggleAnim.put(m.id, m.isEnabled() ? 1f : 0f);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        if (searchField == null) {
            searchField = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Search"));
            searchField.setMaxLength(32);
            searchField.setSuggestion("Search settings...");
            searchField.setChangedListener(s -> {
                searchField.setSuggestion(s.isEmpty() ? "Search settings..." : "");
                scroll = 0;
                scrollTarget = 0;
                tabAt = Util.getMeasuringTimeMs();
            });

            hexField = new TextFieldWidget(textRenderer, 0, 0, 66, 16, Text.literal("Hex"));
            hexField.setMaxLength(7);
            hexField.setChangedListener(this::onHexChanged);

            nameField = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Name"));
            nameField.setMaxLength(24);
            nameField.setText("My setup");

            for (int i = 0; i < strFields.length; i++) {
                final int slot = i;
                TextFieldWidget f = new TextFieldWidget(textRenderer, 0, 0, 100, 16, Text.literal("Text"));
                f.setMaxLength(48);
                f.setChangedListener(t -> {
                    if (!strLoading && strKeys[slot] != null) Config.setStr(strKeys[slot], t);
                });
                strFields[i] = f;
            }
        }
        for (TextFieldWidget f : fields()) {
            if (!children().contains(f)) addSelectableChild(f);
        }
        focusField(null);
    }

    private TextFieldWidget[] fields() {
        TextFieldWidget[] all = new TextFieldWidget[3 + strFields.length];
        all[0] = searchField;
        all[1] = hexField;
        all[2] = nameField;
        System.arraycopy(strFields, 0, all, 3, strFields.length);
        return all;
    }

    private void focusField(TextFieldWidget target) {
        for (TextFieldWidget f : fields()) {
            if (f != null) f.setFocused(f == target);
        }
        setFocused(target);
    }

    private boolean anyFocused() {
        for (TextFieldWidget f : fields()) {
            if (f != null && f.isFocused()) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int a(int argb) {
        int al = Math.round(((argb >>> 24) & 0xFF) * alpha * ea);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private float ease() {
        return 1f - (float) Math.exp(-dt * 16f);
    }

    private boolean animated() {
        return Config.getInt("ui.anim", 1) == 1;
    }

    private void text(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(textRenderer, s, x, y, a(color), false);
    }

    private void toast(String s) {
        toastText = s;
        toastAt = Util.getMeasuringTimeMs();
    }

    private void startDrag(String key, Drag d) {
        drag = d;
        dragKey = key;
    }

    private static float clamp01(double v) {
        return (float) Math.max(0.0, Math.min(1.0, v));
    }

    private int accA() {
        return Themes.accA();
    }

    private int accB() {
        return Themes.accB();
    }

    /** Soft glow: three faint rings around a rounded rectangle. */
    private void glow(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color, int baseAlpha) {
        for (int i = 3; i >= 1; i--) {
            Draw.rrect(ctx, x1 - i, y1 - i, x2 + i, y2 + i, r + i, a(Draw.withAlpha(color, baseAlpha / i)));
        }
    }

    private void chip(DrawContext ctx, int x, int y, int w, int h, String label, boolean selected,
                      int mx, int my, boolean hot, int accent) {
        boolean hov = hot && inside(mx, my, x, y, w, h);
        if (selected) {
            glow(ctx, x, y, x + w, y + h, 4, accent, 0x40);
            Draw.rrect(ctx, x - 1, y - 1, x + w + 1, y + h + 1, 5, a(accent));
            Draw.rrect(ctx, x, y, x + w, y + h, 4, a(Draw.lerp(0xFF14151D, accent, 0.28f)));
        } else {
            Draw.rrect(ctx, x, y, x + w, y + h, 4, a(hov ? 0xFF222538 : 0xFF161820));
        }
        int tw = textRenderer.getWidth(label);
        text(ctx, label, x + (w - tw) / 2, y + (h - 8) / 2, selected ? TEXT_MAIN : (hov ? TEXT_MAIN : TEXT_SOFT));
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean hot, int accent) {
        boolean hov = hot && inside(mx, my, x, y, w, h);
        Draw.rrect(ctx, x, y, x + w, y + h, 4, a(hov ? Draw.lerp(0xFF161820, accent, 0.35f) : 0xFF1B1D29));
        ctx.fill(x + 2, y + h - 1, x + w - 2, y + h, a(Draw.withAlpha(accent, hov ? 0xFF : 0x66)));
        int tw = textRenderer.getWidth(label);
        text(ctx, label, x + (w - tw) / 2, y + (h - 8) / 2, hov ? TEXT_MAIN : TEXT_SOFT);
    }

    private List<String> wrap(String s, int w) {
        List<String> lines = new ArrayList<>();
        String cur = "";
        for (String word : s.split(" ")) {
            String t = cur.isEmpty() ? word : cur + " " + word;
            if (!cur.isEmpty() && textRenderer.getWidth(t) > w) {
                lines.add(cur);
                cur = word;
            } else {
                cur = t;
            }
        }
        if (!cur.isEmpty()) lines.add(cur);
        return lines;
    }

    /** Left-to-right layout of labelled boxes that wraps onto new rows. */
    private final class Flow {
        final int[] xs;
        final int[] rowOf;
        final int[] ws;
        final int rows;

        Flow(String[] labels, int availW) {
            int n = labels.length;
            xs = new int[n];
            rowOf = new int[n];
            ws = new int[n];
            int x = 0;
            int row = 0;
            for (int i = 0; i < n; i++) {
                int w = textRenderer.getWidth(labels[i]) + 18;
                if (x > 0 && x + w > availW) {
                    row++;
                    x = 0;
                }
                xs[i] = x;
                rowOf[i] = row;
                ws[i] = w;
                x += w + 4;
            }
            rows = row + 1;
        }
    }

    private int fw() {
        return cw - 8;
    }

    // ------------------------------------------------------------------ colours

    private Hsv hsvOf(String key, int argb) {
        int rgb = argb & 0xFFFFFF;
        Hsv c = hsvCache.get(key);
        if (c != null && c.rgb == rgb) return c;
        float[] f = Draw.rgbToHsv(argb);
        Hsv n = new Hsv(f[0], f[1], f[2], rgb);
        hsvCache.put(key, n);
        return n;
    }

    private void putColor(String key, float h, float s, float v, int al) {
        int rgb = Draw.hsv(h, s, v) & 0xFFFFFF;
        Config.setIntQuiet(key, (al << 24) | rgb);
        Config.setIntQuiet(key + ".rb", 0);
        hsvCache.put(key, new Hsv(h, s, v, rgb));
    }

    private void onHexChanged(String s) {
        if (hexSuppress || openPicker == null) return;
        String t = s.startsWith("#") ? s.substring(1) : s;
        if (t.length() != 6) return;
        try {
            int rgb = Integer.parseInt(t, 16);
            String key = openPicker;
            int old = Config.getInt(key, 0xFFFFFFFF);
            Config.setIntQuiet(key, (old & 0xFF000000) | rgb);
            Config.setIntQuiet(key + ".rb", 0);
            Config.save();
        } catch (NumberFormatException ignored) {
            // not a valid colour yet
        }
    }

    private void setHexText(String s) {
        hexSuppress = true;
        hexField.setText(s);
        hexSuppress = false;
    }

    // ------------------------------------------------------------------ entries

    private static int moduleHeight(Module m) {
        int h = m.levels() != null ? 68 : 46;
        if (m.note() != null) h += 14;
        if (DupeDetect.find(m) != null) h += 22;
        return h;
    }

    private abstract class Entry {
        int h;

        Entry(int h) { this.h = h; }

        abstract void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent);

        boolean click(double mx, double my, int x, int y, int w) { return false; }
    }

    private final class SectionEntry extends Entry {
        private final String title;

        SectionEntry(String title) {
            super(16);
            this.title = title.toUpperCase();
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            text(ctx, title, x, y + 4, accent);
            int tw = textRenderer.getWidth(title);
            ctx.fill(x + tw + 8, y + 8, x + w, y + 9, a(0x22FFFFFF));
        }
    }

    private final class NoteEntry extends Entry {
        private final List<String> lines;
        private final int color;

        NoteEntry(String body, int color) {
            super(0);
            this.lines = wrap(body, Math.max(60, fw() - 4));
            this.color = color;
            this.h = Math.max(10, lines.size() * 10);
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            for (int i = 0; i < lines.size(); i++) text(ctx, lines.get(i), x, y + i * 10, color);
        }
    }

    /** A module card with an animated switch and, if the module has options, a row of chips. */
    private final class ModuleEntry extends Entry {
        private static final int TOP_H = 46;
        private final Module m;
        private final DupeDetect.Other other;
        private final int base;

        ModuleEntry(Module m) {
            super(moduleHeight(m));
            this.m = m;
            this.other = DupeDetect.find(m);
            this.base = m.levels() != null ? 68 : TOP_H;
        }

        private int noteY() { return base - 4; }

        private int dupeY() { return base - 4 + (m.note() != null ? 14 : 0); }

        private int chipW(int w, int n) {
            return Math.min(56, (w - 14 - 46 - 14 - (n - 1) * 4) / n);
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            boolean hov = hot && inside(mx, my, x, y, w, h);
            float hv = hoverAnim.getOrDefault(m.id, 0f);
            hv += ((hov ? 1f : 0f) - hv) * ease();
            hoverAnim.put(m.id, hv);

            float on = toggleAnim.getOrDefault(m.id, 0f);
            on += ((m.isEnabled() ? 1f : 0f) - on) * ease();
            toggleAnim.put(m.id, on);

            Draw.rrect(ctx, x, y, x + w, y + h, 5, a(Draw.lerp(0xFF161821, 0xFF1E2131, hv)));
            ctx.fill(x + 6, y + 1, x + w - 6, y + 2, a(Draw.withAlpha(0xFFFFFFFF, Math.round(10 + 14 * hv))));
            if (on > 0.01f) {
                Draw.rrect(ctx, x, y + 8, x + 3, y + h - 8, 1, a(Draw.withAlpha(accent, Math.round(255 * on))));
            }

            text(ctx, m.name, x + 14, y + 10, TEXT_MAIN);
            boolean bad = Guard.tripped(m.id);
            String sub = bad ? "Turned off after errors. Click the switch to try again." : m.description;
            text(ctx, textRenderer.trimToWidth(sub, w - 14 - 66), x + 14, y + 25, bad ? TEXT_BAD : TEXT_DIM);

            int sx2 = x + w - 14 - 34;
            int sy2 = y + (TOP_H - 16) / 2;
            if (on > 0.05f) {
                glow(ctx, sx2, sy2, sx2 + 34, sy2 + 16, 8, accent, Math.round(0x50 * on));
            }
            Draw.rrect(ctx, sx2, sy2, sx2 + 34, sy2 + 16, 8, a(Draw.lerp(0xFF2B2E3D, accent, on)));
            int kx = sx2 + 2 + Math.round(on * 18);
            Draw.rrect(ctx, kx, sy2 + 2, kx + 12, sy2 + 14, 6, a(0xFFFFFFFF));

            String[] lv = m.levels();
            if (lv != null) {
                text(ctx, m.levelLabel().toUpperCase(), x + 14, y + 50, TEXT_DIM);
                int cwid = chipW(w, lv.length);
                for (int i = 0; i < lv.length; i++) {
                    chip(ctx, x + 14 + 46 + i * (cwid + 4), y + 46, cwid, 16, lv[i], i == m.level(), mx, my, hot, accent);
                }
            }
            if (m.note() != null) {
                text(ctx, m.note(), x + 14, y + noteY() + 2, 0xFFE0A458);
            }
            if (other != null) {
                int dy = y + dupeY();
                String msg = "Also installed: " + other.name;
                text(ctx, textRenderer.trimToWidth(msg, 120), x + 14, dy + 6, 0xFFE0A458);
                int bx = x + 14 + Math.min(124, textRenderer.getWidth(msg) + 8);
                chip(ctx, bx, dy + 2, 70, 16, "Turn mine off", false, mx, my, hot, accent);
                if (other.jar != null) {
                    String l = DupeDetect.pending(other) ? "Will be disabled (undo)" : "Disable the other";
                    chip(ctx, bx + 74, dy + 2, textRenderer.getWidth(l) + 14, 16, l, DupeDetect.pending(other), mx, my, hot, accent);
                }
            }
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            String[] lv = m.levels();
            if (lv != null) {
                int cwid = chipW(w, lv.length);
                for (int i = 0; i < lv.length; i++) {
                    if (inside(mx, my, x + 14 + 46 + i * (cwid + 4), y + 46, cwid, 16)) {
                        m.setLevel(i);
                        return true;
                    }
                }
            }
            if (other != null) {
                int dy = y + dupeY();
                String msg = "Also installed: " + other.name;
                int bx = x + 14 + Math.min(124, textRenderer.getWidth(msg) + 8);
                if (inside(mx, my, bx, dy + 2, 70, 16)) {
                    Config.set(m.id, false);
                    return true;
                }
                if (other.jar != null) {
                    String l = DupeDetect.pending(other) ? "Will be disabled (undo)" : "Disable the other";
                    if (inside(mx, my, bx + 74, dy + 2, textRenderer.getWidth(l) + 14, 16)) {
                        DupeDetect.toggleDisableOther(other);
                        toast(DupeDetect.pending(other)
                                ? other.name + " will be renamed to .disabled when you close the game"
                                : "Cancelled");
                        return true;
                    }
                }
            }
            if (inside(mx, my, x, y, w, TOP_H)) {
                m.toggle();
                return true;
            }
            return false;
        }
    }

    /** A label with a wrapping row of selectable chips. */
    private final class ChipEntry extends Entry {
        private final String label;
        private final String[] options;
        private final IntSupplier get;
        private final IntConsumer set;
        private final Flow flow;

        ChipEntry(String label, String[] options, IntSupplier get, IntConsumer set) {
            super(0);
            this.label = label;
            this.options = options;
            this.get = get;
            this.set = set;
            this.flow = new Flow(options, fw());
            this.h = 14 + flow.rows * 20 - 4;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            text(ctx, label.toUpperCase(), x, y, TEXT_DIM);
            int sel = get.getAsInt();
            for (int i = 0; i < options.length; i++) {
                chip(ctx, x + flow.xs[i], y + 14 + flow.rowOf[i] * 20, flow.ws[i], 16, options[i], i == sel, mx, my, hot, accent);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            for (int i = 0; i < options.length; i++) {
                if (inside(mx, my, x + flow.xs[i], y + 14 + flow.rowOf[i] * 20, flow.ws[i], 16)) {
                    set.accept(i);
                    return true;
                }
            }
            return false;
        }
    }

    /** Several independent on/off chips (for example the four crosshair arms). */
    private final class ToggleRowEntry extends Entry {
        private final String label;
        private final String[] names;
        private final String[] keys;
        private final Flow flow;

        ToggleRowEntry(String label, String[] names, String[] keys) {
            super(0);
            this.label = label;
            this.names = names;
            this.keys = keys;
            this.flow = new Flow(names, fw());
            this.h = 14 + flow.rows * 20 - 4;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            text(ctx, label.toUpperCase(), x, y, TEXT_DIM);
            for (int i = 0; i < names.length; i++) {
                boolean on = Config.getInt(keys[i], 1) == 1;
                chip(ctx, x + flow.xs[i], y + 14 + flow.rowOf[i] * 20, flow.ws[i], 16, names[i], on, mx, my, hot, accent);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            for (int i = 0; i < names.length; i++) {
                if (inside(mx, my, x + flow.xs[i], y + 14 + flow.rowOf[i] * 20, flow.ws[i], 16)) {
                    Config.setInt(keys[i], Config.getInt(keys[i], 1) == 1 ? 0 : 1);
                    return true;
                }
            }
            return false;
        }
    }

    /** A row of buttons that each run an action. */
    private final class ButtonEntry extends Entry {
        private final String[] labels;
        private final Runnable[] actions;
        private final Flow flow;

        ButtonEntry(String[] labels, Runnable[] actions) {
            super(0);
            this.labels = labels;
            this.actions = actions;
            this.flow = new Flow(labels, fw());
            this.h = flow.rows * 20 - 4;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            for (int i = 0; i < labels.length; i++) {
                button(ctx, x + flow.xs[i], y + flow.rowOf[i] * 20, flow.ws[i], 16, labels[i], mx, my, hot, accent);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            for (int i = 0; i < labels.length; i++) {
                if (inside(mx, my, x + flow.xs[i], y + flow.rowOf[i] * 20, flow.ws[i], 16)) {
                    actions[i].run();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A draggable slider stored as an int setting. The track position is worked out from the arguments of
     * the click itself, so it keeps working even though entries are rebuilt every frame.
     */
    private final class SliderEntry extends Entry {
        private final String label;
        private final String key;
        private final String suffix;
        private final int min;
        private final int max;
        private final int def;

        SliderEntry(String label, String key, int min, int max, int def, String suffix) {
            super(34);
            this.label = label;
            this.key = key;
            this.min = min;
            this.max = max;
            this.def = def;
            this.suffix = suffix;
        }

        private int value() {
            return Math.max(min, Math.min(max, Config.getInt(key, def)));
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            int tx = x + 6;
            int tw = w - 12;
            int v = value();
            float frac = (v - min) / (float) Math.max(1, max - min);
            boolean active = key.equals(dragKey);
            boolean hov = hot && inside(mx, my, x, y + 10, w, 22);

            text(ctx, label.toUpperCase(), x, y, TEXT_DIM);
            String vs = v + suffix;
            text(ctx, vs, x + w - textRenderer.getWidth(vs), y, TEXT_MAIN);

            int ty = y + 18;
            Draw.rrect(ctx, tx, ty, tx + tw, ty + 6, 3, a(0xFF22243A));
            int kx = tx + Math.round(frac * tw);
            int fillEnd = Math.max(tx + 6, kx);
            Draw.rrect(ctx, tx, ty, fillEnd, ty + 6, 3, a(accA()));
            if (accB() != accA()) {
                Draw.hgrad(ctx, tx + 3, ty + 1, Math.max(tx + 4, fillEnd - 2), ty + 5, a(accA()), a(accB()));
            }
            if (active || hov) {
                Draw.rrect(ctx, kx - 7, ty - 4, kx + 7, ty + 10, 7, a(Draw.withAlpha(accent, active ? 0x66 : 0x3A)));
            }
            Draw.rrect(ctx, kx - 5, ty - 2, kx + 5, ty + 8, 5, a(0xFFFFFFFF));
            Draw.rrect(ctx, kx - 2, ty + 1, kx + 2, ty + 5, 2, a(accent));
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            if (!inside(mx, my, x, y + 10, w, 22)) return false;
            final int tx = x + 6;
            final int tw = Math.max(1, w - 12);
            Drag d = (mx2, my2) -> {
                float f = clamp01((mx2 - tx) / (double) tw);
                int v = min + Math.round(f * (max - min));
                if (v != value()) Config.setIntQuiet(key, v);
            };
            startDrag(key, d);
            d.move(mx, my);
            return true;
        }
    }

    /** A colour setting: swatch, hex code and an expandable picker with hue, saturation, brightness, opacity, favourites. */
    private final class ColorEntry extends Entry {
        private final String label;
        private final String key;
        private final int def;
        private final boolean useAlpha;
        private final boolean useRainbow;
        private final boolean open;

        ColorEntry(String label, String key, int def, boolean useAlpha, boolean useRainbow) {
            super(34);
            this.label = label;
            this.key = key;
            this.def = def;
            this.useAlpha = useAlpha;
            this.useRainbow = useRainbow;
            this.open = key.equals(openPicker);
            if (open) this.h = 36 + PICK_H + 4;
        }

        private int current() {
            int c = Config.getInt(key, def);
            return useAlpha ? c : (c | 0xFF000000);
        }

        private boolean rainbowOn() {
            return useRainbow && Config.getInt(key + ".rb", 0) == 1;
        }

        private int alphaOf() {
            return (current() >>> 24) & 0xFF;
        }

        private int sw(int w) {
            return Math.min(160, w - 70);
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            text(ctx, label.toUpperCase(), x, y, TEXT_DIM);
            int ry = y + 12;
            boolean hov = hot && inside(mx, my, x, ry, w, 18);
            Draw.rrect(ctx, x, ry, x + w, ry + 18, 5, a(hov ? 0xFF1E2131 : 0xFF161821));

            int cur = current();
            Draw.checker(ctx, x + 5, ry + 3, x + 41, ry + 15);
            ctx.fill(x + 5, ry + 3, x + 41, ry + 15, a(rainbowOn() ? Draw.rainbow() : cur));
            ctx.fill(x + 4, ry + 2, x + 42, ry + 3, a(0xAAFFFFFF));
            ctx.fill(x + 4, ry + 15, x + 42, ry + 16, a(0xAAFFFFFF));
            ctx.fill(x + 4, ry + 3, x + 5, ry + 15, a(0xAAFFFFFF));
            ctx.fill(x + 41, ry + 3, x + 42, ry + 15, a(0xAAFFFFFF));

            String info = rainbowOn() ? "Rainbow"
                    : String.format(Locale.ROOT, "#%06X", cur & 0xFFFFFF)
                    + (useAlpha ? "   " + Math.round(alphaOf() / 2.55f) + "%" : "");
            text(ctx, info, x + 50, ry + 5, TEXT_SOFT);
            text(ctx, open ? "^" : "v", x + w - 14, ry + 5, TEXT_DIM);

            if (!open) return;

            final int bx = x;
            final int by = y + 36;
            final int swd = sw(w);
            final int shh = 72;
            Hsv hv = hsvOf(key, cur);

            // Saturation / brightness square
            for (int yy = 0; yy < shh; yy += 3) {
                for (int xx = 0; xx < swd; xx += 3) {
                    int c = Draw.hsv(hv.h, xx / (float) Math.max(1, swd - 1), 1f - yy / (float) (shh - 1));
                    ctx.fill(bx + xx, by + yy, bx + Math.min(swd, xx + 3), by + Math.min(shh, yy + 3), a(c));
                }
            }
            int mkx = bx + Math.round(hv.s * (swd - 1));
            int mky = by + Math.round((1f - hv.v) * (shh - 1));
            ctx.fill(mkx - 4, mky - 4, mkx + 5, mky + 5, a(0xFFFFFFFF));
            ctx.fill(mkx - 3, mky - 3, mkx + 4, mky + 4, a(0xFF000000));
            ctx.fill(mkx - 2, mky - 2, mkx + 3, mky + 3, a(Draw.hsv(hv.h, hv.s, hv.v)));

            // Hue bar
            int hx = bx + swd + 8;
            for (int i = 0; i < shh; i++) {
                ctx.fill(hx, by + i, hx + 10, by + i + 1, a(Draw.hsv(360f * i / (shh - 1), 1f, 1f)));
            }
            int hmy = by + Math.round(hv.h / 360f * (shh - 1));
            ctx.fill(hx - 2, hmy - 1, hx + 12, hmy + 2, a(0xFFFFFFFF));
            ctx.fill(hx - 1, hmy, hx + 11, hmy + 1, a(0xFF000000));

            // Opacity bar
            if (useAlpha) {
                int ax = hx + 16;
                Draw.checker(ctx, ax, by, ax + 10, by + shh);
                int rgb = cur & 0xFFFFFF;
                for (int i = 0; i < shh; i++) {
                    ctx.fill(ax, by + i, ax + 10, by + i + 1, a(Draw.withAlpha(rgb, 255 - Math.round(255f * i / (shh - 1)))));
                }
                int amy = by + Math.round((1f - alphaOf() / 255f) * (shh - 1));
                ctx.fill(ax - 2, amy - 1, ax + 12, amy + 2, a(0xFFFFFFFF));
                ctx.fill(ax - 1, amy, ax + 11, amy + 1, a(0xFF000000));
            }

            // Row 2: hex field, rainbow, save
            int r2 = by + shh + 8;
            hexField.setX(x);
            hexField.setY(r2);
            hexField.setWidth(66);
            if (!hexField.isFocused()) {
                String want = String.format(Locale.ROOT, "%06X", cur & 0xFFFFFF);
                if (!want.equals(hexField.getText())) setHexText(want);
            }
            shown.add(hexField);
            hexField.render(ctx, mx, my, 0f);

            int bxx = x + 74;
            if (useRainbow) {
                chip(ctx, bxx, r2, 56, 16, "Rainbow", rainbowOn(), mx, my, hot, accent);
                bxx += 60;
            }
            button(ctx, bxx, r2, 66, 16, "Save color", mx, my, hot, accent);

            // Row 3: quick colours
            int r3 = r2 + 22;
            for (int i = 0; i < Draw.PALETTE.length; i++) {
                int qx = x + i * 20;
                boolean qh = hot && inside(mx, my, qx, r3, 16, 16);
                Draw.rrect(ctx, qx, r3, qx + 16, r3 + 16, 4, a(qh ? Draw.lerp(Draw.PALETTE[i], 0xFFFFFFFF, 0.25f) : Draw.PALETTE[i]));
            }

            // Row 4: favourites
            int r4 = r3 + 22;
            List<Integer> favs = Config.favorites();
            if (favs.isEmpty()) {
                text(ctx, "Saved colors show up here", x, r4 + 4, TEXT_DIM);
            } else {
                for (int i = 0; i < favs.size(); i++) {
                    int fx = x + i * 20;
                    int fc = favs.get(i);
                    boolean fh = hot && inside(mx, my, fx, r4, 16, 16);
                    Draw.checker(ctx, fx, r4, fx + 16, r4 + 16);
                    ctx.fill(fx, r4, fx + 16, r4 + 16, a(fh ? Draw.lerp(fc, 0xFFFFFFFF, 0.2f) : fc));
                }
                int clx = x + favs.size() * 20 + 4;
                if (clx + 44 < x + w) button(ctx, clx, r4, 44, 16, "Clear", mx, my, hot, accent);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            if (inside(mx, my, x, y + 12, w, 18)) {
                if (open) {
                    openPicker = null;
                    focusField(null);
                } else {
                    openPicker = key;
                    setHexText(String.format(Locale.ROOT, "%06X", current() & 0xFFFFFF));
                }
                return true;
            }
            if (!open) return false;

            final int bx = x;
            final int by = y + 36;
            final int swd = sw(w);
            final int shh = 72;

            if (inside(mx, my, bx, by, swd, shh)) {
                Drag d = (mx2, my2) -> {
                    float s = clamp01((mx2 - bx) / (double) Math.max(1, swd - 1));
                    float v = 1f - clamp01((my2 - by) / (double) (shh - 1));
                    Hsv cur = hsvOf(key, current());
                    putColor(key, cur.h, s, v, alphaOf());
                };
                startDrag("sv:" + key, d);
                d.move(mx, my);
                return true;
            }

            int hx = bx + swd + 8;
            if (inside(mx, my, hx - 2, by, 14, shh)) {
                Drag d = (mx2, my2) -> {
                    float hue = clamp01((my2 - by) / (double) (shh - 1)) * 359.9f;
                    Hsv cur = hsvOf(key, current());
                    putColor(key, hue, cur.s, cur.v, alphaOf());
                };
                startDrag("h:" + key, d);
                d.move(mx, my);
                return true;
            }

            if (useAlpha) {
                int ax = hx + 16;
                if (inside(mx, my, ax - 2, by, 14, shh)) {
                    Drag d = (mx2, my2) -> {
                        int al = Math.round(255f * (1f - clamp01((my2 - by) / (double) (shh - 1))));
                        Config.setIntQuiet(key, (al << 24) | (current() & 0xFFFFFF));
                    };
                    startDrag("a:" + key, d);
                    d.move(mx, my);
                    return true;
                }
            }

            int r2 = by + shh + 8;
            int bxx = x + 74;
            if (useRainbow) {
                if (inside(mx, my, bxx, r2, 56, 16)) {
                    Config.setInt(key + ".rb", rainbowOn() ? 0 : 1);
                    return true;
                }
                bxx += 60;
            }
            if (inside(mx, my, bxx, r2, 66, 16)) {
                Config.addFavorite(current() | 0xFF000000);
                toast("Color saved");
                return true;
            }

            int r3 = r2 + 22;
            for (int i = 0; i < Draw.PALETTE.length; i++) {
                if (inside(mx, my, x + i * 20, r3, 16, 16)) {
                    Config.setIntQuiet(key, (alphaOf() << 24) | (Draw.PALETTE[i] & 0xFFFFFF));
                    Config.setIntQuiet(key + ".rb", 0);
                    Config.save();
                    return true;
                }
            }

            int r4 = r3 + 22;
            List<Integer> favs = Config.favorites();
            for (int i = 0; i < favs.size(); i++) {
                if (inside(mx, my, x + i * 20, r4, 16, 16)) {
                    Config.setIntQuiet(key, (alphaOf() << 24) | (favs.get(i) & 0xFFFFFF));
                    Config.setIntQuiet(key + ".rb", 0);
                    Config.save();
                    return true;
                }
            }
            int clx = x + favs.size() * 20 + 4;
            if (!favs.isEmpty() && clx + 44 < x + w && inside(mx, my, clx, r4, 44, 16)) {
                Config.favorites().clear();
                Config.save();
                return true;
            }
            return true; // clicks inside the open picker never fall through
        }
    }

    /** Two live previews: left shows the Default look, right shows Entity in reach (or the look being edited). */
    private final class PreviewEntry extends Entry {
        private final int look;

        PreviewEntry(int look) {
            super(110);
            this.look = look;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            Draw.rrect(ctx, x, y, x + w, y + h, 6, a(0xFF0B0C11));
            int ix1 = x + 3, iy1 = y + 3, ix2 = x + w - 3, iy2 = y + h - 3;
            int mid = x + w / 2;
            Draw.vgrad(ctx, ix1 + 2, iy1, mid - 1, iy2, a(0xFF4A5166), a(0xFF2C3242));
            Draw.vgrad(ctx, mid + 1, iy1, ix2 - 2, iy2, a(0xFF4A5166), a(0xFF2C3242));
            ctx.fill(mid, iy1, mid + 1, iy2, a(0x33FFFFFF));

            String left = "NO ENTITY";
            String right = look >= 2 ? CrosshairRenderer.LOOKS[look].toUpperCase() : "ENTITY IN REACH";
            text(ctx, left, x + 12, y + 10, TEXT_SOFT);
            text(ctx, right, mid + 12, y + 10, TEXT_SOFT);

            int cyy = y + h / 2 + 6;
            CrosshairRenderer.drawPreview(ctx, x + w / 4, cyy, alpha * ea, 0, previewA);
            CrosshairRenderer.drawPreview(ctx, x + w * 3 / 4, cyy, alpha * ea, look >= 2 ? look : 1, previewB);
        }
    }

    /** A pixel grid you paint with the mouse. */
    private final class PixelEntry extends Entry {
        private static final int CELL = 11;
        private final int look;

        PixelEntry(int look) {
            super(CrosshairRenderer.PIXELS * CELL + 6);
            this.look = look;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            int n = CrosshairRenderer.PIXELS;
            int gx = x + 3;
            int gy = y + 3;
            Draw.rrect(ctx, x, y, x + n * CELL + 6, y + n * CELL + 6, 5, a(0xFF0B0C11));
            boolean[] px2 = CrosshairRenderer.readPixels(look);
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    boolean on = px2[j * n + i];
                    boolean center = i == n / 2 && j == n / 2;
                    int c = on ? accent : (center ? 0xFF2A2D40 : 0xFF181A25);
                    ctx.fill(gx + i * CELL + 1, gy + j * CELL + 1, gx + (i + 1) * CELL - 1, gy + (j + 1) * CELL - 1, a(c));
                }
            }
            text(ctx, "Click or drag to paint.", x + n * CELL + 16, y + 8, TEXT_DIM);
            text(ctx, "Click a lit pixel to erase it.", x + n * CELL + 16, y + 20, TEXT_DIM);
            text(ctx, "Pixel size is the Thickness slider.", x + n * CELL + 16, y + 32, TEXT_DIM);
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            final int n = CrosshairRenderer.PIXELS;
            final int gx = x + 3;
            final int gy = y + 3;
            if (!inside(mx, my, gx, gy, n * CELL, n * CELL)) return false;
            int ci = (int) ((mx - gx) / CELL);
            int cj = (int) ((my - gy) / CELL);
            final boolean paint = !CrosshairRenderer.readPixels(look)[cj * n + ci];
            Drag d = (mx2, my2) -> {
                int i = (int) Math.floor((mx2 - gx) / CELL);
                int j = (int) Math.floor((my2 - gy) / CELL);
                if (i < 0 || j < 0 || i >= n || j >= n) return;
                boolean[] cur = CrosshairRenderer.readPixels(look);
                if (cur[j * n + i] != paint) {
                    cur[j * n + i] = paint;
                    CrosshairRenderer.writePixels(look, cur);
                }
            };
            startDrag("pixels", d);
            d.move(mx, my);
            return true;
        }
    }

    private final class TextEntry extends Entry {
        private final String label;
        private final String body;

        TextEntry(String label, String body) {
            super(28);
            this.label = label;
            this.body = body;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            text(ctx, label.toUpperCase(), x, y, TEXT_DIM);
            text(ctx, textRenderer.trimToWidth(body, w), x, y + 12, TEXT_SOFT);
        }
    }

    /** A text box with a button next to it. */
    private final class FieldEntry extends Entry {
        private final TextFieldWidget field;
        private final String btn;
        private final Runnable action;

        FieldEntry(TextFieldWidget field, String btn, Runnable action) {
            super(24);
            this.field = field;
            this.btn = btn;
            this.action = action;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            int bw = textRenderer.getWidth(btn) + 20;
            field.setX(x);
            field.setY(y + 3);
            field.setWidth(w - bw - 6);
            shown.add(field);
            field.render(ctx, mx, my, 0f);
            button(ctx, x + w - bw, y + 3, bw, 16, btn, mx, my, hot, accent);
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            int bw = textRenderer.getWidth(btn) + 20;
            if (inside(mx, my, x + w - bw, y + 3, bw, 16)) {
                action.run();
                return true;
            }
            return false;
        }
    }


    /** A text box tied to a saved string (card titles, custom text lines, labels). */
    private final class StrEntry extends Entry {
        private final String label;
        private final String key;
        private final String def;

        StrEntry(String label, String key, String def) {
            super(24);
            this.label = label;
            this.key = key;
            this.def = def;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            int lw = Math.min(w / 2 - 6, textRenderer.getWidth(label) + 6);
            text(ctx, label, x, y + 8, TEXT_SOFT);
            if (strSlot >= strFields.length) return;
            TextFieldWidget f = strFields[strSlot];
            if (!key.equals(strKeys[strSlot])) {
                strLoading = true;
                f.setText(Config.getStr(key, def));
                strKeys[strSlot] = key;
                strLoading = false;
            }
            strSlot++;
            f.setX(x + lw + 4);
            f.setY(y + 3);
            f.setWidth(w - lw - 4);
            shown.add(f);
            f.render(ctx, mx, my, 0f);
        }
    }

    /** A paged grid of every item (or every potion); click an icon to add or remove it from the counters. */
    private final class PickerEntry extends Entry {
        private static final int CELL = 20;
        private final boolean potions;
        private final String key;
        private final List<Object[]> pool;   // {id string, ItemStack, display name lower-case}
        private int page;
        private String lastFilter = "";
        private List<Object[]> view;

        PickerEntry(boolean potions, String key, List<Object[]> pool) {
            super(12 + 6 * CELL + 20);
            this.potions = potions;
            this.key = key;
            this.pool = pool;
            this.view = pool;
        }

        private int cols(int w) { return Math.max(4, w / CELL); }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            String filter = Config.getStr("ui.pickfilter", "").trim().toLowerCase(Locale.ROOT);
            if (!filter.equals(lastFilter)) {
                lastFilter = filter;
                page = 0;
                if (filter.isEmpty()) view = pool;
                else {
                    List<Object[]> v = new ArrayList<>();
                    for (Object[] r : pool) if (((String) r[2]).contains(filter)) v.add(r);
                    view = v;
                }
            }
            int cols = cols(w);
            int per = cols * 6;
            int pages = Math.max(1, (view.size() + per - 1) / per);
            page = Math.max(0, Math.min(pages - 1, page));
            java.util.Set<String> sel = selected();
            text(ctx, (potions ? "Potions" : "Items") + ": " + sel.size() + " selected", x, y + 2, TEXT_SOFT);
            String pg = "< " + (page + 1) + " / " + pages + " >";
            text(ctx, pg, x + w - textRenderer.getWidth(pg), y + 2, accent);
            int gy = y + 14;
            ItemStack hover = null;
            for (int i = 0; i < per; i++) {
                int idx = page * per + i;
                if (idx >= view.size()) break;
                Object[] r = view.get(idx);
                int cx = x + (i % cols) * CELL;
                int cyy = gy + (i / cols) * CELL;
                boolean on = sel.contains((String) r[0]);
                boolean hov = hot && inside(mx, my, cx, cyy, CELL, CELL);
                Draw.rrect(ctx, cx, cyy, cx + CELL - 2, cyy + CELL - 2, 3,
                        a(on ? Draw.withAlpha(accent, 0x90) : hov ? 0xFF2A2D40 : 0xFF181A26));
                ctx.drawItem((ItemStack) r[1], cx + 1, cyy + 1);
                if (hov) hover = (ItemStack) r[1];
            }
            int by = gy + 6 * CELL + 2;
            button(ctx, x, by, 60, 16, "Clear all", mx, my, hot, accent);
            if (hover != null) text(ctx, hover.getName().getString(), x + 68, by + 4, TEXT_MAIN);
            else text(ctx, "Type in the box above to filter, click icons to pick", x + 68, by + 4, TEXT_DIM);
        }

        private java.util.Set<String> selected() {
            java.util.Set<String> out = new java.util.LinkedHashSet<>();
            for (String t : Config.getStr(key, "").split(",")) if (!t.isBlank()) out.add(t.trim());
            return out;
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            int cols = cols(w);
            int per = cols * 6;
            int pages = Math.max(1, (view.size() + per - 1) / per);
            String pg = "< " + (page + 1) + " / " + pages + " >";
            int pgw = textRenderer.getWidth(pg);
            if (inside(mx, my, x + w - pgw, y, pgw, 12)) {
                if (mx < x + w - pgw / 2.0) page = Math.max(0, page - 1);
                else page = Math.min(pages - 1, page + 1);
                return true;
            }
            int gy = y + 14;
            if (inside(mx, my, x, gy, cols * CELL, 6 * CELL)) {
                int i = (int) ((my - gy) / CELL) * cols + (int) ((mx - x) / CELL);
                int idx = page * per + i;
                if (idx >= 0 && idx < view.size()) {
                    String id = (String) view.get(idx)[0];
                    java.util.Set<String> sel = selected();
                    if (!sel.remove(id)) sel.add(id);
                    Config.setStr(key, String.join(",", sel));
                }
                return true;
            }
            if (inside(mx, my, x, gy + 6 * CELL + 2, 60, 16)) {
                Config.setStr(key, "");
                return true;
            }
            return false;
        }
    }

    /** One saved preset with Load / Update / Copy / Delete. */
    private final class PresetRowEntry extends Entry {
        private final String name;

        PresetRowEntry(String name) {
            super(26);
            this.name = name;
        }

        private String[] labels() {
            return new String[]{"Load", "Update", "Copy", name.equals(confirm) ? "Sure?" : "Delete"};
        }

        private int[] xs(int x, int w) {
            String[] l = labels();
            int[] out = new int[l.length];
            int cur = x + w - 6;
            for (int i = l.length - 1; i >= 0; i--) {
                int bw = textRenderer.getWidth(l[i]) + 14;
                cur -= bw;
                out[i] = cur;
                cur -= 4;
            }
            return out;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            Draw.rrect(ctx, x, y, x + w, y + h, 5, a(0xFF161821));
            String[] l = labels();
            int[] bx = xs(x, w);
            text(ctx, textRenderer.trimToWidth(name, Math.max(20, bx[0] - x - 18)), x + 12, y + 9, TEXT_MAIN);
            for (int i = 0; i < l.length; i++) {
                button(ctx, bx[i], y + 5, textRenderer.getWidth(l[i]) + 14, 16, l[i], mx, my, hot, i == 3 ? 0xFFF87171 : accent);
            }
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            String[] l = labels();
            int[] bx = xs(x, w);
            for (int i = 0; i < l.length; i++) {
                if (!inside(mx, my, bx[i], y + 5, textRenderer.getWidth(l[i]) + 14, 16)) continue;
                switch (i) {
                    case 0 -> {
                        Config.Snapshot s = Config.getPreset(name);
                        if (s != null) {
                            Config.apply(s, false);
                            toast("Loaded \"" + name + "\"");
                        }
                    }
                    case 1 -> {
                        Config.putPreset(name, Config.snapshot());
                        toast("Updated \"" + name + "\" with your current settings");
                    }
                    case 2 -> {
                        Config.Snapshot s = Config.getPreset(name);
                        if (s != null) {
                            String n = Config.uniquePresetName(name + " copy");
                            Config.putPreset(n, s);
                            toast("Duplicated as \"" + n + "\"");
                        }
                    }
                    default -> {
                        if (name.equals(confirm)) {
                            Config.deletePreset(name);
                            confirm = null;
                            toast("Deleted \"" + name + "\"");
                        } else {
                            confirm = name;
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }


    /** A search result that jumps to the tab holding the setting. */
    private final class HitEntry extends Entry {
        private final String title;
        private final int tabIndex;

        HitEntry(String title, int tabIndex) {
            super(22);
            this.title = title;
            this.tabIndex = tabIndex;
        }

        @Override
        void render(DrawContext ctx, int x, int y, int w, int mx, int my, boolean hot, int accent) {
            boolean hov = hot && inside(mx, my, x, y, w, h);
            Draw.rrect(ctx, x, y, x + w, y + h, 5, a(hov ? 0xFF1E2131 : 0xFF161821));
            text(ctx, title, x + 12, y + 7, TEXT_MAIN);
            String t = TABS[tabIndex];
            text(ctx, t, x + w - 12 - textRenderer.getWidth(t), y + 7, accent);
        }

        @Override
        boolean click(double mx, double my, int x, int y, int w) {
            if (!inside(mx, my, x, y, w, h)) return false;
            searchField.setText("");
            switchTab(tabIndex);
            return true;
        }
    }

    // ------------------------------------------------------------ entry builders

    private void switchTab(int i) {
        tab = i;
        scroll = 0;
        scrollTarget = 0;
        tabAt = Util.getMeasuringTimeMs();
        openPicker = null;
        confirm = null;
        focusField(null);
    }

    private ChipEntry onOff(String label, String key, int def) {
        return new ChipEntry(label, OFF_ON,
                () -> Math.max(0, Math.min(1, Config.getInt(key, def))),
                i -> Config.setInt(key, i));
    }

    private ChipEntry choice(String label, String[] options, String key, int def) {
        return new ChipEntry(label, options,
                () -> Math.max(0, Math.min(options.length - 1, Config.getInt(key, def))),
                i -> Config.setInt(key, i));
    }

    private SliderEntry slider(String label, String key, int min, int max, int def, String suffix) {
        return new SliderEntry(label, key, min, max, def, suffix);
    }

    private ColorEntry color(String label, String key, int def, boolean alphaOn, boolean rainbowOn) {
        return new ColorEntry(label, key, def, alphaOn, rainbowOn);
    }

    private List<Module> hudModules() {
        List<Module> l = new ArrayList<>();
        for (Module m : Modules.ALL) if (m.isHud()) l.add(m);
        return l;
    }

    private void tabHud(List<Entry> list) {
        list.add(new SectionEntry("Cards"));
        List<Module> hud = hudModules();
        for (Module m : hud) list.add(new ModuleEntry(m));

        list.add(new SectionEntry("Card settings"));
        String[] names = new String[hud.size()];
        for (int i = 0; i < names.length; i++) names[i] = hud.get(i).name;
        cardSel = Math.max(0, Math.min(names.length - 1, cardSel));
        list.add(new ChipEntry("Edit card", names, () -> cardSel, i -> cardSel = i));
        String id = hud.get(cardSel).id;
        list.add(new StrEntry("Card title", "card." + id + ".title", ""));
        optionEntries(hud.get(cardSel), list);
        list.add(color("Background", HudRenderer.ck(id, "bg"), 0xFF101218, false, false));
        list.add(color("Accent bar", HudRenderer.ck(id, "accent"), Themes.accA(), false, false));
        list.add(slider("Opacity", HudRenderer.ck(id, "opacity"), 10, 100, Config.getInt("ui.cardalpha", 67), "%"));
        list.add(slider("Size", HudRenderer.ck(id, "size"), 60, 200, 100, "%"));
        list.add(new ButtonEntry(new String[]{"Reset this card's style"},
                new Runnable[]{() -> Config.removeIntsWithPrefix("card." + id + ".")}));

        list.add(new SectionEntry("Layout"));
        list.add(new ButtonEntry(
                new String[]{"Edit layout", "Reset layout"},
                new Runnable[]{
                        () -> client.setScreen(new HudEditorScreen(this)),
                        () -> Config.removeIntsWithPrefix("pos.")
                }));
        list.add(new ChipEntry("Grid snapping", GRID_LABELS,
                () -> {
                    int g = Config.getInt("ui.grid", 0);
                    for (int i = 0; i < GRIDS.length; i++) if (GRIDS[i] == g) return i;
                    return 0;
                }, i -> Config.setInt("ui.grid", GRIDS[i])));
        list.add(new ChipEntry("Default corner", CORNERS, Config::corner, i -> Config.setInt("ui.corner", i)));
        list.add(new ChipEntry("HUD scale", SCALE_LABELS, () -> {
            int best = 0;
            int cur = Config.getInt("ui.scale", 100);
            for (int i = 0; i < SCALES.length; i++) {
                if (Math.abs(cur - SCALES[i]) < Math.abs(cur - SCALES[best])) best = i;
            }
            return best;
        }, i -> Config.setInt("ui.scale", SCALES[i])));
        list.add(slider("Default card opacity", "ui.cardalpha", 25, 100, 67, "%"));
        list.add(onOff("Card titles", "ui.labels", 1));
        list.add(onOff("Rounded corners", "ui.round", 1));
    }

    private static List<Object[]> itemPool() {
        if (itemPool == null) {
            List<Object[]> l = new ArrayList<>();
            for (net.minecraft.item.Item it : net.minecraft.registry.Registries.ITEM) {
                if (it == net.minecraft.item.Items.AIR) continue;
                String id = net.minecraft.registry.Registries.ITEM.getId(it).toString();
                ItemStack st = new ItemStack(it);
                l.add(new Object[]{id, st, (id + " " + st.getName().getString()).toLowerCase(Locale.ROOT)});
            }
            itemPool = l;
        }
        return itemPool;
    }

    private static List<Object[]> potionPool() {
        if (potionPool == null) {
            List<Object[]> l = new ArrayList<>();
            for (var entry : net.minecraft.registry.Registries.POTION.streamEntries().toList()) {
                String id = entry.registryKey().getValue().toString();
                ItemStack st = net.minecraft.component.type.PotionContentsComponent.createStack(net.minecraft.item.Items.POTION, entry);
                l.add(new Object[]{id, st, (id + " " + st.getName().getString()).toLowerCase(Locale.ROOT)});
            }
            potionPool = l;
        }
        return potionPool;
    }

    private void optionEntries(Module m, List<Entry> list) {
        for (Opt o : m.options()) {
            String key = m.optKey(o.name);
            switch (o.type) {
                case ONOFF -> list.add(onOff(o.label, key, o.def));
                case CHOICE -> list.add(choice(o.label, o.choices, key, o.def));
                case SLIDER -> list.add(slider(o.label, key, o.min, o.max, o.def, o.suffix));
                case COLOR -> list.add(color(o.label, key, o.def, false, false));
                case TEXT -> list.add(new StrEntry(o.label, key, o.defText));
            }
        }
        if (m.id.equals("itemcount")) {
            list.add(new StrEntry("Filter", "ui.pickfilter", ""));
            list.add(new PickerEntry(false, m.optKey("items"), itemPool()));
            list.add(new PickerEntry(true, m.optKey("potions"), potionPool()));
        }
    }

    /** A titled group of module cards. A module's own settings appear under it while it is switched on. */
    private void moduleGroup(List<Entry> list, String title, String... ids) {
        list.add(new SectionEntry(title));
        for (String id : ids) {
            Module m = Modules.byId(id);
            if (m == null) continue;
            list.add(new ModuleEntry(m));
            if (m.isEnabled() && !m.options().isEmpty() && !m.isHud()) optionEntries(m, list);
        }
    }

    private void tabVisuals(List<Entry> list) {
        moduleGroup(list, "Screen tweaks", "lowshield", "lowfire", "smalltotem");
        moduleGroup(list, "Visibility", "fullbright", "lavavision", "clearwater", "smallexplosions", "hideparticles");
        moduleGroup(list, "Sound and particles", "quietexplosions", "fewtotem");
        moduleGroup(list, "Hands", "handpos", "noequip", "fastswing");
    }

    private void tabMods(List<Entry> list) {
        list.add(new NoteEntry("Rebuilt versions of popular mods, written for this mod. Each one switches on and off by itself. "
                + "If you also have the original installed, a notice appears under it.", TEXT_DIM));
        moduleGroup(list, "Food, items and info", "appleskin", "shulker", "tnt", "shieldstatus", "shieldfix", "hotbarkeys");
        moduleGroup(list, "Camera and movement", "freelook", "zoom", "toggles");
        String fk = PvpMenuClient.FREELOOK_KEY.getBoundKeyLocalizedText().getString();
        String zk = PvpMenuClient.ZOOM_KEY.getBoundKeyLocalizedText().getString();
        list.add(new TextEntry("Freelook key", "[" + fk + "]"));
        list.add(new TextEntry("Zoom key", "[" + zk + "]"));
        list.add(new NoteEntry("Change these in Options > Controls > Key Binds > Ultimate PvP Utilities.", TEXT_DIM));
    }


    private void tabHome(List<Entry> list) {
        int on = 0;
        for (Module m : Modules.ALL) if (m.isEnabled()) on++;
        list.add(new SectionEntry("Welcome"));
        list.add(new NoteEntry("Everything here is client-side and fair play: it only shows information or changes how the game looks. "
                + "Nothing clicks, aims or reacts for you. " + on + " of " + Modules.ALL.size() + " modules are on right now.", TEXT_SOFT));
        list.add(new SectionEntry("Quick start"));
        list.add(new ButtonEntry(new String[]{"Competitive", "Minimal", "Everything"}, new Runnable[]{
                () -> { Config.apply(Presets.build(0), true); toast("Loaded Competitive"); },
                () -> { Config.apply(Presets.build(1), true); toast("Loaded Minimal"); },
                () -> { Config.apply(Presets.build(2), true); toast("Loaded Everything"); }}));
        list.add(new ButtonEntry(new String[]{"Edit HUD layout", "Open Crosshair", "Open Warm-up"}, new Runnable[]{
                () -> client.setScreen(new HudEditorScreen(this)),
                () -> switchTab(3),
                () -> switchTab(5)}));
        list.add(new SectionEntry("Suggestions"));
        if (on == 0) list.add(new NoteEntry("- Nothing is on yet. Load Competitive above for a good start.", TEXT_DIM));
        if (!Modules.CROSSHAIR.isEnabled()) list.add(new NoteEntry("- Turn on Custom Crosshair to get the colour-on-target crosshair.", TEXT_DIM));
        if (!Modules.byId("cps").isEnabled()) list.add(new NoteEntry("- CPS Counter and Keystrokes are popular for checking your clicking.", TEXT_DIM));
        if (!Modules.TNT.isEnabled()) list.add(new NoteEntry("- TNT Fuse Countdown helps in crystal and TNT fights.", TEXT_DIM));
        if (!Modules.NOFOG.isEnabled()) list.add(new NoteEntry("- No Fog keeps far terrain sharp (check server rules).", TEXT_DIM));
        list.add(new NoteEntry("- Use the search box above to find any setting. Save your own setups in Presets.", TEXT_DIM));
        list.add(new NoteEntry("- Warm-up has a reaction test, aim trainer and click speed test to get ready before a fight.", TEXT_DIM));
        list.add(new SectionEntry("Keys"));
        list.add(new TextEntry("Menu", "[" + PvpMenuClient.MENU_KEY.getBoundKeyLocalizedText().getString() + "]"));
        list.add(new TextEntry("Freelook", "[" + PvpMenuClient.FREELOOK_KEY.getBoundKeyLocalizedText().getString() + "]"));
        list.add(new TextEntry("Zoom", "[" + PvpMenuClient.ZOOM_KEY.getBoundKeyLocalizedText().getString() + "]"));
    }

    private void tabGuide(List<Entry> list) {
        list.add(new SectionEntry("How it works"));
        list.add(new NoteEntry("Open this menu with the menu key. Switch modules on with their card. Modules with extra settings show them under the card. "
                + "The search box finds any setting. Everything saves automatically to config/pvpmenu.json.", TEXT_SOFT));
        list.add(new SectionEntry("Tabs"));
        list.add(new NoteEntry("HUD: cards on screen (armor, totems, CPS, keystrokes, target info, info, item counters, potion timers, your own text). Edit layout lets you drag them.", TEXT_DIM));
        list.add(new NoteEntry("Visuals: screen tweaks, fullbright, lava/water/no fog, particles, hands.", TEXT_DIM));
        list.add(new NoteEntry("Crosshair: styles, colour on target, your own pixel drawings and PNG images.", TEXT_DIM));
        list.add(new NoteEntry("Utilities: food display, container preview, TNT countdown, shield indicator, slot key labels, freelook, zoom.", TEXT_DIM));
        list.add(new NoteEntry("Warm-up: reaction test, aim trainer, click speed test.", TEXT_DIM));
        list.add(new NoteEntry("Presets: built-in and your own setups, share codes.", TEXT_DIM));
        list.add(new SectionEntry("Fair play"));
        list.add(new NoteEntry("This mod never automates anything. Some visual tweaks (fullbright, fog, particles, freelook) may be restricted on some servers. "
                + "They start off and are marked. Always read your server's rules before using them.", TEXT_DIM));
        list.add(new SectionEntry("Troubleshooting"));
        list.add(new NoteEntry("A module that keeps erroring switches itself off; click its card to try again. If a tweak does nothing after a Minecraft update, "
                + "that tweak is skipped quietly and the rest keeps working. Use the diagnostics button when you report a problem.", TEXT_DIM));
        list.add(new ButtonEntry(new String[]{"Copy diagnostics"}, new Runnable[]{() -> {
            StringBuilder sb = new StringBuilder("Ultimate PvP Utilities diagnostics\n");
            sb.append("Minecraft ").append(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")).append('\n');
            sb.append("Enabled: ");
            for (Module m : Modules.ALL) if (m.isEnabled()) sb.append(m.id).append(' ');
            sb.append("\nTripped: ").append(Guard.anyTripped());
            client.keyboard.setClipboard(sb.toString());
            toast("Diagnostics copied");
        }}));
        list.add(new SectionEntry("Changelog"));
        list.add(new NoteEntry("v6: new menu layout with Home and Guide, lava and water fix, No Fog, potion timers, custom text cards, image crosshairs, "
                + "smoother TNT countdown, more warm-up tests.", TEXT_DIM));
        list.add(new SectionEntry("Credits"));
        list.add(new NoteEntry("Some utilities are based on open-source work (MIT, Unlicense, CC0). Their notices are inside the mod file under META-INF/licenses.", TEXT_DIM));
    }

    private void tabWarmup(List<Entry> list) {
        list.add(new SectionEntry("Reaction test"));
        list.add(new NoteEntry("Wait for the screen to turn green, then click. It opens a screen that uses your mouse cursor.", TEXT_DIM));
        list.add(new TextEntry("Personal best", Warmup.reactBest() > 0 ? Warmup.reactBest() + " ms" : "none yet"));
        list.add(new ButtonEntry(new String[]{"Start reaction test"},
                new Runnable[]{() -> client.setScreen(new ReactionScreen(this))}));
        list.add(slider("Rounds", "wu.react.rounds", 1, 10, 5, ""));
        list.add(slider("Shortest wait", "wu.react.min", 1, 5, 1, " s"));
        list.add(slider("Longest wait", "wu.react.max", 2, 10, 4, " s"));
        list.add(color("Waiting color", "wu.react.wait", 0xFFB91C1C, false, false));
        list.add(color("Go color", "wu.react.go", 0xFF16A34A, false, false));

        list.add(new SectionEntry("Click speed test"));
        list.add(new NoteEntry("Click the pad as fast as you can. Counts left and right clicks.", TEXT_DIM));
        int cs = Warmup.clickSeconds();
        int cb = Config.getInt("wu.click.best." + cs, 0);
        list.add(new TextEntry("Best (" + cs + " s)", cb > 0 ? String.format(Locale.ROOT, "%.1f CPS", cb / 10f) : "none yet"));
        list.add(new ButtonEntry(new String[]{"Start click test"}, new Runnable[]{() -> client.setScreen(new ClickScreen(this))}));
        list.add(choice("Length", Warmup.CLICK_LENGTHS, "wu.click.len", 1));

        list.add(new SectionEntry("Tracking test"));
        list.add(new NoteEntry("Keep your cursor on a moving target. Great for practising smooth aim.", TEXT_DIM));
        int tb = Config.getInt("wu.track.best." + Warmup.trackSeconds(), 0);
        list.add(new TextEntry("Best", tb > 0 ? tb + "% on target" : "none yet"));
        list.add(new ButtonEntry(new String[]{"Start tracking test"}, new Runnable[]{() -> client.setScreen(new TrackScreen(this))}));
        list.add(choice("Length", Warmup.DURATIONS, "wu.track.len", 1));
        list.add(choice("Speed", Warmup.TRACK_SPEEDS, "wu.track.speed", 1));
        list.add(slider("Target size", "wu.track.size", 10, 50, 22, " px"));

        list.add(new SectionEntry("Aim trainer"));
        list.add(new NoteEntry("Click the targets before they vanish. Pick a level or choose Custom to set everything yourself. "
                + "It opens a screen that uses your mouse cursor.", TEXT_DIM));
        int lvl = Warmup.level();
        if (lvl > 0) {
            list.add(new TextEntry("Best on " + Warmup.LEVELS[lvl], Warmup.aimBest(lvl) > 0 ? Warmup.aimBest(lvl) + " hits" : "none yet"));
        }
        list.add(new ButtonEntry(new String[]{"Start aim trainer"},
                new Runnable[]{() -> client.setScreen(new AimScreen(this))}));
        list.add(choice("Level", Warmup.LEVELS, "wu.aim.level", 2));
        list.add(choice("Length", Warmup.DURATIONS, "wu.aim.dur", 1));
        if (lvl == 0) {
            list.add(slider("Target size", "wu.aim.size", 8, 60, 28, " px"));
            list.add(slider("Target lasts (30 = until hit)", "wu.aim.life", 5, 30, 18, " ds"));
            list.add(slider("Targets at once", "wu.aim.count", 1, 5, 2, ""));
            list.add(choice("Movement", Warmup.MOVING, "wu.aim.move", 0));
        }
        list.add(choice("Shape", Warmup.SHAPES, "wu.aim.shape", 0));
        list.add(color("Target color", "wu.aim.color", 0xFFFF5C7A, false, false));
        list.add(slider("Background darkness", "wu.aim.dim", 20, 100, 55, "%"));
    }

    private void tabCrosshair(List<Entry> list) {
        final int L = xhLook;
        list.add(new ModuleEntry(Modules.CROSSHAIR));
        list.add(new PreviewEntry(L));
        list.add(new ChipEntry("Mode", new String[]{"Simple", "Advanced"}, () -> Config.getInt("ui.xhadv", 0), i -> Config.setInt("ui.xhadv", i)));
        final boolean adv = Config.getInt("ui.xhadv", 0) == 1;
        if (!adv) {
            list.add(new NoteEntry("Simple mode shows the basics. Try: Dot or Chunky preset, then pick a colour for the Entity in reach look "
                    + "so it flips colour when you can hit something. Switch to Advanced for rotation, offsets, animation and more.", TEXT_DIM));
        }

        list.add(new SectionEntry("Edit look"));
        list.add(new ChipEntry("Look", CrosshairRenderer.LOOKS, () -> xhLook, i -> xhLook = i));
        if (L >= 2) {
            list.add(onOff("Use this look", CrosshairRenderer.key(L, "on"), 0));
            list.add(new NoteEntry(LOOK_HINTS[L] + " When it is off, the Default and Entity looks are used.", TEXT_DIM));
            list.add(new ButtonEntry(
                    new String[]{"Copy from Default", "Copy from Entity in reach"},
                    new Runnable[]{
                            () -> Config.copyPrefix(CrosshairRenderer.key(0, ""), CrosshairRenderer.key(L, "")),
                            () -> Config.copyPrefix(CrosshairRenderer.key(1, ""), CrosshairRenderer.key(L, ""))
                    }));
        } else {
            Runnable[] presets = new Runnable[CrosshairRenderer.PRESETS.length];
            for (int i = 0; i < presets.length; i++) {
                final int idx = i;
                presets[i] = () -> {
                    CrosshairRenderer.applyPreset(idx);
                    toast("Preset applied to Default and Entity in reach");
                };
            }
            list.add(new SectionEntry("Presets"));
            list.add(new ButtonEntry(CrosshairRenderer.PRESETS, presets));
        }

        list.add(new SectionEntry("Shape"));
        int style = Math.max(0, Math.min(CrosshairRenderer.STYLES.length - 1, Config.getInt(CrosshairRenderer.key(L, "style"), 0)));
        list.add(choice("Style", CrosshairRenderer.STYLES, CrosshairRenderer.key(L, "style"), 0));
        if (style == CrosshairRenderer.STYLE_IMAGE) {
            List<String> files = dev.pvpmenu.ImageCrosshair.list();
            if (files.isEmpty()) {
                list.add(new NoteEntry("No images yet. Drag a PNG file onto this menu, or put PNG files in config/pvpmenu/crosshairs.", TEXT_DIM));
            } else {
                String cur = Config.getStr(CrosshairRenderer.key(L, "img"), "");
                int selIdx = Math.max(0, files.indexOf(cur));
                String[] names = files.toArray(new String[0]);
                list.add(new ChipEntry("Image", names, () -> Math.max(0, files.indexOf(Config.getStr(CrosshairRenderer.key(L, "img"), files.get(0)))),
                        i -> Config.setStr(CrosshairRenderer.key(L, "img"), files.get(i))));
            }
            list.add(slider("Image size", CrosshairRenderer.key(L, "imgsize"), 8, 64, 16, " px"));
            list.add(onOff("Tint with crosshair color", CrosshairRenderer.key(L, "imgtint"), 0));
            list.add(new NoteEntry("Drag PNG files onto this menu to add them. Small images with a transparent background work best.", TEXT_DIM));
        } else if (style == CrosshairRenderer.STYLE_PIXEL) {
            list.add(new PixelEntry(L));
            list.add(new ButtonEntry(new String[]{"Clear", "Plus", "Invert", "Dot", "Ring"}, new Runnable[]{
                    () -> CrosshairRenderer.pixelPreset(L, 0),
                    () -> CrosshairRenderer.pixelPreset(L, 1),
                    () -> CrosshairRenderer.pixelPreset(L, 2),
                    () -> CrosshairRenderer.pixelPreset(L, 3),
                    () -> CrosshairRenderer.pixelPreset(L, 4)
            }));
            list.add(slider("Pixel size", CrosshairRenderer.key(L, "thick"), 1, 6, 1, " px"));
        } else {
            list.add(slider("Length horizontal", CrosshairRenderer.key(L, "armH"), 0, 30, 5, " px"));
            list.add(slider("Length vertical", CrosshairRenderer.key(L, "armV"), 0, 30, 5, " px"));
            if (style != CrosshairRenderer.STYLE_DOT) {
                list.add(slider("Gap", CrosshairRenderer.key(L, "gap"), 0, 16, 2, " px"));
            }
            list.add(slider("Thickness", CrosshairRenderer.key(L, "thick"), 1, 6, 1, " px"));
            if (style == 0 || style == 1 || style == 10 || style == 11) {
                String[] keys = {
                        CrosshairRenderer.key(L, "top"), CrosshairRenderer.key(L, "bottom"),
                        CrosshairRenderer.key(L, "left"), CrosshairRenderer.key(L, "right")
                };
                list.add(new ToggleRowEntry("Arms", new String[]{"Top", "Bottom", "Left", "Right"}, keys));
            }
        }

        if (adv) {
            list.add(new SectionEntry("Position and rotation"));
            list.add(slider("Rotation", CrosshairRenderer.key(L, "rot"), 0, 359, 0, " deg"));
            list.add(slider("Offset left / right", CrosshairRenderer.key(L, "offx"), -40, 40, 0, " px"));
            list.add(slider("Offset up / down", CrosshairRenderer.key(L, "offy"), -40, 40, 0, " px"));
        }

        list.add(new SectionEntry("Color"));
        list.add(color("Crosshair color", CrosshairRenderer.key(L, "color"), CrosshairRenderer.defColor(L), true, true));
        list.add(slider("Opacity", CrosshairRenderer.key(L, "opacity"), 10, 100, 100, "%"));
        if (style != CrosshairRenderer.STYLE_DOT) {
            list.add(onOff("Center dot", CrosshairRenderer.key(L, "dot"), 0));
            if (Config.getInt(CrosshairRenderer.key(L, "dot"), 0) == 1) {
                list.add(slider("Dot size", CrosshairRenderer.key(L, "dotsize"), 1, 8, 2, " px"));
            }
        }

        list.add(new SectionEntry("Outline"));
        list.add(slider("Outline size", CrosshairRenderer.key(L, "outline"), 0, 4, 1, " px"));
        if (adv) {
            list.add(color("Outline color", CrosshairRenderer.key(L, "outc"), 0xFF000000, true, false));
            list.add(choice("Outline style", CrosshairRenderer.OUTLINE_STYLES, CrosshairRenderer.key(L, "outs"), 0));

            list.add(new SectionEntry("Animation"));
            list.add(choice("Animation", CrosshairRenderer.ANIMS, CrosshairRenderer.key(L, "anim"), 0));
            list.add(slider("Animation speed", "xh.g.animspeed", 1, 10, 4, ""));
        }

        list.add(new SectionEntry("Entity in reach and hits"));
        list.add(choice("Counts as target", CrosshairRenderer.TARGET_FILTERS, "xh.g.targets", 0));
        list.add(onOff("Hit marker", "xh.g.hitmark", 1));
        if (Config.getInt("xh.g.hitmark", 1) == 1) {
            list.add(choice("Hit marker style", CrosshairRenderer.HIT_STYLES, "xh.g.hitstyle", 0));
            list.add(slider("Hit marker size", "xh.g.hitsize", 2, 20, 6, " px"));
            list.add(color("Hit marker color", "xh.g.hitcolor", 0xFFFF4D4D, true, false));
        }
        if (adv) {
            list.add(onOff("Pop when you hit", "xh.g.hitpop", 0));

            list.add(new SectionEntry("Extras"));
            list.add(onOff("Spread while moving", "xh.g.dyn", 0));
            if (Config.getInt("xh.g.dyn", 0) == 1) {
                list.add(slider("Spread amount", "xh.g.spread", 1, 10, 3, " px"));
            }
            list.add(onOff("Cooldown bar", "xh.g.cdbar", 0));
        }

        list.add(new SectionEntry("Share"));
        list.add(new ButtonEntry(new String[]{"Copy crosshair code", "Import code from clipboard"}, new Runnable[]{
                () -> {
                    client.keyboard.setClipboard(Config.encode(Config.snapshotPrefix("xh.")));
                    toast("Crosshair code copied. Paste it to a friend.");
                },
                () -> {
                    Config.Snapshot s = Config.decode(client.keyboard.getClipboard());
                    boolean has = false;
                    if (s != null) for (String k : s.ints.keySet()) if (k.startsWith("xh.")) has = true;
                    if (!has) {
                        toast("The clipboard has no crosshair code");
                    } else {
                        Config.mergePrefix(s, "xh.");
                        toast("Crosshair imported");
                    }
                }
        }));
    }

    private void tabPresets(List<Entry> list) {
        list.add(new SectionEntry("Built-in setups"));
        for (int i = 0; i < Presets.BUILTIN.length; i++) {
            list.add(new TextEntry(Presets.BUILTIN[i], Presets.BUILTIN_INFO[i]));
        }
        Runnable[] loads = new Runnable[Presets.BUILTIN.length];
        for (int i = 0; i < loads.length; i++) {
            final int idx = i;
            loads[i] = () -> {
                Config.apply(Presets.build(idx), true);
                toast("Loaded " + Presets.BUILTIN[idx] + " (your theme and card positions stay)");
            };
        }
        list.add(new ButtonEntry(Presets.BUILTIN, loads));

        list.add(new SectionEntry("My setups"));
        List<String> names = Config.presetNames();
        if (names.isEmpty()) {
            list.add(new NoteEntry("Nothing saved yet. Name your current setup below and press Save.", TEXT_DIM));
        }
        for (String n : names) list.add(new PresetRowEntry(n));
        list.add(new FieldEntry(nameField, "Save current", () -> {
            String n = nameField.getText().trim();
            if (n.isEmpty()) {
                toast("Type a name first");
                return;
            }
            if (n.length() > 24) n = n.substring(0, 24);
            boolean existed = Config.presetNames().contains(n);
            Config.putPreset(n, Config.snapshot());
            toast(existed ? "Replaced \"" + n + "\"" : "Saved \"" + n + "\"");
        }));
        list.add(new NoteEntry("A setup stores every module, crosshair, colour and card setting. Copy makes a duplicate you can change.", TEXT_DIM));

        list.add(new SectionEntry("Share a setup"));
        list.add(new ButtonEntry(new String[]{"Copy my settings code", "Import code from clipboard"}, new Runnable[]{
                () -> {
                    client.keyboard.setClipboard(Config.encode(Config.snapshot()));
                    toast("Settings code copied");
                },
                () -> {
                    Config.Snapshot s = Config.decode(client.keyboard.getClipboard());
                    if (s == null) {
                        toast("The clipboard has no settings code");
                    } else {
                        String n = Config.uniquePresetName("Imported");
                        Config.putPreset(n, s);
                        toast("Imported as \"" + n + "\". Press Load to use it.");
                    }
                }
        }));
    }

    private void tabSettings(List<Entry> list) {
        list.add(new SectionEntry("Theme"));
        list.add(new ChipEntry("Theme", Themes.NAMES, Themes::index, i -> Config.setInt("ui.theme", i)));
        if (Themes.custom()) {
            list.add(color("Main color", "ui.accA", 0xFF7C5CFF, false, false));
            if (Config.getInt("ui.grad", 1) == 1) {
                list.add(color("Gradient color", "ui.accB", 0xFF00E0FF, false, false));
            }
        }
        list.add(onOff("Gradient accents", "ui.grad", 1));
        list.add(slider("Backdrop dim", "ui.dim", 0, 85, 60, "%"));
        list.add(onOff("Menu animations", "ui.anim", 1));
        list.add(slider("Menu opacity", "ui.menualpha", 40, 100, 100, "%"));
        list.add(onOff("Menu click sounds", "ui.sounds", 0));

        list.add(new SectionEntry("Keys"));
        String key = PvpMenuClient.MENU_KEY.getBoundKeyLocalizedText().getString();
        list.add(new TextEntry("Menu key", "Currently [" + key + "]"));
        list.add(new NoteEntry("Every module has its own optional toggle key, and there is a Hide HUD key. "
                + "Set them in Options > Controls > Key Binds > Ultimate PvP Utilities. They start unbound.", TEXT_DIM));

        list.add(new SectionEntry("Safety"));
        if (Guard.anyTripped()) {
            list.add(new NoteEntry("Some modules were switched off after errors so the game would not crash.", TEXT_BAD));
            list.add(new ButtonEntry(new String[]{"Try them again"}, new Runnable[]{Guard::resetAll}));
        } else {
            list.add(new NoteEntry("Crash protection is on. If a module ever keeps throwing errors, it is switched off "
                    + "instead of crashing the game.", TEXT_DIM));
        }
        boolean sure = "reset".equals(confirm);
        list.add(new ButtonEntry(new String[]{sure ? "Click again to reset everything" : "Reset all settings"}, new Runnable[]{() -> {
            if ("reset".equals(confirm)) {
                Config.resetAll();
                confirm = null;
                toast("All settings reset (your saved setups are kept)");
            } else {
                confirm = "reset";
            }
        }}));
    }

    private void searchResults(List<Entry> list, String q) {
        String[] tokens = q.toLowerCase(Locale.ROOT).split("\\s+");
        int found = 0;
        list.add(new SectionEntry("Search results"));
        for (Module m : Modules.ALL) {
            String hay = (m.name + " " + m.description + " " + m.category).toLowerCase(Locale.ROOT);
            if (matches(hay, tokens)) {
                list.add(new ModuleEntry(m));
                found++;
            }
        }
        for (Object[] row : INDEX) {
            String hay = (row[0] + " " + row[2]).toLowerCase(Locale.ROOT);
            if (matches(hay, tokens)) {
                list.add(new HitEntry((String) row[0], (Integer) row[1]));
                found++;
            }
        }
        if (found == 0) list.add(new NoteEntry("Nothing found for \"" + q + "\".", TEXT_DIM));
    }

    private static boolean matches(String hay, String[] tokens) {
        for (String t : tokens) {
            if (!t.isEmpty() && !hay.contains(t)) return false;
        }
        return true;
    }

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        String q = searchField == null ? "" : searchField.getText().trim();
        if (!q.isEmpty()) {
            searchResults(list, q);
            return list;
        }
        switch (tab) {
            case 0 -> tabHome(list);
            case 1 -> tabHud(list);
            case 2 -> tabVisuals(list);
            case 3 -> tabCrosshair(list);
            case 4 -> tabMods(list);
            case 5 -> tabWarmup(list);
            case 6 -> tabPresets(list);
            case 7 -> tabSettings(list);
            default -> tabGuide(list);
        }
        return list;
    }

    private int totalHeight(List<Entry> list) {
        int total = 0;
        for (Entry e : list) total += e.h + GAP;
        return Math.max(0, total - GAP);
    }

    // ------------------------------------------------------------------ layout

    private void layout() {
        pw = Math.min(560, width - 24);
        ph = Math.min(360, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + Math.round((1f - alpha) * 14f);
        sx = px + SIDE_W + 10;
        sy = py + HEADER_H + 10;
        cx = sx;
        cy = sy + SEARCH_H + 8;
        cw = pw - SIDE_W - 10 - 14;
        ch = ph - HEADER_H - 10 - SEARCH_H - 8 - 14;
    }

    private int tabY(int i) { return py + HEADER_H + 14 + i * TAB_STEP; }
    private int tabX() { return px + 14; }
    private int tabW() { return SIDE_W - 22; }
    private int tabH() { return 28; }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (animated()) {
            float t = Math.min(1f, (now - openedAt) / 240f);
            alpha = 1f - (float) Math.pow(1f - t, 3);
        } else {
            alpha = 1f;
        }
        ea = 1f;

        if (!focusCleared) {
            focusField(null);
            focusCleared = true;
        }

        // Keep dragging while the mouse button is held
        if (drag != null) {
            boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (down) {
                drag.move(mouseX, mouseY);
            } else {
                drag = null;
                dragKey = null;
                Config.save();
            }
        }

        layout();
        int dim = Math.round(2.55f * Math.max(0, Math.min(85, Config.getInt("ui.dim", 60))));
        ctx.fill(0, 0, width, height, a(Draw.withAlpha(0x070810, dim)));
        if (alpha < 0.05f) return;

        int accent = accA();
        int accent2 = accB();

        // Panel: glow, gradient border, gradient body, top highlight
        glow(ctx, px, py, px + pw, py + ph, 6, accent, 0x30);
        Draw.rrect(ctx, px - 1, py - 1, px + pw + 1, py + ph + 1, 7, a(Draw.withAlpha(Draw.lerp(accent, accent2, 0.5f), 0x80)));
        int mo = Math.round(2.55f * Math.max(40, Math.min(100, Config.getInt("ui.menualpha", 100))));
        Draw.rrect(ctx, px, py, px + pw, py + ph, 6, a(Draw.withAlpha(0x111219, mo)));
        Draw.vgrad(ctx, px + 1, py + 6, px + pw - 1, py + ph - 6, a(Draw.withAlpha(Themes.top(), Math.min(mo, (Themes.top() >>> 24)))), a(Draw.withAlpha(Themes.bottom(), Math.min(mo, (Themes.bottom() >>> 24)))));
        Draw.hgrad(ctx, px + 28, py + 1, px + pw - 28, py + 2, a(Draw.withAlpha(accent, 0xB0)), a(Draw.withAlpha(accent2, 0xB0)));

        renderHeader(ctx, accent, accent2);
        renderSidebar(ctx, mouseX, mouseY, accent);

        shown.clear();
        strSlot = 0;
        renderSearch(ctx, mouseX, mouseY);
        renderEntries(ctx, mouseX, mouseY, accent);

        // A text box that scrolled out of view must not keep the keyboard
        for (TextFieldWidget f : fields()) {
            if (f != null && f.isFocused() && !shown.contains(f)) focusField(null);
        }

        renderToast(ctx, accent);
    }

    private void renderSearch(DrawContext ctx, int mx, int my) {
        Draw.rrect(ctx, sx, sy, sx + cw, sy + SEARCH_H, 5, a(0xFF0F1017));
        searchField.setX(sx + 6);
        searchField.setY(sy + 1);
        searchField.setWidth(cw - 12);
        shown.add(searchField);
        searchField.render(ctx, mx, my, 0f);
    }

    private void renderToast(DrawContext ctx, int accent) {
        long age = Util.getMeasuringTimeMs() - toastAt;
        if (age > 3000 || toastText.isEmpty()) return;
        float f = age < 200 ? age / 200f : (age > 2600 ? (3000 - age) / 400f : 1f);
        int w = textRenderer.getWidth(toastText) + 24;
        int x = px + (pw - w) / 2;
        int y = py + ph - 30;
        float keep = ea;
        ea = clamp01(f);
        glow(ctx, x, y, x + w, y + 18, 6, accent, 0x40);
        Draw.rrect(ctx, x, y, x + w, y + 18, 6, a(0xFF1B1E2E));
        text(ctx, toastText, x + 12, y + 5, TEXT_MAIN);
        ea = keep;
    }

    private void renderHeader(DrawContext ctx, int accent, int accent2) {
        // Logo
        glow(ctx, px + 12, py + 7, px + 44, py + 39, 6, accent, 0x40);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICON, px + 12, py + 7, 0f, 0f, 32, 32, 32, 32, a(0xFFFFFFFF));

        Text name1 = Text.literal("ULTIMATE").formatted(Formatting.BOLD);
        ctx.drawText(textRenderer, name1, px + 52, py + 12, a(accent), false);
        ctx.drawText(textRenderer, Text.literal("PVP UTILITIES").formatted(Formatting.BOLD),
                px + 52 + textRenderer.getWidth(name1) + 4, py + 12, a(TEXT_MAIN), false);
        text(ctx, "Client-side utilities. Fair play only.", px + 52, py + 25, TEXT_DIM);

        // Key hint as a keycap
        String key = PvpMenuClient.MENU_KEY.getBoundKeyLocalizedText().getString();
        int kw = textRenderer.getWidth(key) + 14;
        int kx = px + pw - 16 - kw;
        Draw.rrect(ctx, kx, py + 13, kx + kw, py + 31, 5, a(0xFF2B2E44));
        Draw.rrect(ctx, kx, py + 13, kx + kw, py + 29, 5, a(0xFF1B1D2B));
        text(ctx, key, kx + 7, py + 18, TEXT_MAIN);
        if (pw >= 500) {
            String close = "or ESC to close";
            text(ctx, close, kx - 8 - textRenderer.getWidth(close), py + 18, TEXT_DIM);
        }

        ctx.fill(px + 14, py + HEADER_H, px + pw - 14, py + HEADER_H + 1, a(0x22FFFFFF));
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY, int accent) {
        Draw.rrect(ctx, px + 8, py + HEADER_H + 8, px + SIDE_W, py + ph - 8, 5, a(0xFF0F1017));

        int active = searchField != null && !searchField.getText().trim().isEmpty() ? -1 : tab;
        if (indicatorY < 0) indicatorY = tabY(tab);
        indicatorY += (tabY(tab) - indicatorY) * ease();

        int tx = tabX(), tw = tabW(), th = tabH();
        int iy = Math.round(indicatorY);
        if (active >= 0) {
            glow(ctx, tx, iy, tx + tw, iy + th, 5, accent, 0x30);
            Draw.rrect(ctx, tx, iy, tx + tw, iy + th, 5, a(Draw.lerp(0xFF14151D, accent, 0.25f)));
            Draw.rrect(ctx, tx, iy + 6, tx + 3, iy + th - 6, 1, a(accent));
        }

        for (int i = 0; i < TABS.length; i++) {
            boolean hov = inside(mouseX, mouseY, tx, tabY(i), tw, th);
            if (hov && i != active) {
                Draw.rrect(ctx, tx, tabY(i), tx + tw, tabY(i) + th, 5, a(0xFF181A25));
            }
            int color = i == active ? TEXT_MAIN : (hov ? TEXT_SOFT : TEXT_DIM);
            text(ctx, TABS[i], tx + 14, tabY(i) + 10, color);
        }

        text(ctx, "v2.0", px + 20, py + ph - 24, 0xFF555A6E);
    }

    private void renderEntries(DrawContext ctx, int mouseX, int mouseY, int accent) {
        strSlot = 0;
        List<Entry> list = entries();
        int total = totalHeight(list);
        float maxScroll = Math.max(0, total - ch);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll += (scrollTarget - scroll) * ease();

        int w = cw - (maxScroll > 0 ? 8 : 0);
        boolean hot = inside(mouseX, mouseY, cx, cy, cw, ch) && drag == null;
        float since = (Util.getMeasuringTimeMs() - tabAt) / 1000f;

        ctx.enableScissor(cx, cy, cx + cw, cy + ch);
        int y = cy - Math.round(scroll);
        int idx = 0;
        for (Entry e : list) {
            if (y + e.h >= cy && y <= cy + ch) {
                float stagger = animated()
                        ? Math.max(0f, Math.min(1f, (since - Math.min(idx, 8) * 0.04f) / 0.18f))
                        : 1f;
                ea = stagger;
                int off = Math.round((1f - stagger) * 6f);
                e.render(ctx, cx, y + off, w, mouseX, mouseY, hot, accent);
            }
            y += e.h + GAP;
            idx++;
        }
        ea = 1f;
        ctx.disableScissor();

        if (maxScroll > 0) {
            int trackX = cx + cw - 4;
            Draw.rrect(ctx, trackX, cy, trackX + 3, cy + ch, 1, a(0x22FFFFFF));
            int thumbH = Math.max(18, Math.round(ch * (ch / (float) total)));
            int thumbY = cy + Math.round((ch - thumbH) * (scroll / maxScroll));
            Draw.rrect(ctx, trackX, thumbY, trackX + 3, thumbY + thumbH, 1, a(Draw.withAlpha(accent, 0xD0)));
        }
    }

    // ------------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        layout();
        double mx = click.x();
        double my = click.y();
        if (Config.getInt("ui.sounds", 0) == 1) {
            client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }

        // Text boxes first
        for (TextFieldWidget f : fields()) {
            if (f != null && shown.contains(f) && f.isMouseOver(mx, my)) {
                focusField(f);
                f.mouseClicked(click, doubled);
                return true;
            }
        }
        focusField(null);

        for (int i = 0; i < TABS.length; i++) {
            if (inside(mx, my, tabX(), tabY(i), tabW(), tabH())) {
                if (!searchField.getText().isEmpty()) searchField.setText("");
                if (tab != i) switchTab(i);
                return true;
            }
        }

        if (inside(mx, my, cx, cy, cw, ch)) {
            List<Entry> list = entries();
            int w = cw - (totalHeight(list) > ch ? 8 : 0);
            int y = cy - Math.round(scroll);
            for (Entry e : list) {
                if (e.click(mx, my, cx, y, w)) return true;
                y += e.h + GAP;
            }
            if (confirm != null) confirm = null;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void onFilesDropped(List<java.nio.file.Path> paths) {
        int n = 0;
        String last = null;
        for (java.nio.file.Path p : paths) {
            String name = dev.pvpmenu.ImageCrosshair.importFile(p);
            if (name != null) {
                n++;
                last = name;
            }
        }
        if (last != null) {
            Config.setInt(CrosshairRenderer.key(xhLook, "style"), CrosshairRenderer.STYLE_IMAGE);
            Config.setStr(CrosshairRenderer.key(xhLook, "img"), last);
            switchTab(3);
            toast("Imported " + n + " image" + (n == 1 ? "" : "s") + " as crosshair");
        } else {
            toast("Only PNG files can be used as a crosshair");
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float) verticalAmount * 28f;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (anyFocused()) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                focusField(null);
                return true;
            }
            return super.keyPressed(input);
        }
        if (PvpMenuClient.MENU_KEY.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }
}
