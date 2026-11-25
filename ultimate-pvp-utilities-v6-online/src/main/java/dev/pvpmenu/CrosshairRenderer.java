package dev.pvpmenu;

import dev.pvpmenu.gui.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The custom crosshair. There are six "looks", each a full crosshair setup of its own:
 * 0 Default (nothing in reach), 1 Entity in reach, and optional looks for a fresh hit, a low attack cooldown,
 * sprinting and a drawn bow. Which look shows is decided from information the game already has (what the
 * vanilla crosshair is pointing at, your own cooldown, sprint and bow state). Display only, nothing here
 * changes gameplay.
 */
public final class CrosshairRenderer {
    public static final String[] STYLES = {
            "Cross", "T-Cross", "X", "Dot", "Circle", "Square", "Diamond", "Brackets",
            "Chevron", "Triangle", "Ring + Cross", "Dashed", "Pixel", "Image"
    };
    public static final int STYLE_DOT = 3;
    public static final int STYLE_PIXEL = 12;
    public static final int STYLE_IMAGE = 13;

    public static final String[] LOOKS = {"Default", "Entity in reach", "On hit", "Low cooldown", "Sprinting", "Bow drawn"};
    public static final String[] ANIMS = {"None", "Pulse", "Spin"};
    public static final String[] HIT_STYLES = {"Ticks", "X", "Plus", "Ring"};
    public static final String[] OUTLINE_STYLES = {"Solid", "Soft"};
    public static final String[] TARGET_FILTERS = {"Any entity", "Players only", "Mobs only"};
    public static final String[] PRESETS = {"Competitive", "Minimal", "Dot", "Chunky", "Ring", "Reset"};
    public static final int PIXELS = 11;

    private static final long HIT_FADE_MS = 320;

    /** Smoothed colour and movement state. One for the game, a few more for the menu previews. */
    public static final class State {
        float r, g, b;
        float spread;
        boolean init;
        long last;
    }

    public static final State LIVE = new State();

    /** Time of your last hit on a living entity. */
    private static long hitAt = -100000;

    private CrosshairRenderer() {}

    // ----------------------------------------------------------------- settings

