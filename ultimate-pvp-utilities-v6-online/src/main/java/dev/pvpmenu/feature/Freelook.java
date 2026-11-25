package dev.pvpmenu.feature;

import dev.pvpmenu.PvpMenuClient;
import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/**
 * Look around without turning your character, like the Perspective mod. While active, mouse movement turns
 * the camera only. The camera mixin sets {@link #cameraHooked}; if it could not hook into the game this
 * version, mouse input is never taken away from you.
 */
public final class Freelook {
    public static boolean cameraHooked;
    private static boolean active;
    private static boolean wasHeld;
    private static float yaw;
    private static float pitch;
    private static Perspective before;

    private Freelook() {}

    public static boolean active() { return active; }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static boolean capturing() { return active && cameraHooked; }

    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
    }

    public static void tick(MinecraftClient mc) {
        boolean enabled = Modules.FREELOOK.isEnabled() && mc.player != null && mc.currentScreen == null;
        if (!enabled) {
            if (active) stop(mc);
            wasHeld = false;
            return;
        }
        boolean held = PvpMenuClient.FREELOOK_KEY.isPressed();
        boolean toggleMode = Modules.FREELOOK.optOf("mode", 0) == 1;
        if (toggleMode) {
            if (held && !wasHeld) {
                if (active) stop(mc); else start(mc);
            }
        } else if (held && !active) {
            start(mc);
        } else if (!held && active) {
            stop(mc);
        }
        wasHeld = held;
    }

    private static void start(MinecraftClient mc) {
        active = true;
        yaw = mc.player.getYaw();
        pitch = mc.player.getPitch();
        before = mc.options.getPerspective();
        if (Modules.FREELOOK.flagOf("third", true)) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    private static void stop(MinecraftClient mc) {
        active = false;
        if (before != null && Modules.FREELOOK.flagOf("third", true)) mc.options.setPerspective(before);
        before = null;
    }
}
