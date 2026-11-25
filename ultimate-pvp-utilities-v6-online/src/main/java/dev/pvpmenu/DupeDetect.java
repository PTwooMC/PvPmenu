package dev.pvpmenu;

import dev.pvpmenu.module.Module;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finds other installed mods that do the same job as one of our modules. Nothing is ever switched off
 * automatically: the menu only shows a notice, and you choose what to do.
 */
public final class DupeDetect {
    public static final class Other {
        public final String id;
        public final String name;
        public final Path jar;

        Other(String id, String name, Path jar) {
            this.id = id;
            this.name = name;
            this.jar = jar;
        }
    }

    private static final Map<String, Other> CACHE = new HashMap<>();
    private static final List<Path> PENDING = new ArrayList<>();
    private static boolean hook;

    private DupeDetect() {}

    /** The other mod that overlaps with this module, or null. */
    public static Other find(Module m) {
        String[] words = m.dupeKeywords();
        if (words == null) return null;
        if (CACHE.containsKey(m.id)) return CACHE.get(m.id);
        Other found = null;
        for (ModContainer c : FabricLoader.getInstance().getAllMods()) {
            String id = c.getMetadata().getId();
            if (id.equals(PvpMenuClient.MOD_ID) || id.equals("minecraft") || id.equals("java")
                    || id.startsWith("fabric") || id.equals("mixinextras")) continue;
            String hay = (id + " " + c.getMetadata().getName()).toLowerCase(Locale.ROOT);
            for (String w : words) {
                if (hay.contains(w.toLowerCase(Locale.ROOT))) {
                    found = new Other(id, c.getMetadata().getName(), jarOf(c));
                    break;
                }
            }
            if (found != null) break;
        }
        CACHE.put(m.id, found);
        return found;
    }

    private static Path jarOf(ModContainer c) {
        try {
            for (Path p : c.getOrigin().getPaths()) {
                if (Files.isRegularFile(p) && p.toString().endsWith(".jar")) return p;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean pending(Other o) {
        return o.jar != null && PENDING.contains(o.jar);
    }

    /** Marks the other mod to be renamed to .jar.disabled once the game closes. Click again to cancel. */
    public static void toggleDisableOther(Other o) {
        if (o.jar == null) return;
        if (PENDING.contains(o.jar)) {
            PENDING.remove(o.jar);
            return;
        }
        PENDING.add(o.jar);
        if (!hook) {
            hook = true;
            Runtime.getRuntime().addShutdownHook(new Thread(DupeDetect::renamePending, "pvpmenu-disable-other"));
        }
    }

    private static void renamePending() {
        for (Path jar : PENDING) {
            Path target = jar.resolveSibling(jar.getFileName() + ".disabled");
            try {
                Files.move(jar, target);
            } catch (Throwable t) {
                // The jar is still locked by the running game (Windows). Rename it a moment after we exit.
                try {
                    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                    ProcessBuilder pb = os.contains("win")
                            ? new ProcessBuilder("cmd", "/c", "ping -n 4 127.0.0.1 >nul & move /Y \"" + jar + "\" \"" + target + "\"")
                            : new ProcessBuilder("sh", "-c", "sleep 3; mv \"" + jar + "\" \"" + target + "\"");
                    pb.start();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
