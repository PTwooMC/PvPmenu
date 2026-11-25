package dev.pvpmenu.gui;

import dev.pvpmenu.Config;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Reaction test: wait for the screen to change colour, then click as fast as you can. This is a menu screen,
 * so it uses your mouse cursor. ESC goes back to the menu.
 */
public class ReactionScreen extends Screen {
    private enum State { IDLE, WAIT, GO, EARLY, ROUND_DONE, FINISHED }

    private final Screen parent;
    private final Random rng = new Random();
    private State state = State.IDLE;
    private long changeAt;
    private long goAt;
    private int lastMs;
    private final List<Integer> times = new ArrayList<>();
    private boolean newBest;

    public ReactionScreen(Screen parent) {
        super(Text.literal("Reaction test"));
        this.parent = parent;
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private void startWait() {
        state = State.WAIT;
        long now = Util.getMeasuringTimeMs();
        int span = Warmup.maxWaitMs() - Warmup.minWaitMs();
        changeAt = now + Warmup.minWaitMs() + rng.nextInt(Math.max(1, span));
    }

    private void press() {
        long now = Util.getMeasuringTimeMs();
        switch (state) {
            case IDLE, FINISHED -> {
                times.clear();
                newBest = false;
                startWait();
            }
            case WAIT -> state = State.EARLY;
            case GO -> {
                lastMs = (int) (now - goAt);
                times.add(lastMs);
                state = times.size() >= Warmup.rounds() ? State.FINISHED : State.ROUND_DONE;
                if (state == State.FINISHED) finish();
            }
            case EARLY, ROUND_DONE -> startWait();
        }
    }

    private void finish() {
        int best = Integer.MAX_VALUE;
        for (int t : times) best = Math.min(best, t);
        int saved = Warmup.reactBest();
        if (saved == 0 || best < saved) {
            newBest = true;
            Config.setInt("wu.react.best", best);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        if (state == State.WAIT && now >= changeAt) {
            state = State.GO;
            goAt = now;
        }

        int bg;
        String big;
        String small;
        switch (state) {
            case WAIT -> { bg = Warmup.waitColor(); big = "Wait for green..."; small = "Do not click yet"; }
            case GO -> { bg = Warmup.goColor(); big = "CLICK!"; small = ""; }
            case EARLY -> { bg = 0xFF1F2937; big = "Too early"; small = "Click to try this round again"; }
            case ROUND_DONE -> {
                bg = 0xFF1F2937;
                big = lastMs + " ms";
                small = "Round " + times.size() + " of " + Warmup.rounds() + ". Click for the next one";
            }
            case FINISHED -> {
                bg = 0xFF1F2937;
                big = String.format(Locale.ROOT, "Average %d ms", average());
                small = "Best this run " + best() + " ms" + (newBest ? "  -  NEW PERSONAL BEST!" : "") + ". Click to go again";
            }
            default -> {
                bg = 0xFF111827;
                big = "Reaction test";
                small = "Click or press Space to start. " + Warmup.rounds() + " rounds."
                        + (Warmup.reactBest() > 0 ? "  Personal best: " + Warmup.reactBest() + " ms" : "");
            }
        }
        ctx.fill(0, 0, width, height, bg);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(width / 2f, height / 2f - 16);
        ctx.getMatrices().scale(3f, 3f);
        ctx.drawText(textRenderer, big, -textRenderer.getWidth(big) / 2, 0, 0xFFFFFFFF, true);
        ctx.getMatrices().popMatrix();
        ctx.drawText(textRenderer, small, (width - textRenderer.getWidth(small)) / 2, height / 2 + 20, 0xFFE5E7EB, true);

        if (!times.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int t : times) sb.append(t).append(" ms   ");
            String s = sb.toString().trim();
            ctx.drawText(textRenderer, s, (width - textRenderer.getWidth(s)) / 2, height / 2 + 38, 0xFFB6BCCB, true);
        }
        String esc = "ESC to go back";
        ctx.drawText(textRenderer, esc, (width - textRenderer.getWidth(esc)) / 2, height - 16, 0x99FFFFFF, true);
    }

    private int average() {
        if (times.isEmpty()) return 0;
        long sum = 0;
        for (int t : times) sum += t;
        return (int) (sum / times.size());
    }

    private int best() {
        int b = Integer.MAX_VALUE;
        for (int t : times) b = Math.min(b, t);
        return b == Integer.MAX_VALUE ? 0 : b;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            press();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_SPACE) {
            press();
            return true;
        }
        return super.keyPressed(input);
    }
}