    private static int clampI(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
    private static float clampF(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    public static String key(int look, String name) {
        return "xh." + look + "." + name;
    }

    private static int gi(int look, String name, int def) {
        return Config.getInt(key(look, name), def);
    }

    public static int gg(String name, int def) {
        return Config.getInt("xh.g." + name, def);
    }

    /** Default crosshair colour for a look. */
    public static int defColor(int look) {
        return switch (look) {
            case 1 -> 0xFFFF4D4D;
            case 2 -> 0xFFFACC15;
            case 3 -> 0xFFFF9F43;
            case 4 -> 0xFF00E0FF;
            case 5 -> 0xFF7C5CFF;
            default -> 0xFFFFFFFF;
        };
    }

    public static boolean lookUsable(int look) {
        return look < 2 || gi(look, "on", 0) == 1;
    }

    private static int resolveColor(int look) {
        if (gi(look, "color.rb", 0) == 1) return Draw.rainbow();
        return gi(look, "color", defColor(look));
    }

    // --------------------------------------------------------------- pixel grid

    private static boolean[] defaultPixels() {
        boolean[] p = new boolean[PIXELS * PIXELS];
        int c = PIXELS / 2;
        for (int i = 0; i < PIXELS; i++) {
            if (Math.abs(i - c) >= 2) {
                p[c * PIXELS + i] = true;
                p[i * PIXELS + c] = true;
            }
        }
        return p;
    }

    public static boolean[] readPixels(int look) {
        if (!Config.hasInt(key(look, "px0"))) return defaultPixels();
        boolean[] p = new boolean[PIXELS * PIXELS];
        for (int i = 0; i < p.length; i++) {
            int word = Config.getInt(key(look, "px" + (i / 32)), 0);
            p[i] = ((word >>> (i % 32)) & 1) == 1;
        }
        return p;
    }

    public static void writePixels(int look, boolean[] p) {
        int[] words = new int[4];
        for (int i = 0; i < p.length; i++) {
            if (p[i]) words[i / 32] |= 1 << (i % 32);
        }
        for (int w = 0; w < 4; w++) Config.setIntQuiet(key(look, "px" + w), words[w]);
    }

    /** 0 clear, 1 plus, 2 invert, 3 dot, 4 ring. */
    public static void pixelPreset(int look, int which) {
        boolean[] p = new boolean[PIXELS * PIXELS];
        int c = PIXELS / 2;
        switch (which) {
            case 1 -> p = defaultPixels();
            case 2 -> {
                boolean[] cur = readPixels(look);
                for (int i = 0; i < p.length; i++) p[i] = !cur[i];
            }
            case 3 -> {
                for (int y = c - 1; y <= c + 1; y++) for (int x = c - 1; x <= c + 1; x++) p[y * PIXELS + x] = true;
            }
            case 4 -> {
                for (int y = 0; y < PIXELS; y++) {
                    for (int x = 0; x < PIXELS; x++) {
                        double d = Math.sqrt((x - c) * (x - c) + (y - c) * (y - c));
                        if (d >= c - 1.0 && d <= c + 0.3) p[y * PIXELS + x] = true;
                    }
                }
            }
            default -> { }
        }
        writePixels(look, p);
        Config.save();
    }

    // ----------------------------------------------------------------- presets

    /** Writes the setting values of a preset for the Default and Entity looks into a map. */
    public static void presetValues(int i, Map<String, Integer> out) {
        for (int look = 0; look <= 1; look++) {
            switch (i) {
                case 0 -> put(out, look, 0, 4, 4, 3, 1, 1);
                case 1 -> put(out, look, 0, 3, 3, 2, 1, 0);
                case 2 -> put(out, look, STYLE_DOT, 4, 4, 2, 1, 1);
                case 3 -> put(out, look, 0, 7, 7, 3, 3, 1);
                case 4 -> {
                    put(out, look, 4, 4, 4, 2, 1, 1);
                    out.put(key(look, "dot"), 1);
                }
                default -> { }
            }
        }
    }

    private static void put(Map<String, Integer> out, int look, int style, int armH, int armV, int gap, int thick, int outline) {
        out.put(key(look, "style"), style);
        out.put(key(look, "armH"), armH);
        out.put(key(look, "armV"), armV);
        out.put(key(look, "gap"), gap);
        out.put(key(look, "thick"), thick);
        out.put(key(look, "outline"), outline);
    }

    public static void applyPreset(int i) {
        Config.removeIntsWithPrefix(key(0, ""));
        Config.removeIntsWithPrefix(key(1, ""));
        Map<String, Integer> vals = new java.util.LinkedHashMap<>();
        presetValues(i, vals);
        for (Map.Entry<String, Integer> e : vals.entrySet()) Config.setIntQuiet(e.getKey(), e.getValue());
        Config.save();
    }

    // ------------------------------------------------------------------ targeting

    /** True when the game's own crosshair target is an entity you could hit right now. */
    public static boolean inRange(MinecraftClient mc) {
        if (mc.player == null || !(mc.crosshairTarget instanceof EntityHitResult ehr)) return false;
        Entity e = ehr.getEntity();
        if (e == null || !e.isAlive()) return false;
        return switch (clampI(gg("targets", 0), 0, TARGET_FILTERS.length - 1)) {
            case 1 -> e instanceof PlayerEntity;
            case 2 -> e instanceof LivingEntity && !(e instanceof PlayerEntity);
            default -> true;
        };
    }

    public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hit) {
        if (world.isClient() && entity instanceof LivingEntity) {
            hitAt = Util.getMeasuringTimeMs();
        }
        return ActionResult.PASS;
    }

    private static boolean bowDrawn(MinecraftClient mc) {
        if (!mc.player.isUsingItem()) return false;
        var item = mc.player.getActiveItem();
        return item.isOf(Items.BOW) || item.isOf(Items.CROSSBOW) || item.isOf(Items.TRIDENT);
    }

    /** Which look to show right now. Bow beats hit, hit beats low cooldown, then sprint, then entity/default. */
    public static int activeLook(MinecraftClient mc, boolean inRange) {
        int base = inRange ? 1 : 0;
        if (mc == null || mc.player == null) return base;
        if (lookUsable(5) && bowDrawn(mc)) return 5;
        if (lookUsable(2) && Util.getMeasuringTimeMs() - hitAt < 220) return 2;
        if (lookUsable(3) && mc.player.getAttackCooldownProgress(0f) < 0.9f) return 3;
        if (lookUsable(4) && mc.player.isSprinting()) return 4;
        return base;
    }

    public static void drawLive(DrawContext ctx, MinecraftClient mc) {
        boolean in = inRange(mc);
        draw(ctx, mc.getWindow().getScaledWidth() / 2, mc.getWindow().getScaledHeight() / 2,
                1f, activeLook(mc, in), in, LIVE, mc);
    }

    /** Menu preview of one look. No movement or real cooldown. */
    public static void drawPreview(DrawContext ctx, int cx, int cy, float alpha, int look, State st) {
        draw(ctx, cx, cy, alpha, look, look == 1, st, null);
    }

    // ------------------------------------------------------------------- drawing

    private static final class P {
        int style, armH, armV, gap, t;
        boolean top, bottom, left, right;
        int look;
    }

    private static void draw(DrawContext ctx, int cx, int cy, float alpha, int look, boolean inRange, State st, MinecraftClient mc) {
        boolean live = mc != null && mc.player != null;
        long now = Util.getMeasuringTimeMs();
        float dt = st.last == 0 ? 0.016f : Math.min(0.1f, (now - st.last) / 1000f);
        st.last = now;
        float k = 1f - (float) Math.exp(-dt * 16f);

        // Smooth colour change between looks
        int target = resolveColor(look);
        float tr = (target >> 16) & 0xFF, tg = (target >> 8) & 0xFF, tb = target & 0xFF;
        if (!st.init) {
            st.r = tr; st.g = tg; st.b = tb; st.init = true;
        } else {
            st.r += (tr - st.r) * k;
            st.g += (tg - st.g) * k;
            st.b += (tb - st.b) * k;
        }
        int rgb = (Math.round(st.r) << 16) | (Math.round(st.g) << 8) | Math.round(st.b);

        int colorAlpha = (gi(look, "color", defColor(look)) >>> 24) & 0xFF;
        float op = clampF(gi(look, "opacity", 100) / 100f, 0.1f, 1f) * alpha * (colorAlpha / 255f);
        int a8 = Math.round(255f * op);

        P p = new P();
        p.look = look;
        p.style = clampI(gi(look, "style", 0), 0, STYLES.length - 1);
        p.armH = clampI(gi(look, "armH", 5), 0, 30);
        p.armV = clampI(gi(look, "armV", 5), 0, 30);
        p.gap = clampI(gi(look, "gap", 2), 0, 16);
        p.t = clampI(gi(look, "thick", 1), 1, 6);
        p.top = gi(look, "top", 1) == 1 && p.style != 1;
        p.bottom = gi(look, "bottom", 1) == 1;
        p.left = gi(look, "left", 1) == 1;
        p.right = gi(look, "right", 1) == 1;

        int ol = clampI(gi(look, "outline", 1), 0, 4);
        int olColor = gi(look, "outc", 0xFF000000);
        boolean soft = gi(look, "outs", 0) == 1;
        boolean dot = gi(look, "dot", 0) == 1;
        int ds = clampI(gi(look, "dotsize", 2), 1, 8);
        float rot = clampI(gi(look, "rot", 0), 0, 359);
        int offX = clampI(gi(look, "offx", 0), -40, 40);
        int offY = clampI(gi(look, "offy", 0), -40, 40);
        int anim = clampI(gi(look, "anim", 0), 0, ANIMS.length - 1);
        float animSpeed = clampI(gg("animspeed", 4), 1, 10);

        float scale = 1f;
        float seconds = now / 1000f;
        if (anim == 1) scale *= 1f + 0.14f * (float) Math.sin(seconds * animSpeed * 1.4f);
        if (anim == 2) rot = (rot + seconds * animSpeed * 40f) % 360f;

        // Dynamic spread: the crosshair opens up while you move
        if (live && gg("dyn", 0) == 1) {
            Vec3d v = mc.player.getVelocity();
            float speed = (float) Math.sqrt(v.x * v.x + v.z * v.z);
            float want = Math.min(1f, speed / 0.28f);
            st.spread += (want - st.spread) * k;
            p.gap += Math.round(st.spread * clampI(gg("spread", 3), 1, 10));
        } else {
            st.spread = 0f;
        }

        // Pop when you land a hit
        float hitFade = live ? clampF(1f - (now - hitAt) / (float) HIT_FADE_MS, 0f, 1f) : (inRange ? 1f : 0f);
        if (live && gg("hitpop", 0) == 1 && hitFade > 0f) {
            scale *= 1f + 0.35f * hitFade;
        }

        if (a8 > 2) {
            List<int[]> rects = new ArrayList<>(); // x, y, w, h relative to the centre
            shape(rects, p);
            if (dot && p.style != STYLE_DOT) {
                rects.add(new int[]{-ds / 2, -ds / 2, ds, ds});
            }

            int fill = (a8 << 24) | rgb;
            int edgeA = Math.round(((olColor >>> 24) & 0xFF) * op * (soft ? 0.55f : 1f));
            int edge = (edgeA << 24) | (olColor & 0xFFFFFF);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate((float) (cx + offX), (float) (cy + offY));
            if (rot != 0f) ctx.getMatrices().rotate((float) Math.toRadians(rot));
            if (scale != 1f) ctx.getMatrices().scale(scale, scale);
            if (ol > 0) {
                for (int[] r : rects) {
                    ctx.fill(r[0] - ol, r[1] - ol, r[0] + r[2] + ol, r[1] + r[3] + ol, edge);
                }
            }
            for (int[] r : rects) {
                ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], fill);
            }
            if (p.style == STYLE_IMAGE) {
                ImageCrosshair.draw(ctx, Config.getStr(key(look, "img"), ""), clampI(gi(look, "imgsize", 16), 8, 64),
                        gi(look, "imgtint", 0) == 1, fill, a8);
            }
            ctx.getMatrices().popMatrix();
        }

