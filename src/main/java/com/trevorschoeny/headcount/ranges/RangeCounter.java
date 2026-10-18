package com.trevorschoeny.headcount.ranges;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/** Counts the entities around a player for each range: the server half's one job. */
final class RangeCounter {

    private RangeCounter() {}

    /**
     * One count per range, in order. A single entity lookup covers the largest range;
     * each entity is then tested against every range by squared distance from the
     * player's position (feet), so a range is a true sphere. The player is never counted.
     */
    static List<Integer> count(ServerPlayer player, List<RangeSpec> ranges) {
        int[] counts = new int[ranges.size()];
        int[] radii = new int[ranges.size()];
        int largest = 0;
        for (int i = 0; i < ranges.size(); i++) {
            radii[i] = Math.clamp(ranges.get(i).radius(), 1, RangeSpec.MAX_RADIUS);
            largest = Math.max(largest, radii[i]);
        }
        for (Entity e : player.level().getEntities(player, new AABB(player.blockPosition()).inflate(largest), e -> true)) {
            double d2 = e.distanceToSqr(player);
            for (int i = 0; i < counts.length; i++) {
                if (d2 <= (double) radii[i] * radii[i] && ranges.get(i).counts(e.getType())) counts[i]++;
            }
        }
        List<Integer> out = new ArrayList<>(counts.length);
        for (int c : counts) out.add(c);
        return out;
    }
}
