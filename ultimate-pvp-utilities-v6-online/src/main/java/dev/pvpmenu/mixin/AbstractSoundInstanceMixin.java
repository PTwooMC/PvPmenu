package dev.pvpmenu.mixin;

import dev.pvpmenu.Visuals;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quieter explosions and softer shield sounds. */
@Mixin(AbstractSoundInstance.class)
public abstract class AbstractSoundInstanceMixin {
    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void pvpmenu$volume(CallbackInfoReturnable<Float> cir) {
        String path = ((SoundInstance) (Object) this).getId().getPath();
        float f = Visuals.soundFactor(path);
        if (f != 1f) cir.setReturnValue(cir.getReturnValueF() * f);
    }
}
