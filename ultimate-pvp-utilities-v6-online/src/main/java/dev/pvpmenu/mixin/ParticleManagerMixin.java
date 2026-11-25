package dev.pvpmenu.mixin;

import dev.pvpmenu.Visuals;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hide particles, smaller explosions and fewer totem particles all drop particles here. */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void pvpmenu$drop(ParticleEffect effect, double x, double y, double z, double vx, double vy, double vz,
                              CallbackInfoReturnable<Particle> cir) {
        if (Visuals.dropParticle(effect)) cir.setReturnValue(null);
    }
}
