package dev.pvpmenu.gui;

import dev.pvpmenu.Config;

/** Menu and HUD colour themes. Each theme is two accent colours (used as a gradient) and the panel tint. */
public final class Themes {
    public record Theme(String name, int a, int b, int top, int bottom) {}

    public static final Theme[] LIST = {
            new Theme("Violet", 0xFF7C5CFF, 0xFFC25CFF, 0xFF161825, 0xFF0E0F16),
            new Theme("Ocean", 0xFF00B4FF, 0xFF00FFD0, 0xFF131A24, 0xFF0C1016),
            new Theme("Sunset", 0xFFFF6A3D, 0xFFFF3D8B, 0xFF1D1620, 0xFF120E14),
            new Theme("Forest", 0xFF39D98A, 0xFFB6F23A, 0xFF131C19, 0xFF0C120F),
            new Theme("Rose", 0xFFFF4D8D, 0xFFFF9AC0, 0xFF1D1520, 0xFF120E14),
            new Theme("Ember", 0xFFFFB020, 0xFFFF4D4D, 0xFF1D1A14, 0xFF12100C),
            new Theme("Ice", 0xFF7CD4FF, 0xFFB8A6FF, 0xFF151A26, 0xFF0D1018),
            new Theme("Midnight", 0xFF4D7CFF, 0xFF7C5CFF, 0xFF121624, 0xFF0A0C14),
            new Theme("Mono", 0xFFE6E8F5, 0xFF8A8FA3, 0xFF181A20, 0xFF101116)
    };

    public static final String[] NAMES;

    static {
        NAMES = new String[LIST.length + 1];
        for (int i = 0; i < LIST.length; i++) NAMES[i] = LIST[i].name();
        NAMES[LIST.length] = "Custom";
    }

    private Themes() {}

    public static int index() {
        return Math.max(0, Math.min(LIST.length, Config.getInt("ui.theme", 0)));
    }

    public static boolean custom() {
        return index() == LIST.length;
    }

    public static int accA() {
        return custom() ? (Config.getInt("ui.accA", 0xFF7C5CFF) | 0xFF000000) : LIST[index()].a();
    }

    /** Second gradient colour. Same as the first when gradients are switched off. */
    public static int accB() {
        if (Config.getInt("ui.grad", 1) == 0) return accA();
        return custom() ? (Config.getInt("ui.accB", 0xFF00E0FF) | 0xFF000000) : LIST[index()].b();
    }

    public static int top() {
        return custom() ? Draw.lerp(0xFF14151E, accA(), 0.08f) : LIST[index()].top();
    }

    public static int bottom() {
        return custom() ? 0xFF0E0F16 : LIST[index()].bottom();
    }
}
