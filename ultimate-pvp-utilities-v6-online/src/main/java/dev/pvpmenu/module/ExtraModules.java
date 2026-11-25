package dev.pvpmenu.module;

import java.util.List;

/** The visual tweaks, the rebuilt community mods and the small tools. */
public final class ExtraModules {
    public static final String RULES = "Check your server's rules before using this.";

    private ExtraModules() {}

    /** A module without a card whose behaviour is implemented elsewhere (mixins, events, key handlers). */
    static class Plain extends Module {
        private final String[] levels;
        private final String levelLabel;
        private final int defaultLevel;
        private final String note;
        private final List<Opt> opts;
        private final String[] dupes;

        Plain(String id, String name, String desc, String category, boolean on,
              String[] levels, String levelLabel, int defaultLevel, String note, List<Opt> opts, String... dupes) {
            super(id, name, desc, category, on);
            this.levels = levels;
            this.levelLabel = levelLabel;
            this.defaultLevel = defaultLevel;
            this.note = note;
            this.opts = opts;
            this.dupes = dupes.length == 0 ? null : dupes;
        }

        @Override public String[] levels() { return levels; }
        @Override public String levelLabel() { return levelLabel; }
        @Override public int defaultLevel() { return defaultLevel; }
        @Override public String note() { return note; }
        @Override public List<Opt> options() { return opts; }
        @Override public String[] dupeKeywords() { return dupes; }
    }

    private static final String[] NONE = null;

    // ----------------------------------------------------------------- Visuals

    static Module fullbright() {
        return new Plain("fullbright", "Fullbright", "Makes dark areas bright so you can see everything.", "Visuals", false,
                new String[]{"Bright", "Brighter", "Max"}, "Level", 2, RULES, List.of(), "fullbright", "gamma");
    }

    static Module lava() {
        return new Plain("lavavision", "See Through Lava", "Clear view while swimming in lava instead of the thick orange fog.", "Visuals", false,
                NONE, "", 0, RULES, List.of());
    }

    static Module clearWater() {
        return new Plain("clearwater", "Clear Water", "Removes the blue underwater fog so you can see far.", "Visuals", false,
                NONE, "", 0, RULES, List.of());
    }

    static Module noFog() {
        return new Plain("nofog", "No Fog", "Removes the distance haze so far away terrain stays sharp.", "Visuals", false,
                NONE, "", 0, RULES, List.of());
    }

    static Module quietExplosions() {
        return new Plain("quietexplosions", "Quieter Explosions", "Turns the explosion sound down.", "Visuals", false,
                new String[]{"Softer", "Quiet", "Very quiet"}, "Volume", 1, null, List.of());
    }

    static Module smallExplosions() {
        return new Plain("smallexplosions", "Smaller Explosions", "Shows fewer explosion puffs so the screen stays clear.", "Visuals", false,
                new String[]{"Fewer", "Much fewer", "Almost none"}, "Amount", 1, RULES, List.of());
    }

    static Module fewTotemParticles() {
        return new Plain("fewtotem", "Fewer Totem Particles", "Cuts the green particle burst when a totem pops.", "Visuals", false,
                new String[]{"Fewer", "Much fewer", "Almost none"}, "Amount", 1, null, List.of());
    }

    static Module hideParticles() {
        return new Plain("hideparticles", "Hide Particles", "Removes some or all particles to clean up the screen.", "Visuals", false,
                new String[]{"Some", "Most", "All"}, "Amount", 1, RULES, List.of());
    }

    static Module handPosition() {
        return new Plain("handpos", "Hand and Item Position", "Move and resize the item you hold in first person.", "Visuals", false,
                NONE, "", 0, null, List.of(
                Opt.slider("Left / right", "x", -50, 50, 0, ""),
                Opt.slider("Up / down", "y", -50, 50, 0, ""),
                Opt.slider("Closer / farther", "z", -50, 50, 0, ""),
                Opt.slider("Size", "size", 50, 150, 100, "%")));
    }

    static Module noEquip() {
        return new Plain("noequip", "No Item Switch Animation", "Held items appear instantly and do not dip when you attack.", "Visuals", false,
                NONE, "", 0, null, List.of());
    }

