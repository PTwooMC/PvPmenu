package dev.pvpmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only numbers for the info cards: clicks per second, combo, last target, speed. Everything here only
 * observes what you already do (mouse polling, vanilla hit results); nothing sends or changes any input.
 */
public final class Stats {
    // ---- clicks per second (polled every frame so fast clicking is not missed)
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean lastLeft;
    private static boolean lastRight;
    private static long sessionStart;
    private static int sessionClicks;
    private static long lastClick;
    private static float peak;

    // ---- combo
    private static final class Pending {
        final Entity target;
        final int tick;
        boolean counted;
        Pending(Entity t, int tick) { this.target = t; this.tick = tick; }
    }
    private static final List<Pending> PENDING = new ArrayList<>();
    private static int tickCounter;
    private static int combo;
    private static int bestCombo;
    private static long lastComboHit;
    private static int lastPlayerHurt;

    // ---- target
    private static Entity lastTarget;
    private static long lastTargetAt;

    // ---- speed
    private static double prevX;
    private static double prevZ;
    private static boolean havePrev;
    private static float speed;

    private Stats() {}

    // ------------------------------------------------------------------ clicks

    /** Call once per rendered frame. */
    public static void frame(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        boolean l = false;
        boolean r = false;
        if (mc.currentScreen == null && mc.getWindow() != null) {
            long h = mc.getWindow().getHandle();
            l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        }
        if (l && !lastLeft) click(LEFT, now);
        if (r && !lastRight) click(RIGHT, now);
        lastLeft = l;
        lastRight = r;
        trim(LEFT, now);
        trim(RIGHT, now);
        float c = Math.max(cps(true), cps(false));
        if (c > peak) peak = c;
        if (now - lastClick > 10000) peak = 0f;
    }

    private static void click(ArrayDeque<Long> q, long now) {
        q.addLast(now);
        if (now - lastClick > 2000) {
            sessionStart = now;
            sessionClicks = 0;
        }
        sessionClicks++;
        lastClick = now;
    }

    private static void trim(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    public static int cps(boolean left) {
        return (left ? LEFT : RIGHT).size();
    }

    public static boolean leftDown() { return lastLeft; }
    public static boolean rightDown() { return lastRight; }

    /** Average CPS over the current burst of clicking. */
    public static float averageCps() {
        if (System.currentTimeMillis() - lastClick > 2000 || sessionClicks < 2) return 0f;
        float secs = Math.max(0.2f, (lastClick - sessionStart) / 1000f);
        return (sessionClicks - 1) / secs;
    }

    public static float peakCps() { return peak; }

    // ------------------------------------------------------------------- combo

    public static ActionResult onAttack(PlayerEntity player, World world, Hand hand, Entity entity, HitResult hit) {
        if (world.isClient() && entity instanceof LivingEntity) {
            PENDING.add(new Pending(entity, tickCounter));
            lastTarget = entity;
            lastTargetAt = System.currentTimeMillis();
        }
        return ActionResult.PASS;
    }

    public static int combo() { return combo; }
    public static int bestCombo() { return bestCombo; }

    // -------------------------------------------------------------------- tick

    public static void tick(MinecraftClient mc) {
        tickCounter++;
        if (mc.player == null) {
            PENDING.clear();
            havePrev = false;
            combo = 0;
            return;
        }

        // speed in blocks per second
        double x = mc.player.getX();
        double z = mc.player.getZ();
        if (havePrev) {
            double dx = x - prevX;
            double dz = z - prevZ;
            speed = speed * 0.6f + (float) (Math.sqrt(dx * dx + dz * dz) * 20.0) * 0.4f;
        }
        prevX = x;
        prevZ = z;
        havePrev = true;

        // combo: an attack counts once the target shows its hurt flash
        for (int i = PENDING.size() - 1; i >= 0; i--) {
            Pending p = PENDING.get(i);
            if (p.target instanceof LivingEntity le && le.hurtTime > 0 && !p.counted) {
                p.counted = true;
                combo++;
                if (combo > bestCombo) bestCombo = combo;
                lastComboHit = System.currentTimeMillis();
            }
            if (tickCounter - p.tick > 5) PENDING.remove(i);
        }
        int hurt = mc.player.hurtTime;
        if (hurt > 0 && lastPlayerHurt == 0) combo = 0;
        lastPlayerHurt = hurt;
        if (combo > 0 && System.currentTimeMillis() - lastComboHit > 3000) combo = 0;
    }

    public static float speed() { return speed; }

    // ------------------------------------------------------------------ target

    /** The entity you last hit (for three seconds) or the one under your crosshair. */
    public static LivingEntity target(MinecraftClient mc) {
        if (lastTarget instanceof LivingEntity le && le.isAlive()
                && System.currentTimeMillis() - lastTargetAt < 3000) {
            return le;
        }
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }
}
