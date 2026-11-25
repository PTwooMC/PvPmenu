package dev.pvpmenu.module;

import dev.pvpmenu.Config;
import dev.pvpmenu.Guard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * One entry in the menu. HUD modules (category "HUD") also draw a card on screen.
 * Modules with {@link #levels()} get a row of chips in the menu to pick a strength.
 */
public abstract class Module {
    public final String id;
    public final String name;
    public final String description;
    public final String category;
    private final boolean defaultOn;

    protected Module(String id, String name, String description, String category, boolean defaultOn) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultOn = defaultOn;
    }

    public boolean isEnabled() {
        return !Guard.tripped(id) && Config.isOn(id, defaultOn);
    }

    public void toggle() {
        if (Guard.tripped(id)) {
            // A click on a module that was switched off by crash protection tries it again
            Guard.reset(id);
            Config.set(id, true);
            return;
        }
        Config.set(id, !Config.isOn(id, defaultOn));
    }

    public boolean isHud() {
        return "HUD".equals(category);
    }

    // ---- extra settings, notes and helpers

    /** Extra settings shown in the menu under this module. */
    public List<Opt> options() { return List.of(); }

    /** A short warning shown under the module, for example the server rules reminder. */
    public String note() { return null; }

    /** Words that identify a mod doing the same thing (used by duplicate detection). */
    public String[] dupeKeywords() { return null; }

    /** HUD modules can hide themselves (for example Target Info when nothing is targeted). */
    public boolean visible(MinecraftClient mc) { return true; }

    public String optKey(String name) { return "mo." + id + "." + name; }

    public int optOf(String name, int def) { return opt(name, def); }

    public boolean flagOf(String name, boolean def) { return flag(name, def); }

    public String strOf(String name, String def) { return Config.getStr(optKey(name), def); }

    protected String str(String name, String def) { return Config.getStr(optKey(name), def); }

    /** The card title, which the user can rename in the menu. */
    public String title() {
        String t = Config.getStr("card." + id + ".title", "");
        return t.isEmpty() ? name : t;
    }

    protected int opt(String name, int def) { return Config.getInt(optKey(name), def); }

    protected boolean flag(String name, boolean def) { return Config.getInt(optKey(name), def ? 1 : 0) == 1; }

    // ---- optional strength levels (used by the Visuals modules)

    public String[] levels() { return null; }
    public String levelLabel() { return "Strength"; }
    public int defaultLevel() { return 1; }

    public int level() {
        String[] l = levels();
        if (l == null) return 0;
        int v = Config.getInt(id + ".level", defaultLevel());
        return Math.max(0, Math.min(l.length - 1, v));
    }

    public void setLevel(int i) {
        Config.setInt(id + ".level", i);
    }

    // ---- HUD card content (only used when isHud())

    /** Height of the content area in GUI pixels. */
    public int contentHeight(MinecraftClient mc) { return 0; }

    /** Draw the content. (x, y) is the top-left of the content area, w is its width. */
    public void renderContent(DrawContext ctx, MinecraftClient mc, int x, int y, int w) {}

    protected static int healthColor(float fraction) {
        if (fraction > 0.6f) return 0xFF4ADE80;
        if (fraction > 0.3f) return 0xFFFACC15;
        return 0xFFF87171;
    }
}
