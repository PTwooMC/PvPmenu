package dev.pvpmenu;

import dev.pvpmenu.module.Module;
import dev.pvpmenu.module.Modules;

/** The built-in setups. Loading one keeps your menu theme and card positions. */
public final class Presets {
    public static final String[] BUILTIN = {"Competitive", "Minimal", "Everything"};
    public static final String[] BUILTIN_INFO = {
            "Core HUD cards, CPS, keystrokes, lowered shield and fire, small totem pop and a clean crosshair.",
            "Only the attack cooldown card. Everything else off.",
            "Every module except the ones that need a server-rules check (they stay off)."
    };

    private static final java.util.Set<String> COMPETITIVE = java.util.Set.of(
            "cooldown", "armor", "totems", "lowshield", "lowfire", "smalltotem", "crosshair",
            "cps", "keystrokes", "appleskin", "shulker", "tnt", "shieldfix", "hotbarkeys", "quietexplosions", "fewtotem");

    private Presets() {}

    public static Config.Snapshot build(int i) {
        Config.Snapshot s = new Config.Snapshot();
        for (Module m : Modules.ALL) {
            boolean rules = m.note() != null;
            boolean on = switch (i) {
                case 0 -> !rules && COMPETITIVE.contains(m.id);
                case 1 -> m.id.equals("cooldown");
                default -> !rules;
            };
            s.enabled.put(m.id, on);
        }
        if (i == 0) CrosshairRenderer.presetValues(0, s.ints);
        return s;
    }
}
