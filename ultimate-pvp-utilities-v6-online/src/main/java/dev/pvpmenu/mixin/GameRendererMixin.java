package dev.pvpmenu.mixin;

import dev.pvpmenu.TotemPop;
import dev.pvpmenu.feature.Zoom;
import dev.pvpmenu.Visuals;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Small Totem Pop: swap the full-screen totem animation for our smaller one. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void pvpmenu$smallTotem(ItemStack stack, CallbackInfo ci) {
        if (Visuals.smallTotem() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
            TotemPop.start(stack);
            ci.cancel();
        }
    }

    /** Zoom: narrow the field of view while the zoom key is held. */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void pvpmenu$zoom(CallbackInfoReturnable<Float> cir) {
        if (Zoom.active()) cir.setReturnValue(cir.getReturnValueF() / Zoom.factor());
        dev.pvpmenu.feature.TntTimer.noteFov(cir.getReturnValueF());
    }
}
