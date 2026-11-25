package dev.pvpmenu.mixin;

import dev.pvpmenu.Visuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fullbright: the brightness option reports a high value while the module is on. Your saved setting is untouched. */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin {
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void pvpmenu$gamma(CallbackInfoReturnable<Object> cir) {
        if (!Visuals.fullbright()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        if ((Object) this == mc.options.getGamma()) cir.setReturnValue(Visuals.gamma());
    }
}
