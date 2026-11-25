package dev.pvpmenu;

import dev.pvpmenu.feature.AppleSkin;
import dev.pvpmenu.feature.HotbarKeys;
import dev.pvpmenu.feature.ShieldStatus;
import dev.pvpmenu.feature.TntTimer;
import dev.pvpmenu.gui.Draw;
import dev.pvpmenu.gui.HudEditorScreen;
import dev.pvpmenu.gui.Themes;
import dev.pvpmenu.module.Module;
import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws every enabled HUD module as a card. Cards you never moved are stacked in the corner chosen in
 * the HUD tab; cards you dragged in the layout editor sit exactly where you put them. Every card can have
 * its own background colour, accent colour, opacity and size.
 */
public final class HudRenderer {
    public static final int CARD_W = 118;
    private static final int GAP = 4;
    private static final int MARGIN = 6;
    private static final int PAD_X = 8;
    private static final int LABEL_H = 11;
    private static final int PAD_Y = 5;

    /** Toggled by the "Hide HUD" key. Hides all cards at once. */
    public static boolean hidden = false;

    public static final class Placed {
        public final Module m;
        public final float s;
        public final int rawH;
        public final int w;
        public final int h;
        public int x;
        public int y;

        Placed(Module m, int rawH, float s) {
            this.m = m;
            this.rawH = rawH;
            this.s = s;
            this.w = Math.round(CARD_W * s);
            this.h = Math.round(rawH * s);
        }
    }

    private HudRenderer() {}

    // ------------------------------------------------------------ per-card style

    public static String ck(String id, String name) {
        return "card." + id + "." + name;
    }

    public static int cardBg(String id) {
        return Config.getInt(ck(id, "bg"), 0xFF101218) | 0xFF000000;
    }

    public static int cardAccent(String id) {
        String k = ck(id, "accent");
        return Config.hasInt(k) ? (Config.getInt(k, 0) | 0xFF000000) : Themes.accA();
    }

    public static int cardOpacity(String id) {
        return Math.max(10, Math.min(100, Config.getInt(ck(id, "opacity"), Config.getInt("ui.cardalpha", 67))));
    }

    public static float cardSize(String id) {
        return Math.max(60, Math.min(200, Config.getInt(ck(id, "size"), 100))) / 100f;
    }

    // ------------------------------------------------------------------ drawing

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Stats.frame(mc);
        } catch (Throwable t) {
            Guard.fail("cps", t);
        }
        if (mc.player == null || mc.options.hudHidden) return;

        // The layout editor draws the cards itself
        if (!hidden && !(mc.currentScreen instanceof HudEditorScreen)) {
            float scale = Config.scale();
            List<Placed> cards = layout(mc, null);
            if (!cards.isEmpty()) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().scale(scale, scale);
                for (Placed p : cards) {
                    Guard.run(p.m.id, () -> drawCard(ctx, mc, p, false));
                }
                ctx.getMatrices().popMatrix();
            }
        }
        Guard.run("appleskin", () -> AppleSkin.render(ctx, mc));
        Guard.run("hotbarkeys", () -> HotbarKeys.renderHud(ctx, mc));
        final float delta = tickCounter.getTickProgress(false);
        Guard.run("tnt", () -> TntTimer.render(ctx, mc, delta));
        Guard.run("shieldstatus", () -> ShieldStatus.render(ctx, mc));
        Guard.run("smalltotem", () -> TotemPop.render(ctx, mc));
    }

    /** Positions of all enabled cards, in HUD space (screen size divided by the HUD scale). */
    public static List<Placed> layout(MinecraftClient mc, Map<String, int[]> override) {
        List<Placed> out = new ArrayList<>();
        List<Placed> auto = new ArrayList<>();
        boolean labels = Config.getInt("ui.labels", 1) == 1;
        float scale = Config.scale();
        int sw = Math.round(mc.getWindow().getScaledWidth() / scale);
        int sh = Math.round(mc.getWindow().getScaledHeight() / scale);

        for (Module m : Modules.ALL) {
            if (!m.isHud() || !m.isEnabled()) continue;
            int raw = PAD_Y * 2 + (labels ? LABEL_H : 0);
            try {
                if (override == null && !m.visible(mc)) continue;
                raw += m.contentHeight(mc);
            } catch (Throwable t) {
                Guard.fail(m.id, t);
                continue;
            }
            Placed p = new Placed(m, raw, cardSize(m.id));
            int[] ov = override != null ? override.get(m.id) : null;
            int px = ov != null ? ov[0] : Config.getInt("pos." + m.id + ".x", -1);
            int py = ov != null ? ov[1] : Config.getInt("pos." + m.id + ".y", -1);
            if (px >= 0 && py >= 0) {
                p.x = Math.max(0, Math.min(Math.max(0, sw - p.w), px));
                p.y = Math.max(0, Math.min(Math.max(0, sh - p.h), py));
                out.add(p);
            } else {
                auto.add(p);
            }
        }

        if (!auto.isEmpty()) {
            int corner = Config.corner();
            boolean right = corner == 1 || corner == 3;
            boolean bottom = corner >= 2;
            int total = GAP * (auto.size() - 1);
            for (Placed p : auto) total += p.h;
            int y = bottom ? sh - MARGIN - total : MARGIN;
            for (Placed p : auto) {
                p.x = right ? sw - p.w - MARGIN : MARGIN;
                p.y = y;
                y += p.h + GAP;
                out.add(p);
            }
        }
        return out;
    }

    public static void drawCard(DrawContext ctx, MinecraftClient mc, Placed p, boolean highlight) {
        boolean labels = Config.getInt("ui.labels", 1) == 1;
        int radius = Config.getInt("ui.round", 1) == 1 ? 4 : 0;
        int bgA = Math.round(255f * cardOpacity(p.m.id) / 100f);
        int accent = cardAccent(p.m.id);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) p.x, (float) p.y);
        if (p.s != 1f) ctx.getMatrices().scale(p.s, p.s);

        int w = CARD_W;
        int h = p.rawH;
        Draw.rrect(ctx, 0, 0, w, h, radius, (bgA << 24) | (cardBg(p.m.id) & 0xFFFFFF));
        if (radius > 0) {
            Draw.rrect(ctx, 0, 4, 2, h - 4, 1, accent);
        } else {
            ctx.fill(0, 0, 2, h, accent);
        }

        int cy = PAD_Y;
        if (labels) {
            ctx.drawText(mc.textRenderer, p.m.title().toUpperCase(), PAD_X, cy, 0xFF8A8FA3, false);
            cy += LABEL_H;
        }
        p.m.renderContent(ctx, mc, PAD_X, cy, w - PAD_X * 2);
        ctx.getMatrices().popMatrix();

        if (highlight) {
            ctx.fill(p.x - 1, p.y - 1, p.x + p.w + 1, p.y, accent);
            ctx.fill(p.x - 1, p.y + p.h, p.x + p.w + 1, p.y + p.h + 1, accent);
            ctx.fill(p.x - 1, p.y, p.x, p.y + p.h, accent);
            ctx.fill(p.x + p.w, p.y, p.x + p.w + 1, p.y + p.h, accent);
        }
    }
}
