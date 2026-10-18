package com.trevorschoeny.headcount.cramming;

import com.trevorschoeny.headcount.HeadcountSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.Team.CollisionRule;

/**
 * The crammed entities under the crosshair, and which one of them the player has picked.
 *
 * <p>"Crammed" is vanilla's own test (LivingEntity.pushEntities): the entities pushing on
 * the one you look at, which are the pushable entities whose bounding boxes overlap its
 * box, not counting passengers. The count is that list plus the entity itself, so it is
 * the number vanilla compares to maxEntityCramming (the readout leaves the limit out). Ten mobs in a row that each touch
 * only their neighbours read 3 from the middle one.
 *
 * <p>Everything is recomputed each frame from vanilla's crosshair pick
 * ({@link #afterPick}); the only state kept between frames is the picked entity's id, so
 * the pick follows that entity while crammed mobs shove each other around.
 */
public final class Cramming {

    private Cramming() {}

    // The crammed entities around what the crosshair is on this frame, ordered by entity
    // id so the order (and "3 / 18") holds still while they shove. Empty when the
    // crosshair isn't on an entity.
    private static List<Entity> crammed = List.of();

    // The entity vanilla's crosshair pick found this frame, before any pick replaces it.
    private static Entity looked;

    // The picked entity's id, or NONE until the wheel is used on these crammed entities.
    private static final int NONE = -1;
    private static int pickedId = NONE;

    // The last entity picked, kept after you look away (pickedId is not), so coming back
    // and scrolling carries on from it: wax number 3, look away, come back, scroll, and
    // number 4 is next. Each entity's number is its place in id order, so it holds as long
    // as the same entities are crammed together; it shifts only when that set changes,
    // and restarting the game renumbers everything.
    private static int lastPickedId = NONE;

    /**
     * Runs right after vanilla's crosshair pick each frame. Records the crammed entities
     * around vanilla's target and, if the player has picked one of them, returns a hit on
     * that entity instead, so every vanilla click path (attack, use, name tag, lead,
     * honeycomb) aims at the pick. The server accepts it: interaction packets name the
     * entity by id and the server checks only reach, which a crammed entity is within
     * because it overlaps the one in reach.
     */
    public static HitResult afterPick(Minecraft mc, HitResult vanilla, float partialTicks) {
        looked = vanilla instanceof EntityHitResult hit ? hit.getEntity() : null;
        crammed = looked == null ? List.of() : crammedWith(looked);

        // A pick ends when you look away or the picked entity is no longer crammed with
        // the one you look at; the wheel goes back to the hotbar.
        Entity picked = picked();
        if (picked == null) {
            pickedId = NONE;
            return vanilla;
        }
        if (picked == looked) return vanilla;
        return new EntityHitResult(picked, aimPoint(mc, picked, partialTicks));
    }

    /** The entity itself plus every entity vanilla's cramming check counts against it. */
    static List<Entity> crammedWith(Entity target) {
        List<Entity> out = new ArrayList<>();
        out.add(target);
        // Not level.getPushableEntities: on the client its predicate admits only the local
        // player, so the server's rule is applied here instead (pushesAgainst).
        out.addAll(target.level().getEntities(target, target.getBoundingBox(),
                e -> !e.isPassenger() && pushesAgainst(target, e)));   // vanilla doesn't count passengers either
        out.sort(Comparator.comparingInt(Entity::getId));
        return out;
    }

    /**
     * EntitySelector.pushableBy as the server runs it: not a spectator, pushable, and the
     * two entities' team collision rules allow it. A team with collisionRule "never" is a
     * common way to stop cramming, so the count has to honour it like vanilla does.
     */
    static boolean pushesAgainst(Entity target, Entity other) {
        Team ownTeam = target.getTeam(), theirTeam = other.getTeam();
        CollisionRule own = ownTeam == null ? CollisionRule.ALWAYS : ownTeam.getCollisionRule();
        CollisionRule their = theirTeam == null ? CollisionRule.ALWAYS : theirTeam.getCollisionRule();
        if (other.isSpectator() || !other.isPushable()) return false;
        if (own == CollisionRule.NEVER || their == CollisionRule.NEVER) return false;
        boolean sameTeam = ownTeam != null && ownTeam.isAlliedTo(theirTeam);
        if ((own == CollisionRule.PUSH_OWN_TEAM || their == CollisionRule.PUSH_OWN_TEAM) && sameTeam) return false;
        return own != CollisionRule.PUSH_OTHER_TEAMS && their != CollisionRule.PUSH_OTHER_TEAMS || sameTeam;
    }

    /**
     * The mouse wheel, while the crosshair is on two or more crammed entities: steps the
     * pick and returns true (the hotbar doesn't move). Otherwise false, and vanilla
     * scrolls the hotbar. Steps the same direction the hotbar would, wrapping at the ends.
     */
    public static boolean scroll(int wheel) {
        if (!HeadcountSettings.scrollToPick() || crammed.size() < 2) return false;
        // The first notch carries on from the last pick if it is still here, else starts
        // from the entity the crosshair is on.
        int from = pickedId != NONE ? indexOf(pickedId)
                : indexOf(lastPickedId) >= 0 ? indexOf(lastPickedId)
                : lookedIndex();
        int to = ScrollWheelHandler.getNextScrollWheelSelection(wheel, from, crammed.size());
        pickedId = lastPickedId = crammed.get(to).getId();
        return true;
    }

    /** True for the picked entity, which vanilla then draws with an outline. */
    public static boolean isPicked(Entity e) {
        return pickedId != NONE && e.getId() == pickedId;
    }

    /** How many entities are crammed together under the crosshair (0 when none). */
    public static int count() { return crammed.size(); }

    /** The pick's 1-based place among the crammed entities, or 0 before the wheel is used. */
    public static int pickNumber() {
        return pickedId == NONE ? 0 : indexOf(pickedId) + 1;
    }

    /** The picked entity, or null before the wheel is used on these crammed entities. */
    public static Entity picked() {
        if (pickedId == NONE) return null;
        for (Entity e : crammed) if (e.getId() == pickedId) return e;
        return null;
    }

    // The place of entity `id` among the crammed entities, or -1 if it isn't one of them.
    private static int indexOf(int id) {
        for (int i = 0; i < crammed.size(); i++) if (crammed.get(i).getId() == id) return i;
        return -1;
    }

    private static int lookedIndex() {
        return Math.max(crammed.indexOf(looked), 0);
    }

    // Where the click lands on the picked entity: where the view ray crosses its box, or
    // its centre when the ray passes beside it. Some entities (armor stands) use this point.
    private static Vec3 aimPoint(Minecraft mc, Entity picked, float partialTicks) {
        Entity camera = mc.getCameraEntity();
        AABB box = picked.getBoundingBox();
        if (camera == null) return box.getCenter();
        Vec3 eye = camera.getEyePosition(partialTicks);
        Vec3 far = eye.add(camera.getViewVector(partialTicks).scale(16));
        return box.clip(eye, far).orElse(box.getCenter());
    }
}
