package dev.pvpmenu.feature;

import dev.pvpmenu.PvpMenuClient;
import dev.pvpmenu.module.Modules;
import net.minecraft.client.MinecraftClient;

/** Hold a key to zoom in. The field of view change is applied by GameRendererMixin. */
public final class Zoom {
    private static float current = 1f;

    private Zoom() {}

    public static void tick(MinecraftClient mc) {
        boolean on = Modules.ZOOM.isEnabled() && mc.currentScreen == null && PvpMenuClient.ZOOM_KEY.isPressed();
        float target = on ? Math.max(1.5f, Modules.ZOOM.optOf("amount", 40) / 10f) : 1f;
        boolean smooth = Modules.ZOOM.flagOf("smooth", true);
        current = smooth ? current + (target - current) * 0.35f : target;
        if (Math.abs(current - target) < 0.01f) current = target;
    }

    public static boolean active() { return current > 1.01f; }
    public static float factor() { return current; }
}
