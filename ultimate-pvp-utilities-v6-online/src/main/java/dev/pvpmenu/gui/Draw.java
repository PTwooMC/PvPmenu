package dev.pvpmenu.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

/** Small drawing helpers: rounded rectangles, gradients and colour maths. */
public final class Draw {
    /** Quick colour row shown in every colour picker. */
    public static final int[] PALETTE = {
            0xFFFFFFFF, 0xFFFF4D4D, 0xFFFF9F43, 0xFFFACC15, 0xFF4ADE80,
            0xFF00E0FF, 0xFF4D7CFF, 0xFF7C5CFF, 0xFFFF4D8D, 0xFF111111
    };

    private Draw() {}

    public static int accent() {
        return Themes.accA();
    }

    public static int accent2() {
        return Themes.accB();
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    /** Hue 0-360, saturation and value 0-1. Returns opaque ARGB. */
    public static int hsv(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f;
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        int ri = Math.round((r + m) * 255f);
        int gi = Math.round((g + m) * 255f);
        int bi = Math.round((b + m) * 255f);
        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    /** Returns {hue 0-360, saturation 0-1, value 0-1}. */
    public static float[] rgbToHsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0f) h = 0f;
        else if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * (((b - r) / d) + 2f);
        else h = 60f * (((r - g) / d) + 4f);
        if (h < 0) h += 360f;
        return new float[]{h, max == 0f ? 0f : d / max, max};
    }

    /** A slowly cycling rainbow colour. */
    public static int rainbow() {
        float hue = (Util.getMeasuringTimeMs() / 12L) % 360L;
        return hsv(hue, 0.85f, 1f);
    }

    /** Filled rounded rectangle, x2/y2 exclusive. */
    public static void rrect(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) {
            c.fill(x1, y1, x2, y2, color);
            return;
        }
        c.fill(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x1 + dx, y1 + i, x2 - dx, y1 + i + 1, color);
            c.fill(x1 + dx, y2 - i - 1, x2 - dx, y2 - i, color);
        }
    }

    public static void vgrad(DrawContext c, int x1, int y1, int x2, int y2, int top, int bottom) {
        int h = y2 - y1;
        if (h <= 0) return;
        for (int i = 0; i < h; i++) {
            c.fill(x1, y1 + i, x2, y1 + i + 1, lerp(top, bottom, h == 1 ? 0f : i / (float) (h - 1)));
        }
    }

    public static void hgrad(DrawContext c, int x1, int y1, int x2, int y2, int left, int right) {
        int w = x2 - x1;
        if (w <= 0) return;
        if (left == right) {
            c.fill(x1, y1, x2, y2, left);
            return;
        }
        for (int i = 0; i < w; i++) {
            c.fill(x1 + i, y1, x1 + i + 1, y2, lerp(left, right, w == 1 ? 0f : i / (float) (w - 1)));
        }
    }

    /** Grey checkerboard, drawn behind colours that can be see-through. */
    public static void checker(DrawContext c, int x1, int y1, int x2, int y2) {
        c.fill(x1, y1, x2, y2, 0xFF2A2C38);
        for (int y = y1; y < y2; y += 4) {
            for (int x = x1 + (((y - y1) / 4) % 2 == 0 ? 0 : 4); x < x2; x += 8) {
                c.fill(x, y, Math.min(x2, x + 4), Math.min(y2, y + 4), 0xFF3C3F50);
            }
        }
    }
}
