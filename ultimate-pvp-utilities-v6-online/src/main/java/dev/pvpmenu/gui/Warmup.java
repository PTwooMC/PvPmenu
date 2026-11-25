package dev.pvpmenu.gui;

import dev.pvpmenu.Config;

/** Settings and saved best scores for the warm-up tools. */
public final class Warmup {
    public static final String[] LEVELS = {"Custom", "Level 1", "Level 2", "Level 3", "Level 4", "Level 5"};
    public static final String[] DURATIONS = {"15 s", "30 s", "60 s"};
    public static final int[] DURATION_S = {15, 30, 60};
    public static final String[] SHAPES = {"Circle", "Square", "Ring"};
    public static final String[] MOVING = {"Still", "Slow", "Medium", "Fast"};

    // size, lifetime ms (0 = stays), targets at once, moving 0..3
    private static final int[][] LEVEL_VALUES = {
            {0, 0, 0, 0},
            {40, 2600, 1, 0},
            {32, 2000, 2, 0},
            {26, 1600, 2, 1},
            {20, 1300, 3, 2},
            {15, 1000, 3, 3},
    };

    private Warmup() {}

    public static int level() { return Math.max(0, Math.min(5, Config.getInt("wu.aim.level", 2))); }

    public static int size() {
        int l = level();
        return l == 0 ? Config.getInt("wu.aim.size", 28) : LEVEL_VALUES[l][0];
    }

    /** How long a target stays, in milliseconds. 0 means it stays until you hit it. */
    public static int lifetimeMs() {
        int l = level();
        if (l != 0) return LEVEL_VALUES[l][1];
        int v = Config.getInt("wu.aim.life", 18);
        return v >= 30 ? 0 : v * 100;
    }

    public static int targets() {
        int l = level();
        return l == 0 ? Config.getInt("wu.aim.count", 2) : LEVEL_VALUES[l][2];
    }

    public static int moving() {
        int l = level();
        return l == 0 ? Config.getInt("wu.aim.move", 0) : LEVEL_VALUES[l][3];
    }

    public static int durationSeconds() {
        return DURATION_S[Math.max(0, Math.min(2, Config.getInt("wu.aim.dur", 1)))];
    }

    public static int shape() { return Math.max(0, Math.min(2, Config.getInt("wu.aim.shape", 0))); }
    public static int targetColor() { return Config.getInt("wu.aim.color", 0xFFFF5C7A) | 0xFF000000; }
    public static int dim() { return Config.getInt("wu.aim.dim", 55); }

    public static int rounds() { return Math.max(1, Config.getInt("wu.react.rounds", 5)); }
    public static int minWaitMs() { return Config.getInt("wu.react.min", 1) * 1000; }
    public static int maxWaitMs() { return Math.max(minWaitMs() + 500, Config.getInt("wu.react.max", 4) * 1000); }
    public static int waitColor() { return Config.getInt("wu.react.wait", 0xFFB91C1C) | 0xFF000000; }
    public static int goColor() { return Config.getInt("wu.react.go", 0xFF16A34A) | 0xFF000000; }

    public static int reactBest() { return Config.getInt("wu.react.best", 0); }
    public static int aimBest(int level) { return Config.getInt("wu.aim.best." + level, 0); }

    public static final String[] CLICK_LENGTHS = {"5 s", "10 s", "30 s"};
    public static final int[] CLICK_S = {5, 10, 30};
    public static int clickSeconds() { return CLICK_S[Math.max(0, Math.min(2, Config.getInt("wu.click.len", 1)))]; }

    public static final String[] TRACK_SPEEDS = {"Slow", "Medium", "Fast"};
    public static int trackSeconds() { return DURATION_S[Math.max(0, Math.min(2, Config.getInt("wu.track.len", 1)))]; }
    public static int trackSize() { return Config.getInt("wu.track.size", 22); }
    public static float trackSpeed() { return new float[]{80f, 160f, 280f}[Math.max(0, Math.min(2, Config.getInt("wu.track.speed", 1)))]; }
}
