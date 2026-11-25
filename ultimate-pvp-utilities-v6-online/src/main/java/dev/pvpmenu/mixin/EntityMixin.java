package dev.pvpmenu.mixin;

import dev.pvpmenu.feature.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: while active, mouse movement turns the camera instead of your character. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void pvpmenu$freelook(double dx, double dy, CallbackInfo ci) {
        if (!Freelook.capturing()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && (Object) this == mc.player) {
            Freelook.look(dx, dy);
            ci.cancel();
        }
    }
}
