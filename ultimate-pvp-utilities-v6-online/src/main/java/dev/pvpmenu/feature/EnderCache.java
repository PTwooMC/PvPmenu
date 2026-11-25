package dev.pvpmenu.feature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.JsonOps;
import dev.pvpmenu.PvpMenuClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers what was in your ender chest the last time you opened it, per server or world, so the preview
 * works without opening it again. Only what the game already showed you is stored, on your own computer.
 */
public final class EnderCache {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpmenu-enderchest.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, List<ItemStack>> MEMORY = new HashMap<>();
    private static Map<String, List<JsonElement>> disk;
    private static final Map<String, Boolean> LOADED = new HashMap<>();

    private EnderCache() {}

    public static String key(MinecraftClient mc) {
        try {
            ServerInfo si = mc.getCurrentServerEntry();
            if (si != null && si.address != null) return "server:" + si.address;
        } catch (Throwable ignored) {
        }
        return "singleplayer";
    }

    /** Stores the 27 slots. Returns quietly if nothing changed. */
    public static void store(MinecraftClient mc, List<ItemStack> stacks) {
        String k = key(mc);
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack s : stacks) copy.add(s.copy());
        List<ItemStack> old = MEMORY.get(k);
        if (old != null && old.size() == copy.size()) {
            boolean same = true;
            for (int i = 0; i < copy.size(); i++) {
                if (!ItemStack.areEqual(old.get(i), copy.get(i))) { same = false; break; }
            }
            if (same) return;
        }
        MEMORY.put(k, copy);
        LOADED.put(k, true);
        saveToDisk(mc, k, copy);
    }

    /** The remembered contents, or null if this server or world has none yet. */
    public static List<ItemStack> get(MinecraftClient mc) {
        String k = key(mc);
        List<ItemStack> m = MEMORY.get(k);
        if (m != null) return m;
        if (LOADED.containsKey(k) || mc.world == null) return null;
        LOADED.put(k, true);
        try {
            readDisk();
            List<JsonElement> raw = disk.get(k);
            if (raw == null) return null;
            var ops = mc.world.getRegistryManager().getOps(JsonOps.INSTANCE);
            List<ItemStack> out = new ArrayList<>();
            for (JsonElement el : raw) {
                out.add(ItemStack.OPTIONAL_CODEC.parse(ops, el).result().orElse(ItemStack.EMPTY));
            }
            MEMORY.put(k, out);
            return out;
        } catch (Throwable t) {
            PvpMenuClient.LOGGER.warn("Could not read the ender chest cache", t);
            return null;
        }
    }

    private static void readDisk() throws Exception {
        if (disk != null) return;
        disk = new HashMap<>();
        if (Files.exists(FILE)) {
            Map<String, List<JsonElement>> m = GSON.fromJson(Files.readString(FILE),
                    new TypeToken<Map<String, List<JsonElement>>>() {}.getType());
            if (m != null) disk = m;
        }
    }

    private static void saveToDisk(MinecraftClient mc, String k, List<ItemStack> stacks) {
        try {
            if (mc.world == null) return;
            readDisk();
            var ops = mc.world.getRegistryManager().getOps(JsonOps.INSTANCE);
            List<JsonElement> raw = new ArrayList<>();
            for (ItemStack s : stacks) {
                raw.add(ItemStack.OPTIONAL_CODEC.encodeStart(ops, s).result().orElseThrow());
            }
            disk.put(k, raw);
            Files.writeString(FILE, GSON.toJson(disk));
        } catch (Throwable t) {
            PvpMenuClient.LOGGER.warn("Could not save the ender chest cache (it is still kept for this session)", t);
        }
    }
}
