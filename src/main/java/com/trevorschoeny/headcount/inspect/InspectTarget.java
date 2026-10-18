package com.trevorschoeny.headcount.inspect;

import com.trevorschoeny.headcount.cramming.Cramming;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Which entity the info readout is about, and the server's latest facts on it.
 *
 * <p>The target is whatever the crosshair is on, except in a cram: there it is the
 * picked entity, and nothing at all until something is picked (Trev, 2026-10-18), so a
 * pile of mobs shows just its count and "Scroll to select".
 *
 * <p>When the server has Headcount, the client asks about the target when it changes and
 * every half second after, so timers (effects, breeding, eggs) stay current.
 */
public final class InspectTarget {

    private InspectTarget() {}

    private static final int REFRESH_TICKS = 10;

    private static ServerFacts facts;
    private static int askedId = -1;
    private static int ticksSinceAsk;

    /** The entity to show info for, or null. */
    public static Entity target() {
        if (Cramming.count() >= 2) return Cramming.picked();
        return Minecraft.getInstance().crosshairPickEntity;
    }

    /** Each client tick: ask the server about the target when due. */
    public static void tick() {
        Entity t = target();
        // A server without Headcount never registered the ask, so it can't be sent.
        if (t == null || !ClientPlayNetworking.canSend(InspectNet.Ask.TYPE)) {
            askedId = -1;
            return;
        }
        if (t.getId() != askedId || ++ticksSinceAsk >= REFRESH_TICKS) {
            ClientPlayNetworking.send(new InspectNet.Ask(t.getId()));
            askedId = t.getId();
            ticksSinceAsk = 0;
        }
    }

    public static void received(ServerFacts answer) { facts = answer; }

    public static void reset() { facts = null; askedId = -1; }

    /** The server's facts on {@code e}, or null (no server half, or not answered yet). */
    public static ServerFacts factsFor(Entity e) {
        return e != null && facts != null && facts.entityId() == e.getId() ? facts : null;
    }
}
