package com.trevorschoeny.headcount.ranges;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * The first dropdown of a range's filter: which kind of entity it counts. Decided by
 * entity type alone, so the second dropdown (the types in the category) and the
 * server's count agree on every entity, modded ones included.
 *
 * <ul>
 *   <li>Mobs: living entities other than players, armor stands and mannequins.
 *       "Living" is "has default attributes", which every living type registers.</li>
 *   <li>Hostile: mobs in vanilla's MONSTER spawn category, the one the hostile mob cap
 *       counts.</li>
 *   <li>Passive: every other mob (animals, villagers, golems, fish, bats).</li>
 *   <li>Non-mobs: everything else: items, XP orbs, minecarts, boats, projectiles,
 *       displays, armor stands, other players.</li>
 * </ul>
 */
public enum Category {
    ALL("All"), MOBS("Mobs"), HOSTILE("Hostile"), PASSIVE("Passive"), NON_MOBS("Non-mobs");

    public final String label;

    Category(String label) { this.label = label; }

    public boolean includes(EntityType<?> type) {
        return switch (this) {
            case ALL -> true;
            case MOBS -> isMob(type);
            case HOSTILE -> isMob(type) && type.getCategory() == MobCategory.MONSTER;
            case PASSIVE -> isMob(type) && type.getCategory() != MobCategory.MONSTER;
            case NON_MOBS -> !isMob(type);
        };
    }

    static boolean isMob(EntityType<?> type) {
        return DefaultAttributes.hasSupplier(type)
                && type != EntityTypes.PLAYER && type != EntityTypes.ARMOR_STAND && type != EntityTypes.MANNEQUIN;
    }
}
