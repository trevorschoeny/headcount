package com.trevorschoeny.headcount.mixin;

import com.trevorschoeny.headcount.cramming.Cramming;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two hooks on Minecraft for scrolling through crammed entities:
 * after vanilla's per-frame crosshair pick, the picked entity replaces vanilla's target,
 * so clicks go to it; and the picked entity is drawn with vanilla's glowing outline.
 */
@Mixin(Minecraft.class)
abstract class MinecraftPickMixin {
    @Shadow public HitResult hitResult;
    @Shadow public Entity crosshairPickEntity;

    @Inject(method = "pick", at = @At("TAIL"))
    private void headcount$pickCrammed(float partialTicks, CallbackInfo ci) {
        if (hitResult == null) return;   // no level or player: vanilla picked nothing
        hitResult = Cramming.afterPick((Minecraft) (Object) this, hitResult, partialTicks);
        crosshairPickEntity = hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void headcount$outlinePicked(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Cramming.isPicked(entity)) cir.setReturnValue(true);
    }
}
