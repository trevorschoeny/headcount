package com.trevorschoeny.headcount.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.trevorschoeny.headcount.cramming.Cramming;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the crosshair is on crammed entities, the mouse wheel steps through them instead
 * of the hotbar. Hooked at the one call where vanilla turns the wheel into a hotbar slot,
 * so screens, spectator mode and flying speed are untouched, and number keys still switch
 * the hotbar.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Inject(method = "onScroll", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
    private void headcount$scrollCrammed(long handle, double xoffset, double yoffset, CallbackInfo ci,
                                         @Local(ordinal = 0) int wheel) {
        if (Cramming.scroll(wheel)) ci.cancel();
    }
}
