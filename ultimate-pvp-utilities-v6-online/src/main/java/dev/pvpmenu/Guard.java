package dev.pvpmenu;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Crash protection. Wrap a module's drawing code in {@link #run}. If it throws, the error is logged once and
 * after three failures that module is switched off for the rest of the session instead of crashing the game.
 * The menu shows it as "turned off after errors" and a click on its switch tries again.
 */
public final class Guard {
    private static final Map<String, Integer> FAILS = new HashMap<>();
    private static final Set<String> TRIPPED = new LinkedHashSet<>();

    private Guard() {}

    public static boolean tripped(String id) {
        return TRIPPED.contains(id);
    }

    public static boolean anyTripped() {
        return !TRIPPED.isEmpty();
    }

    public static void reset(String id) {
        TRIPPED.remove(id);
        FAILS.remove(id);
    }

    public static void resetAll() {
        TRIPPED.clear();
        FAILS.clear();
    }

    public static void run(String id, Runnable r) {
        if (TRIPPED.contains(id)) return;
        try {
            r.run();
        } catch (Throwable t) {
            fail(id, t);
        }
    }

    public static void fail(String id, Throwable t) {
        int n = FAILS.merge(id, 1, Integer::sum);
        if (n == 1) PvpMenuClient.LOGGER.warn("Module '{}' threw an error", id, t);
        if (n >= 3 && TRIPPED.add(id)) {
            PvpMenuClient.LOGGER.warn("Module '{}' was switched off after repeated errors", id);
        }
    }
}
