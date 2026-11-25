package dev.pvpmenu.mixin;

import dev.pvpmenu.Visuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lower Shield: after vanilla positions a first-person item, push a held shield further down. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private ItemStack mainHand;
    @Shadow private ItemStack offHand;

    /** No Item Switch Animation: keep the held items fully raised and always up to date. */
    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void pvpmenu$noEquip(CallbackInfo ci) {
        if (!Visuals.noEquip()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mainHand = mc.player.getMainHandStack();
        offHand = mc.player.getOffHandStack();
        equipProgressMainHand = 1f;
        prevEquipProgressMainHand = 1f;
        equipProgressOffHand = 1f;
        prevEquipProgressOffHand = 1f;
    }

    @Inject(method = "applyEquipOffset", at = @At("TAIL"))
    private void pvpmenu$handPosition(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        if (!Visuals.handPos()) return;
        float side = arm == Arm.RIGHT ? 1f : -1f;
        matrices.translate(Visuals.handX() * side, Visuals.handY(), Visuals.handZ());
        float s = Visuals.handSize();
        matrices.scale(s, s, s);
    }

    @Inject(method = "applyEquipOffset", at = @At("TAIL"))
    private void pvpmenu$lowerShield(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        if (!Visuals.lowShield()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Hand hand = arm == mc.player.getMainArm() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (mc.player.getStackInHand(hand).isOf(Items.SHIELD)) {
            matrices.translate(0f, -Visuals.shieldDrop(), 0f);
        }
    }
}
