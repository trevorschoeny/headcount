package com.trevorschoeny.headcount.cramming;

import com.trevorschoeny.headcount.HeadcountSettings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The crammed entities under the crosshair, and which one of them the player has picked.
 *
 * <p>"Crammed" is vanilla's own test (LivingEntity.pushEntities): the entities pushing on
 * the one you look at, which are the pushable entities whose bounding boxes overlap its
 * box, not counting passengers. The count is that list plus the entity itself, so it is
 * the number vanilla compares to maxEntityCramming. Ten mobs in a row that each touch
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
        for (Entity e : target.level().getPushableEntities(target, target.getBoundingBox())) {
            if (!e.isPassenger()) out.add(e);   // vanilla doesn't count passengers either
        }
        out.sort(Comparator.comparingInt(Entity::getId));
        return out;
    }

    /**
     * The mouse wheel, while the crosshair is on two or more crammed entities: steps the
     * pick and returns true (the hotbar doesn't move). Otherwise false, and vanilla
     * scrolls the hotbar. Steps the same direction the hotbar would, wrapping at the ends.
     */
    public static boolean scroll(int wheel) {
        if (!HeadcountSettings.scrollToPick() || crammed.size() < 2) return false;
        // The first notch starts from the entity the crosshair is on.
        int from = pickedId == NONE ? lookedIndex() : pickedIndex();
        int to = ScrollWheelHandler.getNextScrollWheelSelection(wheel, from, crammed.size());
        pickedId = crammed.get(to).getId();
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
        return pickedId == NONE ? 0 : pickedIndex() + 1;
    }

    /**
     * The maxEntityCramming value to count against. The game rule isn't sent to clients:
     * in single player it is read from the integrated server; elsewhere it is vanilla's
     * default until Headcount's server half sends the real value.
     */
    public static int limit(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        return server != null
                ? server.getGameRules().get(GameRules.MAX_ENTITY_CRAMMING)
                : HeadcountSettings.DEFAULT_CRAMMING_LIMIT;
    }

    private static Entity picked() {
        if (pickedId == NONE) return null;
        for (Entity e : crammed) if (e.getId() == pickedId) return e;
        return null;
    }

    private static int pickedIndex() {
        for (int i = 0; i < crammed.size(); i++) if (crammed.get(i).getId() == pickedId) return i;
        return 0;
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
