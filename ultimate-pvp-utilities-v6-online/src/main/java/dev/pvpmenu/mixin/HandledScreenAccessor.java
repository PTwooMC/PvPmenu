package dev.pvpmenu.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the shulker preview and the hotbar key labels read where the inventory window sits. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("focusedSlot")
    Slot pvpmenu$getFocusedSlot();

    @Accessor("x")
    int pvpmenu$getX();

    @Accessor("y")
    int pvpmenu$getY();
}
