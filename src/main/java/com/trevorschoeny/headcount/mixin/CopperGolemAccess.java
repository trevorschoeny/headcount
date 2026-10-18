package com.trevorschoeny.headcount.mixin;

import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether a copper golem is waxed: vanilla keeps it only in this private field. */
@Mixin(CopperGolem.class)
public interface CopperGolemAccess {
    @Accessor("nextWeatheringTick")
    long headcount$nextWeatheringTick();
}