        // Hit marker
        if (gg("hitmark", 1) == 1 && hitFade > 0.02f) {
            drawHitMarker(ctx, cx, cy, hitFade, alpha);
        }

        // Attack cooldown bar under the crosshair (only while recharging)
        if (gg("cdbar", 0) == 1) {
            float prog = live ? mc.player.getAttackCooldownProgress(0f) : 0.55f;
            if (prog < 1f) {
                int ext = Math.max(p.armV, p.armH) + p.gap + p.t + 6 + Math.abs(offY);
                int bw = 24;
                int bx = cx - bw / 2;
                int by = cy + ext;
                int fa = Math.round(255f * alpha);
                ctx.fill(bx - 1, by - 1, bx + bw + 1, by + 3, (Math.round(fa * 0.5f) << 24));
                ctx.fill(bx, by, bx + Math.round(bw * clampF(prog, 0f, 1f)), by + 2, (fa << 24) | rgb);
            }
        }
    }

    private static void drawHitMarker(DrawContext ctx, int cx, int cy, float fade, float alpha) {
        int style = clampI(gg("hitstyle", 0), 0, HIT_STYLES.length - 1);
        int size = clampI(gg("hitsize", 6), 2, 20);
        int col = gg("hitcolor", 0xFFFF4D4D);
        int ha = Math.round(((col >>> 24) & 0xFF) * fade * alpha);
        int hc = (ha << 24) | (col & 0xFFFFFF);
        int he = (Math.round(ha * 0.85f) << 24);
        int d0 = 4;
        List<int[]> m = new ArrayList<>();
        switch (style) {
            case 0 -> { // Ticks
                for (int i = 0; i < size / 2 + 2; i++) {
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sy = -1; sy <= 1; sy += 2) {
                            m.add(new int[]{cx + sx * (d0 + i) - (sx < 0 ? 1 : 0), cy + sy * (d0 + i) - (sy < 0 ? 1 : 0), 2, 2});
                        }
                    }
                }
            }
            case 1 -> { // X
                for (int i = 0; i < size; i++) {
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sy = -1; sy <= 1; sy += 2) {
                            m.add(new int[]{cx + sx * (d0 + i) - (sx < 0 ? 1 : 0), cy + sy * (d0 + i) - (sy < 0 ? 1 : 0), 2, 2});
                        }
                    }
                }
            }
            case 2 -> { // Plus
                m.add(new int[]{cx - 1, cy - d0 - size, 2, size});
                m.add(new int[]{cx - 1, cy + d0, 2, size});
                m.add(new int[]{cx - d0 - size, cy - 1, size, 2});
                m.add(new int[]{cx + d0, cy - 1, size, 2});
            }
            default -> ring(m, cx, cy, size + Math.round((1f - fade) * 8f), 2);
        }
        for (int[] r : m) ctx.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, he);
        for (int[] r : m) ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hc);
    }

    // --------------------------------------------------------------------- shapes

    private static void shape(List<int[]> out, P p) {
        int t = p.t;
        int o = t / 2;
        int h = p.armH;
        int v = p.armV;
        int gap = p.gap;
        switch (p.style) {
            case 0, 1 -> { // Cross, T-Cross
                if (p.top && v > 0) out.add(new int[]{-o, -o - gap - v, t, v});
                if (p.bottom && v > 0) out.add(new int[]{-o, -o + t + gap, t, v});
                if (p.left && h > 0) out.add(new int[]{-o - gap - h, -o, h, t});
                if (p.right && h > 0) out.add(new int[]{-o + t + gap, -o, h, t});
            }
            case 2 -> { // X
                int len = Math.max(h, 1);
                for (int i = 0; i < len; i++) {
                    int d = gap + i;
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sy = -1; sy <= 1; sy += 2) {
                            int x = sx > 0 ? d : -d - t;
                            int y = sy > 0 ? d : -d - t;
                            out.add(new int[]{x, y, t, t});
                        }
                    }
                }
            }
            case STYLE_DOT -> {
                int d = Math.max(2, h / 2 + t);
                out.add(new int[]{-d / 2, -d / 2, d, d});
            }
            case 4 -> ring(out, 0, 0, Math.max(2, h + gap + 2), t); // Circle
            case 5 -> { // Square (separate width and height)
                int hx = h + gap;
                int hy = v + gap;
                out.add(new int[]{-hx, -hy, 2 * hx + 1, t});
                out.add(new int[]{-hx, hy + 1 - t, 2 * hx + 1, t});
                int inner = 2 * hy + 1 - 2 * t;
                if (inner > 0) {
                    out.add(new int[]{-hx, -hy + t, t, inner});
                    out.add(new int[]{hx + 1 - t, -hy + t, t, inner});
                }
            }
            case 6 -> { // Diamond
                int hx = Math.max(1, h + gap + 1);
                int hy = Math.max(1, v + gap + 1);
                for (int i = 0; i <= hx; i++) {
                    int y = Math.round(hy * (1f - i / (float) hx));
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sy = -1; sy <= 1; sy += 2) {
                            out.add(new int[]{sx * i - (sx < 0 ? t - 1 : 0), sy * y - (sy < 0 ? t - 1 : 0), t, t});
                        }
                    }
                }
            }
            case 7 -> { // Brackets
                int hx = h + gap + 1;
                int hy = v + gap + 1;
                int armX = Math.max(2, hx / 2 + 1);
                int armY = Math.max(2, hy / 2 + 1);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        int bx = sx > 0 ? hx - armX + 1 : -hx;
                        int by = sy > 0 ? hy - t + 1 : -hy;
                        out.add(new int[]{bx, by, armX, t});
                        int vx = sx > 0 ? hx - t + 1 : -hx;
                        int vy = sy > 0 ? hy - armY + 1 : -hy;
                        out.add(new int[]{vx, vy, t, armY});
                    }
                }
            }
            case 8 -> { // Chevron (points up)
                int len = Math.max(2, h);
                int rise = Math.max(1, v);
                for (int i = 0; i <= len; i++) {
                    int y = Math.round(i * rise / (float) len) - rise / 2 + gap;
                    out.add(new int[]{i, y, t, t});
                    out.add(new int[]{-i - t + 1, y, t, t});
                }
            }
            case 9 -> { // Triangle (points up)
                int hx = Math.max(2, h + gap);
                int hy = Math.max(2, v + gap);
                for (int j = 0; j <= 2 * hy; j++) {
                    int xh = Math.round(hx * j / (float) (2 * hy));
                    out.add(new int[]{xh, -hy + j, t, 1});
                    out.add(new int[]{-xh - t + 1, -hy + j, t, 1});
                }
                out.add(new int[]{-hx, hy, 2 * hx + t, t});
            }
            case 10 -> { // Ring + Cross
                int r = Math.max(3, h);
                ring(out, 0, 0, r, t);
                if (p.top && v > 0) out.add(new int[]{-o, -r - gap - v, t, v});
                if (p.bottom && v > 0) out.add(new int[]{-o, r + gap + 1, t, v});
                if (p.left && v > 0) out.add(new int[]{-r - gap - v, -o, v, t});
                if (p.right && v > 0) out.add(new int[]{r + gap + 1, -o, v, t});
            }
            case 11 -> { // Dashed cross
                if (p.top) for (int s = 0; s < v; s += 4) out.add(new int[]{-o, -o - gap - s - Math.min(2, v - s), t, Math.min(2, v - s)});
                if (p.bottom) for (int s = 0; s < v; s += 4) out.add(new int[]{-o, -o + t + gap + s, t, Math.min(2, v - s)});
                if (p.left) for (int s = 0; s < h; s += 4) out.add(new int[]{-o - gap - s - Math.min(2, h - s), -o, Math.min(2, h - s), t});
                if (p.right) for (int s = 0; s < h; s += 4) out.add(new int[]{-o + t + gap + s, -o, Math.min(2, h - s), t});
            }
            case STYLE_IMAGE -> { }
            default -> { // Pixel
                boolean[] px = readPixels(p.look);
                int c = PIXELS / 2;
                for (int y = 0; y < PIXELS; y++) {
                    int start = -1;
                    for (int x = 0; x <= PIXELS; x++) {
                        boolean on = x < PIXELS && px[y * PIXELS + x];
                        if (on && start < 0) {
                            start = x;
                        } else if (!on && start >= 0) {
                            out.add(new int[]{(start - c) * t - t / 2, (y - c) * t - t / 2, (x - start) * t, t});
                            start = -1;
                        }
                    }
                }
            }
        }
    }

    /** A pixel ring made of horizontal runs, so large circles stay cheap to draw. */
    private static void ring(List<int[]> out, int cx, int cy, int r, int t) {
        double outer = r + 0.5;
        double inner = Math.max(0, r - t + 0.5);
        for (int dy = -r; dy <= r; dy++) {
            int start = Integer.MIN_VALUE;
            for (int dx = -r; dx <= r + 1; dx++) {
                boolean in = false;
                if (dx <= r) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    in = dist <= outer && dist >= inner;
                }
                if (in && start == Integer.MIN_VALUE) {
                    start = dx;
                } else if (!in && start != Integer.MIN_VALUE) {
                    out.add(new int[]{cx + start, cy + dy, dx - start, 1});
                    start = Integer.MIN_VALUE;
                }
            }
        }
    }
}
