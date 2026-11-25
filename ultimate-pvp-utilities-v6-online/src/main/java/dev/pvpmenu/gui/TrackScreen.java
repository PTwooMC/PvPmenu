package dev.pvpmenu.gui;

import dev.pvpmenu.Config;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.Locale;

/** Tracking test: keep your cursor on a moving target. Score is the share of time you stayed on it. */
public class TrackScreen extends Screen {
    private final Screen parent;
    private long startAt = -1;
    private long last;
    private float onTime;
    private boolean done;
    private boolean newBest;
    private double tx, ty, vx, vy;
    private long turnAt;
    private final java.util.Random rng = new java.util.Random();
    private final int seconds = Warmup.trackSeconds();
    private final int radius = Warmup.trackSize();
    private final float speed = Warmup.trackSpeed();

    public TrackScreen(Screen parent) {
        super(Text.literal("Tracking test"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private void newDirection(long now) {
        double a = rng.nextDouble() * Math.PI * 2;
        vx = Math.cos(a) * speed;
        vy = Math.sin(a) * speed;
        turnAt = now + 500 + rng.nextInt(900);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        ctx.fill(0, 0, width, height, 0xFF000000 | (Math.round(255f * (100 - Warmup.dim()) / 100f * 0.12f) * 0x010101));
        if (tx == 0 && ty == 0) {
            tx = width / 2.0;
            ty = height / 2.0;
        }
        boolean running = startAt >= 0 && !done;
        float dt = last == 0 ? 0f : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        if (running) {
            if (now >= turnAt) newDirection(now);
            tx += vx * dt;
            ty += vy * dt;
            if (tx < radius || tx > width - radius) { vx = -vx; tx = Math.max(radius, Math.min(width - radius, tx)); }
            if (ty < radius + 20 || ty > height - radius) { vy = -vy; ty = Math.max(radius + 20, Math.min(height - radius, ty)); }
            double dx = mouseX - tx, dy = mouseY - ty;
            if (dx * dx + dy * dy <= (double) radius * radius) onTime += dt;
            if (now - startAt >= seconds * 1000L) {
                done = true;
                int pct = Math.round(100f * onTime / seconds);
                if (pct > Config.getInt("wu.track.best." + seconds, 0)) {
                    Config.setInt("wu.track.best." + seconds, pct);
                    newBest = true;
                }
            }
        }
        boolean on = (mouseX - tx) * (mouseX - tx) + (mouseY - ty) * (mouseY - ty) <= (double) radius * radius;
        int col = Warmup.targetColor();
        Draw.rrect(ctx, (int) tx - radius, (int) ty - radius, (int) tx + radius, (int) ty + radius, radius,
                running && on ? 0xFF4ADE80 : col);
        String msg;
        if (done) {
            msg = Math.round(100f * onTime / seconds) + "% on target" + (newBest ? "  -  NEW BEST!" : "") + "   Click to go again";
        } else if (running) {
            msg = String.format(Locale.ROOT, "%.0f%% on target   %.1f s left", 100f * onTime / Math.max(0.2f, (now - startAt) / 1000f),
                    Math.max(0f, seconds - (now - startAt) / 1000f));
        } else {
            int best = Config.getInt("wu.track.best." + seconds, 0);
            msg = "Click to start. Keep your cursor on the target for " + seconds + " s." + (best > 0 ? "  Best " + best + "%" : "");
        }
        ctx.drawText(textRenderer, msg, (width - textRenderer.getWidth(msg)) / 2, 8, 0xFFFFFFFF, true);
        String esc = "ESC to go back";
        ctx.drawText(textRenderer, esc, (width - textRenderer.getWidth(esc)) / 2, height - 14, 0x99FFFFFF, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (done || startAt < 0) {
            done = false;
            newBest = false;
            onTime = 0;
            startAt = Util.getMeasuringTimeMs();
            last = startAt;
            newDirection(startAt);
        }
        return true;
    }
}
