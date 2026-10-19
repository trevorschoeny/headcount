package com.trevorschoeny.headcount.ranges;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * What one range counts: entities of a category within a sphere of {@code radius}
 * blocks around the player, less the entity types unticked in its second dropdown.
 *
 * <p>The filter keeps the unticked types, not the ticked ones, so "all ticked" (the
 * default) is an empty list, and an entity type added later (a new mod) is counted
 * until someone unticks it.
 */
public record RangeSpec(int radius, Category category, List<Identifier> excluded) {

    public static final int MAX_RADIUS = 256;
    static final int MAX_EXCLUDED = 1024;

    public static final StreamCodec<RegistryFriendlyByteBuf, RangeSpec> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RangeSpec::radius,
            ByteBufCodecs.idMapper(i -> Category.values()[i], Category::ordinal), RangeSpec::category,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_EXCLUDED)), RangeSpec::excluded,
            RangeSpec::new);

    public RangeSpec {
        excluded = List.copyOf(excluded);
    }

    public static RangeSpec of(int radius, Category category) {
        return new RangeSpec(radius, category, List.of());
    }

    public boolean counts(EntityType<?> type) {
        return category.includes(type) && !excluded.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }
}
