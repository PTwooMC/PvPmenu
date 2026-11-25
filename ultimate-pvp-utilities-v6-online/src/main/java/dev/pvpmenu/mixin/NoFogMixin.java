package dev.pvpmenu.mixin;

import dev.pvpmenu.module.Modules;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Fog: removes the distance haze. Only the fog distances are changed; the fog system itself keeps running normally. */
@Mixin(targets = "net.minecraft.client.render.fog.AtmosphericFogModifier")
public abstract class NoFogMixin {
    @Inject(method = "applyStartEndModifier", at = @At("RETURN"))
    private void pvpmenu$clear(FogData data, Entity cameraEntity, BlockPos cameraPos, ClientWorld world,
                               float viewDistance, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!Modules.NOFOG.isEnabled()) return;
        float far = Math.max(viewDistance, 64f) * 4f;
        data.environmentalStart = far;
        data.environmentalEnd = far + 16f;
        data.renderDistanceStart = far; data.renderDistanceEnd = far + 16f;
    }
}
