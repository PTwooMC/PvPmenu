package dev.pvpmenu.mixin;

import dev.pvpmenu.feature.Freelook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.Camera;

/** Freelook: after the camera copies your rotation, swap in the freelook rotation. */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", shift = At.Shift.AFTER))
    private void pvpmenu$freelook(CallbackInfo ci) {
        Freelook.cameraHooked = true;
        if (Freelook.active()) setRotation(Freelook.yaw(), Freelook.pitch());
    }

    /** Remember the exact partial-tick value the world is drawn with, so overlays line up with it. */
    @Inject(method = "update", at = @At("HEAD"))
    private void pvpmenu$delta(net.minecraft.world.World area, net.minecraft.entity.Entity focused,
                               boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        dev.pvpmenu.feature.TntTimer.cameraDelta = tickProgress;
        dev.pvpmenu.feature.TntTimer.deltaSeen = true;
    }
}
