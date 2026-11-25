package dev.pvpmenu;

import dev.pvpmenu.module.Modules;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

/** Values read by the mixins and the totem animation. Kept in one place so the mixins stay tiny. */
public final class Visuals {
    private static final float[] SHIELD_DROP = {0.08f, 0.16f, 0.26f, 0.36f};
    private static final float[] FIRE_DROP = {0.10f, 0.20f, 0.30f, 0.40f};
    private static final float[] TOTEM_SCALE = {2f, 3f, 4f, 6f};

    private Visuals() {}

    public static boolean lowShield() { return Modules.LOW_SHIELD.isEnabled(); }
    public static float shieldDrop() { return SHIELD_DROP[Modules.LOW_SHIELD.level()]; }

    public static boolean lowFire() { return Modules.LOW_FIRE.isEnabled(); }
    public static float fireDrop() { return FIRE_DROP[Modules.LOW_FIRE.level()]; }

    public static boolean smallTotem() { return Modules.SMALL_TOTEM.isEnabled(); }
    public static float totemScale() { return TOTEM_SCALE[Modules.SMALL_TOTEM.level()]; }

    // ---- round 2 and 3

    private static final double[] GAMMA = {3.0, 8.0, 15.0};
    private static final float[] EXPLOSION_VOLUME = {0.6f, 0.35f, 0.15f};
    private static final float[] SHIELD_VOLUME = {0.6f, 0.3f, 0.0f};
    private static final float[] EXPLOSION_DROP = {0.5f, 0.8f, 0.97f};
    private static final float[] TOTEM_DROP = {0.5f, 0.8f, 0.95f};
    private static final float[] HIDE_DROP = {0.4f, 0.8f, 1.0f};
    private static final int[] SWING = {5, 4, 3};

    public static boolean fullbright() { return Modules.FULLBRIGHT.isEnabled(); }
    public static double gamma() { return GAMMA[Modules.FULLBRIGHT.level()]; }

    /** Volume multiplier for a sound path, or 1 if it is not one we change. */
    public static float soundFactor(String path) {
        if (path.equals("entity.generic.explode") && Modules.QUIET_EXPLOSIONS.isEnabled()) {
            return EXPLOSION_VOLUME[Modules.QUIET_EXPLOSIONS.level()];
        }
        if ((path.equals("item.shield.block") || path.equals("item.shield.break")) && Modules.SHIELDFIX.isEnabled()) {
            return SHIELD_VOLUME[Modules.SHIELDFIX.level()];
        }
        return 1f;
    }

    /** True if this particle should be skipped. */
    public static boolean dropParticle(ParticleEffect effect) {
        float p = 0f;
        ParticleType<?> t = effect.getType();
        if ((t == ParticleTypes.EXPLOSION || t == ParticleTypes.EXPLOSION_EMITTER) && Modules.SMALL_EXPLOSIONS.isEnabled()) {
            p = Math.max(p, EXPLOSION_DROP[Modules.SMALL_EXPLOSIONS.level()]);
        }
        if (t == ParticleTypes.TOTEM_OF_UNDYING && Modules.FEW_TOTEM.isEnabled()) {
            p = Math.max(p, TOTEM_DROP[Modules.FEW_TOTEM.level()]);
        }
        if (Modules.HIDE_PARTICLES.isEnabled()) p = Math.max(p, HIDE_DROP[Modules.HIDE_PARTICLES.level()]);
        return p > 0f && Math.random() < p;
    }

    public static boolean handPos() { return Modules.HANDPOS.isEnabled(); }
    public static float handX() { return Modules.HANDPOS.optOf("x", 0) / 100f; }
    public static float handY() { return Modules.HANDPOS.optOf("y", 0) / 100f; }
    public static float handZ() { return Modules.HANDPOS.optOf("z", 0) / 100f; }
    public static float handSize() { return Modules.HANDPOS.optOf("size", 100) / 100f; }

    public static boolean noEquip() { return Modules.NOEQUIP.isEnabled(); }

    public static boolean fastSwing() { return Modules.FASTSWING.isEnabled(); }
    public static int swingTicks() { return SWING[Modules.FASTSWING.level()]; }
}
