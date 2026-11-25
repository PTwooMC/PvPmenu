package dev.pvpmenu.mixin;

import dev.pvpmenu.Visuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Faster Swing Animation: only the look of your own arm swing. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void pvpmenu$swing(CallbackInfoReturnable<Integer> cir) {
        if (!Visuals.fastSwing()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && (Object) this == mc.player) {
            cir.setReturnValue(Math.min(cir.getReturnValue(), Visuals.swingTicks()));
        }
    }
}
