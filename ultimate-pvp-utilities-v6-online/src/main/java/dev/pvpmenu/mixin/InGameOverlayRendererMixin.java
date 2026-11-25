package dev.pvpmenu.mixin;

import dev.pvpmenu.Visuals;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lower Fire: shift the matrix stack down before the fire overlay is drawn and shift it back afterwards,
 * so nothing drawn later is affected. Only the MatrixStack argument is used.
 */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    private static MatrixStack pvpmenu$shifted;
    private static float pvpmenu$amount;

    @ModifyVariable(method = "renderFireOverlay", at = @At("HEAD"), argsOnly = true)
    private static MatrixStack pvpmenu$lowerFire(MatrixStack matrices) {
        if (Visuals.lowFire()) {
            pvpmenu$amount = Visuals.fireDrop();
            pvpmenu$shifted = matrices;
            matrices.translate(0f, -pvpmenu$amount, 0f);
        }
        return matrices;
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void pvpmenu$restore(CallbackInfo ci) {
        if (pvpmenu$shifted != null) {
            pvpmenu$shifted.translate(0f, pvpmenu$amount, 0f);
            pvpmenu$shifted = null;
        }
    }
}
