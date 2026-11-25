package dev.pvpmenu.gui;

import dev.pvpmenu.Config;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.Locale;

/** Click speed test: click the big pad as fast as you can until the timer ends. ESC goes back. */
public class ClickScreen extends Screen {
    private final Screen parent;
    private long startAt = -1;
    private int clicks;
    private boolean done;
    private boolean newBest;
    private final int seconds;

    public ClickScreen(Screen parent) {
        super(Text.literal("Click speed test"));
        this.parent = parent;
        this.seconds = Warmup.clickSeconds();
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        if (startAt >= 0 && !done && now - startAt >= seconds * 1000L) {
            done = true;
            float cps = clicks / (float) seconds;
            int stored = Config.getInt("wu.click.best." + seconds, 0);
            int x10 = Math.round(cps * 10f);
            if (x10 > stored) {
                Config.setInt("wu.click.best." + seconds, x10);
                newBest = true;
            }
        }
        ctx.fill(0, 0, width, height, 0xFF111827);
        int pw = Math.min(width - 40, 260);
        int ph = 110;
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        boolean running = startAt >= 0 && !done;
        Draw.rrect(ctx, px, py, px + pw, py + ph, 8, running ? 0xFF1D4ED8 : done ? 0xFF1F2937 : 0xFF374151);

        String big;
        String small;
        if (done) {
            big = String.format(Locale.ROOT, "%.1f CPS", clicks / (float) seconds);
            small = clicks + " clicks in " + seconds + " s" + (newBest ? "  -  NEW BEST!" : "") + ".  Click to go again";
        } else if (running) {
            float left = Math.max(0f, seconds - (now - startAt) / 1000f);
            big = String.valueOf(clicks);
            small = String.format(Locale.ROOT, "%.1f s left   %.1f CPS", left, clicks / Math.max(0.2f, (now - startAt) / 1000f));
        } else {
            big = "Click to start";
            int best = Config.getInt("wu.click.best." + seconds, 0);
            small = seconds + " second test." + (best > 0 ? "  Best " + String.format(Locale.ROOT, "%.1f", best / 10f) + " CPS" : "");
        }
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(width / 2f, py + 30);
        ctx.getMatrices().scale(2.5f, 2.5f);
        ctx.drawText(textRenderer, big, -textRenderer.getWidth(big) / 2, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().popMatrix();
        ctx.drawText(textRenderer, small, (width - textRenderer.getWidth(small)) / 2, py + 80, 0xFFE5E7EB, true);
        String esc = "ESC to go back";
        ctx.drawText(textRenderer, esc, (width - textRenderer.getWidth(esc)) / 2, height - 16, 0x99FFFFFF, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int b = click.button();
        if (b != 0 && b != 1) return super.mouseClicked(click, doubled);
        if (done) {
            done = false;
            newBest = false;
            startAt = -1;
            clicks = 0;
            return true;
        }
        if (startAt < 0) startAt = Util.getMeasuringTimeMs();
        clicks++;
        return true;
    }
}
