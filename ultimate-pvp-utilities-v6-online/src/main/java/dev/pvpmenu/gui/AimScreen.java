package dev.pvpmenu.gui;

import dev.pvpmenu.Config;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A simple aim trainer: click the targets before they disappear. Levels change the size, speed, how long
 * targets last and how many are on screen; "Custom" uses your own numbers. It is a menu screen, so it uses
 * your mouse cursor. ESC goes back to the menu.
 */
public class AimScreen extends Screen {
    private enum State { READY, RUNNING, DONE }

    private static final class Target {
        float x, y, vx, vy;
        long born;
    }

    private final Screen parent;
    private final Random rng = new Random();
    private final List<Target> targets = new ArrayList<>();
    private State state = State.READY;
    private long startedAt;
    private long lastFrame;
    private int hits;
    private int misses;
    private int expired;
    private long hitTimeSum;
    private boolean newBest;
    private long hitFlashAt;
    private float flashX, flashY;

    public AimScreen(Screen parent) {
        super(Text.literal("Aim trainer"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private void start() {
        state = State.RUNNING;
        hits = 0;
        misses = 0;
        expired = 0;
        hitTimeSum = 0;
        newBest = false;
        targets.clear();
        startedAt = Util.getMeasuringTimeMs();
        lastFrame = startedAt;
    }

    private float speedPx() {
        return switch (Warmup.moving()) {
            case 1 -> 40f;
            case 2 -> 90f;
            case 3 -> 160f;
            default -> 0f;
        };
    }

    private void spawn(long now) {
        int r = Warmup.size();
        Target t = new Target();
        int mx = r + 20;
        int top = 40;
        for (int tries = 0; tries < 12; tries++) {
            t.x = mx + rng.nextFloat() * Math.max(1, width - mx * 2);
            t.y = top + r + rng.nextFloat() * Math.max(1, height - top - r - 30);
            boolean clash = false;
            for (Target o : targets) {
                float dx = o.x - t.x, dy = o.y - t.y;
                if (dx * dx + dy * dy < (r * 2.4f) * (r * 2.4f)) { clash = true; break; }
            }
            if (!clash) break;
        }
        float sp = speedPx();
        if (sp > 0f) {
            double a = rng.nextDouble() * Math.PI * 2;
            t.vx = (float) Math.cos(a) * sp;
            t.vy = (float) Math.sin(a) * sp;
        }
        t.born = now;
        targets.add(t);
    }

    private void finish() {
        state = State.DONE;
        int lvl = Warmup.level();
        if (lvl > 0 && hits > Warmup.aimBest(lvl)) {
            Config.setInt("wu.aim.best." + lvl, hits);
            newBest = true;
        }
    }

    private boolean hitTest(Target t, double mx, double my) {
        float r = Warmup.size();
        float dx = (float) mx - t.x;
        float dy = (float) my - t.y;
        if (Warmup.shape() == 1) return Math.abs(dx) <= r && Math.abs(dy) <= r;
        return dx * dx + dy * dy <= r * r;
    }

    private void drawTarget(DrawContext ctx, Target t, int alpha) {
        int r = Warmup.size();
        int c = (alpha << 24) | (Warmup.targetColor() & 0xFFFFFF);
        int cx = Math.round(t.x), cy = Math.round(t.y);
        switch (Warmup.shape()) {
            case 1 -> ctx.fill(cx - r, cy - r, cx + r, cy + r, c);
            case 2 -> {
                Draw.rrect(ctx, cx - r, cy - r, cx + r, cy + r, r, c);
                Draw.rrect(ctx, cx - r / 2, cy - r / 2, cx + r / 2, cy + r / 2, r / 2, 0xFF0B0C14);
            }
            default -> Draw.rrect(ctx, cx - r, cy - r, cx + r, cy + r, r, c);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        ctx.fill(0, 0, width, height, (Math.round(255f * Math.max(20, Warmup.dim()) / 100f) << 24) | 0x0B0C14);

        if (state == State.RUNNING) {
            float dt = Math.min(0.05f, (now - lastFrame) / 1000f);
            lastFrame = now;
            long elapsed = now - startedAt;
            if (elapsed >= Warmup.durationSeconds() * 1000L) {
                finish();
            } else {
                int life = Warmup.lifetimeMs();
                for (int i = targets.size() - 1; i >= 0; i--) {
                    Target t = targets.get(i);
                    t.x += t.vx * dt;
                    t.y += t.vy * dt;
                    int r = Warmup.size();
                    if (t.x < r || t.x > width - r) { t.vx = -t.vx; t.x = Math.max(r, Math.min(width - r, t.x)); }
                    if (t.y < 40 + r || t.y > height - r) { t.vy = -t.vy; t.y = Math.max(40 + r, Math.min(height - r, t.y)); }
                    if (life > 0 && now - t.born > life) {
                        targets.remove(i);
                        expired++;
                    }
                }
                while (targets.size() < Warmup.targets()) spawn(now);
                for (Target t : targets) {
                    int a = 255;
                    if (life > 0) {
                        float left = 1f - (now - t.born) / (float) life;
                        if (left < 0.25f) a = 90 + Math.round(165 * (left / 0.25f));
                    }
                    drawTarget(ctx, t, a);
                }
                if (now - hitFlashAt < 150) {
                    float k = 1f - (now - hitFlashAt) / 150f;
                    int ring = Math.round(Warmup.size() * (1f + (1f - k) * 0.6f));
                    int c = Draw.withAlpha(0xFFFFFFFF, Math.round(160 * k));
                    Draw.rrect(ctx, Math.round(flashX) - ring, Math.round(flashY) - ring,
                            Math.round(flashX) + ring, Math.round(flashY) + ring, ring, c);
                }
            }
        }

        // top bar
        int total = Warmup.durationSeconds();
        float left = state == State.RUNNING ? Math.max(0f, total - (now - startedAt) / 1000f) : total;
        String top = String.format(Locale.ROOT, "%s   Hits %d   Misses %d   Time %.1fs",
                Warmup.LEVELS[Warmup.level()], hits, misses, left);
        ctx.drawText(textRenderer, top, 10, 12, 0xFFE5E7EB, true);
        if (state == State.RUNNING) {
            int bw = width - 20;
            ctx.fill(10, 26, 10 + bw, 29, 0x33FFFFFF);
            ctx.fill(10, 26, 10 + Math.round(bw * (left / total)), 29, Warmup.targetColor());
        }

        if (state != State.RUNNING) centerPanel(ctx);
    }

    private void centerPanel(DrawContext ctx) {
        int pw = 260;
        int ph = state == State.DONE ? 118 : 96;
        int x = (width - pw) / 2;
        int y = (height - ph) / 2;
        Draw.rrect(ctx, x, y, x + pw, y + ph, 8, 0xF0161821);
        ctx.fill(x + 8, y + 1, x + pw - 8, y + 2, Draw.withAlpha(Themes.accA(), 0xA0));
        String head = state == State.DONE ? "Finished!" : "Aim trainer";
        ctx.drawText(textRenderer, head, x + (pw - textRenderer.getWidth(head)) / 2, y + 12, 0xFFFFFFFF, true);
        List<String> lines = new ArrayList<>();
        if (state == State.DONE) {
            int shots = hits + misses;
            int acc = shots == 0 ? 0 : Math.round(100f * hits / shots);
            int avg = hits == 0 ? 0 : (int) (hitTimeSum / hits);
            lines.add("Hits " + hits + "    Misses " + misses + "    Missed targets " + expired);
            lines.add("Accuracy " + acc + "%    Average time to hit " + avg + " ms");
            int lvl = Warmup.level();
            if (lvl > 0) lines.add((newBest ? "NEW BEST: " : "Best: ") + Math.max(hits, Warmup.aimBest(lvl)) + " hits on " + Warmup.LEVELS[lvl]);
            lines.add("Click to play again.");
        } else {
            lines.add(Warmup.LEVELS[Warmup.level()] + ", " + Warmup.durationSeconds() + " seconds");
            lines.add("Click the targets before they vanish.");
            lines.add("Change the level and look in the Warm-up tab.");
            lines.add("Click to start.");
        }
        int yy = y + 32;
        for (String s : lines) {
            ctx.drawText(textRenderer, s, x + (pw - textRenderer.getWidth(s)) / 2, yy, 0xFFB6BCCB, true);
            yy += 13;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        if (state != State.RUNNING) {
            start();
            return true;
        }
        long now = Util.getMeasuringTimeMs();
        for (int i = targets.size() - 1; i >= 0; i--) {
            Target t = targets.get(i);
            if (hitTest(t, click.x(), click.y())) {
                hits++;
                hitTimeSum += now - t.born;
                flashX = t.x;
                flashY = t.y;
                hitFlashAt = now;
                targets.remove(i);
                return true;
            }
        }
        misses++;
        return true;
    }
}
