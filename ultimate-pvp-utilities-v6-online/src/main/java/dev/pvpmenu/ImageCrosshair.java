package dev.pvpmenu;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * PNG crosshairs. Images live in config/pvpmenu/crosshairs. Each image is scaled to the chosen size and drawn as
 * runs of same-coloured pixels, so it needs no texture uploads and cannot break rendering.
 */
public final class ImageCrosshair {
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("pvpmenu").resolve("crosshairs");
    private static final Map<String, int[]> CACHE = new HashMap<>();

    private ImageCrosshair() {}

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(DIR);
            try (Stream<Path> s = Files.list(DIR)) {
                s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .map(p -> p.getFileName().toString()).sorted().limit(24).forEach(out::add);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /** Copies a dropped PNG into the crosshair folder. Returns the stored file name, or null. */
    public static String importFile(Path src) {
        try {
            String n = src.getFileName().toString();
            if (!n.toLowerCase(Locale.ROOT).endsWith(".png")) return null;
            if (Files.size(src) > 4_000_000) return null;
            Files.createDirectories(DIR);
            n = n.replaceAll("[^A-Za-z0-9._-]", "_");
            Files.copy(src, DIR.resolve(n), StandardCopyOption.REPLACE_EXISTING);
            CACHE.clear();
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    /** ARGB pixels, size x size, or null if the image cannot be read. */
    private static int[] pixels(String name, int size) {
        String k = name + "|" + size;
        if (CACHE.containsKey(k)) return CACHE.get(k);
        int[] out = null;
        try {
            Path f = DIR.resolve(name).normalize();
            if (!name.isEmpty() && f.startsWith(DIR) && Files.exists(f)) {
                BufferedImage img = ImageIO.read(f.toFile());
                if (img != null) {
                    int w = img.getWidth();
                    int h = img.getHeight();
                    float scale = Math.min(size / (float) w, size / (float) h);
                    int ow = Math.max(1, Math.round(w * scale));
                    int oh = Math.max(1, Math.round(h * scale));
                    out = new int[2 + ow * oh];
                    out[0] = ow;
                    out[1] = oh;
                    for (int y = 0; y < oh; y++) {
                        for (int x = 0; x < ow; x++) {
                            // average a small block so downscaled images stay readable
                            int x0 = x * w / ow, x1 = Math.max(x0 + 1, (x + 1) * w / ow);
                            int y0 = y * h / oh, y1 = Math.max(y0 + 1, (y + 1) * h / oh);
                            long a = 0, r = 0, g = 0, b = 0;
                            int c = 0;
                            for (int yy = y0; yy < y1 && yy < h; yy++) {
                                for (int xx = x0; xx < x1 && xx < w; xx++) {
                                    int p = img.getRGB(xx, yy);
                                    int pa = (p >>> 24) & 0xFF;
                                    a += pa;
                                    r += ((p >> 16) & 0xFF) * pa;
                                    g += ((p >> 8) & 0xFF) * pa;
                                    b += (p & 0xFF) * pa;
                                    c++;
                                }
                            }
                            if (c == 0 || a == 0) {
                                out[2 + y * ow + x] = 0;
                            } else {
                                out[2 + y * ow + x] = ((int) (a / c) << 24) | ((int) (r / a) << 16) | ((int) (g / a) << 8) | (int) (b / a);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            out = null;
        }
        if (CACHE.size() > 40) CACHE.clear();
        CACHE.put(k, out);
        return out;
    }

    /** Draws the image centred on (0, 0) of the current matrix. */
    public static void draw(DrawContext ctx, String name, int size, boolean tint, int tintArgb, int opacity8) {
        int[] px = pixels(name, size);
        if (px == null) return;
        int w = px[0];
        int h = px[1];
        int x0 = -w / 2;
        int y0 = -h / 2;
        for (int y = 0; y < h; y++) {
            int x = 0;
            while (x < w) {
                int c = px[2 + y * w + x];
                int e = x + 1;
                while (e < w && px[2 + y * w + e] == c) e++;
                int a = (c >>> 24) & 0xFF;
                if (a > 8) {
                    int al = a * opacity8 / 255;
                    int rgb = tint ? (tintArgb & 0xFFFFFF) : (c & 0xFFFFFF);
                    ctx.fill(x0 + x, y0 + y, x0 + e, y0 + y + 1, (al << 24) | rgb);
                }
                x = e;
            }
        }
    }
}
