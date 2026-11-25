package dev.pvpmenu.gui;

import dev.pvpmenu.Config;
import dev.pvpmenu.HudRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Drag the HUD cards anywhere on the screen, with optional grid snapping. ESC goes back to the menu. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private final Map<String, int[]> override = new HashMap<>();
    private String dragId;
    private int dragW;
    private int offX;
    private int offY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Layout"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private float scale() {
        return Config.scale();
    }

    private static int snap(int v, int grid) {
        return grid <= 0 ? v : Math.round(v / (float) grid) * grid;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x88070810);

        float scale = scale();
        float hx = mouseX / scale;
        float hy = mouseY / scale;
        int sw = Math.round(width / scale);
        int sh = Math.round(height / scale);
        int grid = Config.getInt("ui.grid", 0);
        int accent = Draw.accent();

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);
        if (grid > 0) {
            int line = Draw.withAlpha(0xFFFFFFFF, 0x12);
            for (int gx = 0; gx < sw; gx += grid) ctx.fill(gx, 0, gx + 1, sh, line);
            for (int gy = 0; gy < sh; gy += grid) ctx.fill(0, gy, sw, gy + 1, line);
        }
        ctx.getMatrices().popMatrix();

        // Continue or finish a drag by watching the mouse button directly
        if (dragId != null) {
            boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (down) {
                int nx = Math.round(hx - offX);
                int ny = Math.round(hy - offY);
                int[] cur = override.get(dragId);
                if (grid > 0) {
                    nx = snap(nx, grid);
                    ny = snap(ny, grid);
                } else {
                    // snap to the screen edges and the horizontal centre
                    if (Math.abs(nx - 6) < 5) nx = 6;
                    if (Math.abs(nx - (sw - dragW - 6)) < 5) nx = sw - dragW - 6;
                    if (Math.abs(nx - (sw - dragW) / 2) < 4) nx = (sw - dragW) / 2;
                    if (Math.abs(ny - 6) < 5) ny = 6;
                }
                cur[0] = Math.max(0, nx);
                cur[1] = Math.max(0, ny);
            } else {
                int[] fin = override.remove(dragId);
                if (fin != null) {
                    Config.setIntQuiet("pos." + dragId + ".x", fin[0]);
                    Config.setIntQuiet("pos." + dragId + ".y", fin[1]);
                    Config.save();
                }
                dragId = null;
            }
        }

        List<HudRenderer.Placed> cards = HudRenderer.layout(client, override);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(scale, scale);
        for (HudRenderer.Placed p : cards) {
            boolean hover = hx >= p.x && hx < p.x + p.w && hy >= p.y && hy < p.y + p.h;
            HudRenderer.drawCard(ctx, client, p, hover || p.m.id.equals(dragId));
        }
        ctx.getMatrices().popMatrix();

        String title = "HUD layout";
        ctx.drawText(textRenderer, title, (width - textRenderer.getWidth(title)) / 2, 14, 0xFFFFFFFF, true);
        String hint = cards.isEmpty()
                ? "No cards are switched on. Enable them in the HUD tab first. ESC to go back."
                : "Drag the cards where you want them" + (grid > 0 ? " (snapping to a " + grid + " px grid)" : "") + ". ESC to go back.";
        ctx.drawText(textRenderer, hint, (width - textRenderer.getWidth(hint)) / 2, 27, 0xFF8A8FA3, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);
        float scale = scale();
        float hx = (float) click.x() / scale;
        float hy = (float) click.y() / scale;

        List<HudRenderer.Placed> cards = HudRenderer.layout(client, override);
        for (int i = cards.size() - 1; i >= 0; i--) {
            HudRenderer.Placed p = cards.get(i);
            if (hx >= p.x && hx < p.x + p.w && hy >= p.y && hy < p.y + p.h) {
                dragId = p.m.id;
                dragW = p.w;
                offX = Math.round(hx - p.x);
                offY = Math.round(hy - p.y);
                override.put(dragId, new int[]{p.x, p.y});
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
