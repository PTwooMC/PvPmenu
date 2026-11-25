package dev.pvpmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * JSON config stored in .minecraft/config/pvpmenu.json.
 * Everything is an on/off flag or a whole number (colours are stored as ARGB numbers), so a whole setup can be
 * copied into a preset or turned into a share code.
 */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT = new Gson();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpmenu.json");
    private static final String CODE_PREFIX = "UPU1:";
    public static final int MAX_FAVORITES = 12;

    /** A copy of all settings. Used for presets and share codes. */
    public static class Snapshot {
        public Map<String, Boolean> enabled = new LinkedHashMap<>();
        public Map<String, Integer> ints = new LinkedHashMap<>();
        public Map<String, String> strs = new LinkedHashMap<>();

        public Snapshot copy() {
            Snapshot s = new Snapshot();
            s.enabled.putAll(enabled);
            s.ints.putAll(ints);
            s.strs.putAll(strs);
            return s;
        }

        void fix() {
            if (enabled == null) enabled = new LinkedHashMap<>();
            if (ints == null) ints = new LinkedHashMap<>();
            if (strs == null) strs = new LinkedHashMap<>();
        }
    }

    static class Data {
        Map<String, Boolean> enabled = new LinkedHashMap<>();
        Map<String, Integer> ints = new LinkedHashMap<>();
        Map<String, String> strs = new LinkedHashMap<>();
        Map<String, Snapshot> presets = new LinkedHashMap<>();
        List<Integer> favorites = new ArrayList<>();
    }

    private static Data data = new Data();

    private Config() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) {
                        data = d;
                        if (data.enabled == null) data.enabled = new LinkedHashMap<>();
                        if (data.ints == null) data.ints = new LinkedHashMap<>();
                        if (data.strs == null) data.strs = new LinkedHashMap<>();
                        if (data.presets == null) data.presets = new LinkedHashMap<>();
                        if (data.favorites == null) data.favorites = new ArrayList<>();
                        for (Snapshot s : data.presets.values()) if (s != null) s.fix();
                        data.presets.values().removeIf(s -> s == null);
                    }
                }
            }
        } catch (Exception e) {
            PvpMenuClient.LOGGER.warn("Could not read config, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(data));
        } catch (Exception e) {
            PvpMenuClient.LOGGER.warn("Could not save config", e);
        }
    }

    // ------------------------------------------------------------- plain settings

    public static boolean isOn(String id, boolean def) {
        return data.enabled.getOrDefault(id, def);
    }

    public static void set(String id, boolean on) {
        data.enabled.put(id, on);
        save();
    }

    public static boolean hasInt(String key) {
        return data.ints.containsKey(key);
    }

    public static int getInt(String key, int def) {
        Integer v = data.ints.get(key);
        return v == null ? def : v;
    }

    public static void setInt(String key, int value) {
        data.ints.put(key, value);
        save();
    }

    /** Sets a value without writing the file. Call {@link #save()} afterwards (used while dragging). */
    public static void setIntQuiet(String key, int value) {
        data.ints.put(key, value);
    }

    public static String getStr(String key, String def) {
        String v = data.strs.get(key);
        return v == null ? def : v;
    }

    public static void setStr(String key, String value) {
        data.strs.put(key, value);
        save();
    }

    public static void removeInt(String key) {
        data.ints.remove(key);
        save();
    }

    /** Removes every setting whose key starts with the prefix, then saves. */
    public static void removeIntsWithPrefix(String prefix) {
        data.ints.keySet().removeIf(k -> k.startsWith(prefix));
        save();
    }

    /** Copies every setting under one prefix to another prefix (replacing what was there). */
    public static void copyPrefix(String from, String to) {
        Map<String, Integer> moved = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : data.ints.entrySet()) {
            if (e.getKey().startsWith(from)) moved.put(to + e.getKey().substring(from.length()), e.getValue());
        }
        data.ints.keySet().removeIf(k -> k.startsWith(to));
        data.ints.putAll(moved);
        save();
    }

    public static int corner() { return Math.max(0, Math.min(3, getInt("ui.corner", 0))); }

    public static float scale() {
        int p = getInt("ui.scale", 100);
        return Math.max(0.5f, Math.min(2f, p / 100f));
    }

    // ------------------------------------------------------------------ snapshots

    public static Snapshot snapshot() {
        Snapshot s = new Snapshot();
        s.enabled.putAll(data.enabled);
        s.ints.putAll(data.ints);
        s.strs.putAll(data.strs);
        return s;
    }

    public static Snapshot snapshotPrefix(String prefix) {
        Snapshot s = new Snapshot();
        for (Map.Entry<String, Integer> e : data.ints.entrySet()) {
            if (e.getKey().startsWith(prefix)) s.ints.put(e.getKey(), e.getValue());
        }
        return s;
    }

    /** Replaces the whole setup. With keepUi the menu look and card positions stay as they are. */
    public static void apply(Snapshot s, boolean keepUi) {
        Map<String, Integer> keep = new LinkedHashMap<>();
        if (keepUi) {
            for (Map.Entry<String, Integer> e : data.ints.entrySet()) {
                String k = e.getKey();
                if (k.startsWith("ui.") || k.startsWith("pos.")) keep.put(k, e.getValue());
            }
        }
        data.enabled = new LinkedHashMap<>(s.enabled);
        data.ints = new LinkedHashMap<>(s.ints);
        data.ints.putAll(keep);
        data.strs = new LinkedHashMap<>(s.strs == null ? new LinkedHashMap<>() : s.strs);
        save();
    }

    /** Replaces every setting under the prefix with the ones in the snapshot. */
    public static void mergePrefix(Snapshot s, String prefix) {
        data.ints.keySet().removeIf(k -> k.startsWith(prefix));
        for (Map.Entry<String, Integer> e : s.ints.entrySet()) {
            if (e.getKey().startsWith(prefix)) data.ints.put(e.getKey(), e.getValue());
        }
        save();
    }

    public static void resetAll() {
        data.enabled.clear();
        data.ints.clear();
        data.strs.clear();
        save();
    }

    // -------------------------------------------------------------------- presets

    public static List<String> presetNames() {
        return new ArrayList<>(data.presets.keySet());
    }

    public static Snapshot getPreset(String name) {
        Snapshot s = data.presets.get(name);
        return s == null ? null : s.copy();
    }

    public static void putPreset(String name, Snapshot s) {
        data.presets.put(name, s.copy());
        save();
    }

    public static void deletePreset(String name) {
        data.presets.remove(name);
        save();
    }

    public static String uniquePresetName(String base) {
        String b = base.length() > 24 ? base.substring(0, 24) : base;
        if (!data.presets.containsKey(b)) return b;
        for (int i = 2; i < 1000; i++) {
            String n = b + " " + i;
            if (!data.presets.containsKey(n)) return n;
        }
        return b;
    }

    // ------------------------------------------------------------------ favourites

    public static List<Integer> favorites() {
        return data.favorites;
    }

    public static void addFavorite(int argb) {
        data.favorites.remove((Integer) argb);
        data.favorites.add(0, argb);
        while (data.favorites.size() > MAX_FAVORITES) data.favorites.remove(data.favorites.size() - 1);
        save();
    }

    public static void removeFavorite(int argb) {
        data.favorites.remove((Integer) argb);
        save();
    }

    // ----------------------------------------------------------------- share codes

    public static String encode(Snapshot s) {
        try {
            byte[] json = COMPACT.toJson(s).getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bo)) {
                gz.write(json);
            }
            return CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bo.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    /** Returns null when the text is not a valid code. */
    public static Snapshot decode(String code) {
        try {
            String c = code == null ? "" : code.trim();
            if (!c.startsWith(CODE_PREFIX)) return null;
            byte[] raw = Base64.getUrlDecoder().decode(c.substring(CODE_PREFIX.length()));
            try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                String json = new String(gz.readNBytes(1_000_000), StandardCharsets.UTF_8);
                Snapshot s = COMPACT.fromJson(json, Snapshot.class);
                if (s == null) return null;
                s.fix();
                return s;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
