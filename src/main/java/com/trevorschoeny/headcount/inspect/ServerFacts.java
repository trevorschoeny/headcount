package com.trevorschoeny.headcount.inspect;

import com.trevorschoeny.headcount.mixin.CopperGolemAccess;
import com.trevorschoeny.headcount.mixin.ZombieVillagerAccess;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;

/**
 * What only the server knows about an entity, for the info readout. Each field has a
 * "doesn't apply" value, so one record covers every kind of entity.
 *
 * @param entityId  which entity these facts are about
 * @param effects   active effects (the client only ever sees their particle colours)
 * @param armor     armor points
 * @param waxed     a copper golem that has been waxed (the client sees its stage only)
 * @param persistent a mob that would despawn but won't (name tag, picked-up item, leash)
 * @param age       AgeableMob age: negative is ticks until grown, positive is ticks until
 *                  it can breed again; {@link #NO_AGE} when not ageable
 * @param eggTicks  a chicken's ticks until its next egg, or -1
 * @param cureTicks a curing zombie villager's ticks until cured, or -1
 * @param owner     a tamed animal's owner's name, or "" (unknown or untamed)
 * @param speed     a horse-like's movement speed attribute, or -1
 * @param jump      a horse-like's jump strength attribute, or -1
 */
public record ServerFacts(int entityId, List<MobEffectInstance> effects, int armor, boolean waxed,
                          boolean persistent, int age, int eggTicks, int cureTicks, String owner,
                          double speed, double jump) {

    public static final int NO_AGE = Integer.MIN_VALUE;

    // Vanilla's marker for a golem that no longer weathers, which is what waxing sets.
    private static final long WAXED_TICK = -2L;

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerFacts> STREAM_CODEC = StreamCodec.of(
            (buf, f) -> {
                buf.writeVarInt(f.entityId);
                MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, f.effects);
                buf.writeVarInt(f.armor);
                buf.writeBoolean(f.waxed);
                buf.writeBoolean(f.persistent);
                buf.writeInt(f.age);
                buf.writeInt(f.eggTicks);
                buf.writeInt(f.cureTicks);
                buf.writeUtf(f.owner);
                buf.writeDouble(f.speed);
                buf.writeDouble(f.jump);
            },
            buf -> new ServerFacts(buf.readVarInt(),
                    MobEffectInstance.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readUtf(), buf.readDouble(), buf.readDouble()));

    /** Reads the facts off a live entity, on the server. */
    static ServerFacts of(Entity e, ServerLevel level) {
        LivingEntity living = e instanceof LivingEntity l ? l : null;
        return new ServerFacts(
                e.getId(),
                living == null ? List.of() : List.copyOf(living.getActiveEffects()),
                living == null ? 0 : living.getArmorValue(),
                e instanceof CopperGolem g && ((CopperGolemAccess) g).headcount$nextWeatheringTick() == WAXED_TICK,
                // Only worth saying for a mob of a kind that despawns (a zombie, not a cow).
                e instanceof Mob m && m.removeWhenFarAway(Double.MAX_VALUE)
                        && (m.isPersistenceRequired() || m.requiresCustomPersistence()),
                e instanceof AgeableMob a ? a.getAge() : NO_AGE,
                e instanceof Chicken c && !c.isBaby() ? c.eggTime : -1,
                e instanceof ZombieVillager z && z.isConverting() ? ((ZombieVillagerAccess) z).headcount$villagerConversionTime() : -1,
                ownerName(e, level),
                e instanceof AbstractHorse h ? h.getAttributeValue(Attributes.MOVEMENT_SPEED) : -1,
                e instanceof AbstractHorse h ? h.getAttributeValue(Attributes.JUMP_STRENGTH) : -1);
    }

    // The owner's name when they're online; offline owners show no owner line.
    // ponytail: online only; add the server's name cache if offline owners matter.
    private static String ownerName(Entity e, ServerLevel level) {
        if (!(e instanceof OwnableEntity o)) return "";
        EntityReference<LivingEntity> ref = o.getOwnerReference();
        if (ref == null) return "";
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ref.getUUID());
        return owner == null ? "" : owner.getPlainTextName();
    }
}