    static Module fastSwing() {
        return new Plain("fastswing", "Faster Swing Animation", "Shortens the visual swing of your arm. Hit timing does not change.", "Visuals", false,
                new String[]{"Slightly", "Faster", "Fastest"}, "Speed", 1, null, List.of());
    }

    // -------------------------------------------------------------------- Mods

    static Module appleSkin() {
        return new Plain("appleskin", "Food and Saturation Display", "Saturation bar, a preview of what held food restores, and the numbers, on your hunger bar.", "Mods", false,
                NONE, "", 0, null, List.of(
                Opt.onOff("Saturation bar", "saturation", true),
                Opt.onOff("Hunger preview for held food", "preview", true),
                Opt.onOff("Numbers", "numbers", true),
                Opt.onOff("Food tooltip", "tooltip", true)), "appleskin");
    }

    static Module shulker() {
        return new Plain("shulker", "Container Preview", "See inside shulker boxes and your ender chest by hovering them.", "Mods", false,
                NONE, "", 0, null, List.of(
                Opt.onOff("Ender chest preview", "ender", true),
                Opt.onOff("Only while holding Shift", "shift", false)), "shulkerboxtooltip", "shulker box tooltip");
    }

    static Module tnt() {
        return new Plain("tnt", "TNT Fuse Countdown", "Time left floating over lit TNT.", "Mods", false,
                NONE, "", 0, null, List.of(
                Opt.slider("Range", "range", 8, 64, 40, " blocks"),
                Opt.onOff("Show distance", "dist", true)), "tnttimer", "tnt timer");
    }

    static Module shieldFix() {
        return new Plain("shieldfix", "Shield Sound Control", "Softer or muted shield block and break sounds.", "Mods", false,
                new String[]{"Softer", "Quiet", "Muted"}, "Sounds", 0, null, List.of(), "shieldfixes", "shield fixes");
    }

    static Module shieldStatus() {
        return new Plain("shieldstatus", "Shield State Indicator", "Tints your hotbar shield: ready, raised or disabled.", "Mods", false,
                NONE, "", 0, null, List.of(
                Opt.onOff("Tint hotbar shield slots", "slots", true),
                Opt.slider("Tint strength", "tint", 10, 90, 45, "%"),
                Opt.color("Ready color", "rdycol", 0xFF9CA3AF),
                Opt.color("Raised color", "upcol", 0xFF4ADE80),
                Opt.color("Disabled color", "cdcol", 0xFFF87171),
                Opt.onOff("Text under crosshair", "text", false),
                Opt.slider("Text height", "dy", -60, 80, 26, " px")), "shieldstatus", "shield status");
    }

    static Module hotbarKeys() {
        return new Plain("hotbarkeys", "Slot Key Labels", "Shows the key for each hotbar slot, on the hotbar and in inventories.", "Mods", false,
                NONE, "", 0, null, List.of(
                Opt.onOff("On the hotbar", "hotbar", true),
                Opt.onOff("In inventories", "inventory", true),
                Opt.slider("Label size", "size", 50, 100, 75, "%"),
                Opt.color("Label color", "color", 0xFFFFFFFF)), "hotbarkeys", "hotbar keys");
    }

    static Module freelook() {
        return new Plain("freelook", "Freelook", "Hold a key to look around without turning your character.", "Mods", false,
                NONE, "", 0, RULES, List.of(
                Opt.choice("Key mode", "mode", 0, "Hold", "Toggle"),
                Opt.onOff("Switch to third person", "third", true)), "perspective", "freelook");
    }

    static Module zoom() {
        return new Plain("zoom", "Zoom", "Hold a key to zoom in.", "Mods", false,
                NONE, "", 0, null, List.of(
                Opt.slider("Zoom amount (x10)", "amount", 15, 100, 40, ""),
                Opt.onOff("Smooth zoom", "smooth", true)), "ok_zoomer", "zoomify", "ok zoomer");
    }

    static Module toggles() {
        return new Plain("toggles", "Toggle Sprint and Sneak", "Tap once to sprint or sneak instead of holding the key.", "Mods", false,
                NONE, "", 0, RULES, List.of(
                Opt.onOff("Toggle sprint", "sprint", true),
                Opt.onOff("Toggle sneak", "sneak", false)), "togglesprint", "toggle sprint");
    }
}
