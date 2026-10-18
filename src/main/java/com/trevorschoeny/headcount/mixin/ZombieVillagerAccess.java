package com.trevorschoeny.headcount.mixin;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How long a curing zombie villager has left: vanilla keeps it only in this private field. */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerAccess {
    @Accessor("villagerConversionTime")
    int headcount$villagerConversionTime();
}
