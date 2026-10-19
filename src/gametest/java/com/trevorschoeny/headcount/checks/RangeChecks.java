package com.trevorschoeny.headcount.checks;

import com.google.gson.JsonArray;
import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;
import com.trevorschoeny.headcount.ranges.Category;
import com.trevorschoeny.headcount.ranges.RangeClient;
import com.trevorschoeny.headcount.ranges.RangeSpec;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;
import testgallery.SceneWorld;

import java.util.ArrayList;
import java.util.List;

/** Checks for the ranges scene (test-gallery/scenes/headcount/ranges.json). */
public class RangeChecks implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "headcount", "ranges")) {
            countsEachRange(ctx, scene);
        }
    }

    /**
     * Station "headcount:range_counts": standing at z 2 in front of the station, the
     * scene's mobs are at known distances straight ahead: 3 pigs at 4, 2 creepers at 6,
     * a husk at 8, 3 creepers at 25, 2 cows at 38, 4 creepers at 78. Each range in the
     * list below reads what those distances say. The station's floating labels are
     * non-mobs, so those counts are taken from the world rather than assumed.
     */
    private static void countsEachRange(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        JsonArray o = scene.station("headcount:range_counts").getAsJsonArray("origin");
        Vec3 stand = new Vec3(o.get(0).getAsDouble() + 2.5, o.get(1).getAsDouble(), 2.0);
        scene.world().getServer().runCommand("tp @p " + stand.x + " " + stand.y + " " + stand.z + " 0 0");
        ctx.waitTicks(5);

        List<RangeSpec> specs = List.of(
                RangeSpec.of(16, Category.ALL),
                RangeSpec.of(16, Category.HOSTILE),
                RangeSpec.of(32, Category.HOSTILE),
                RangeSpec.of(128, Category.HOSTILE),
                RangeSpec.of(64, Category.PASSIVE),
                new RangeSpec(128, Category.HOSTILE, List.of(Identifier.withDefaultNamespace("creeper"))),
                RangeSpec.of(16, Category.NON_MOBS));
        List<RangeEntry> entries = new ArrayList<>();
        for (RangeSpec s : specs) entries.add(HeadcountSettings.entry(s, true));
        ctx.runOnClient(mc -> HeadcountSettings.setRanges(entries));
        ctx.waitTicks(30);   // the watch, then at least one round of counts

        int labels = scene.world().getServer().computeOnServer(server -> server.overworld()
                .getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(stand, stand).inflate(16),
                        e -> e instanceof Display && e.position().distanceToSqr(stand) <= 16 * 16).size());
        List<Integer> expected = List.of(6 + labels, 3, 6, 10, 5, 1, labels);
        List<Integer> got = ctx.computeOnClient(mc -> {
            List<Integer> out = new ArrayList<>();
            for (int i = 0; i < specs.size(); i++) out.add(RangeClient.count(i));
            return out;
        });
        ctx.takeScreenshot("headcount-range-counts");
        if (!got.equals(expected)) throw new AssertionError("range counts: expected " + expected + ", got " + got);

        // Shots of the default list in each position the setting offers (for review; asserts nothing).
        scene.world().getServer().runCommand("gamerule send_command_feedback false");
        ctx.runOnClient(mc -> {
            mc.gui.hud.getChat().clearMessages(false);
            // The defaults, plus a line for each look: passive's tint, a filtered range,
            // and an empty range (no mob within 2 blocks).
            HeadcountSettings.setRanges(List.of(
                    HeadcountSettings.entry(RangeSpec.of(16, Category.ALL), true),
                    HeadcountSettings.entry(RangeSpec.of(32, Category.HOSTILE), true),
                    HeadcountSettings.entry(RangeSpec.of(128, Category.HOSTILE), true),
                    HeadcountSettings.entry(RangeSpec.of(64, Category.PASSIVE), true),
                    HeadcountSettings.entry(new RangeSpec(128, Category.HOSTILE, List.of(Identifier.withDefaultNamespace("creeper"))), true),
                    HeadcountSettings.entry(RangeSpec.of(2, Category.MOBS), true)));
        });
        ctx.waitTicks(30);
        for (HeadcountSettings.HudPosition pos : HeadcountSettings.HudPosition.values()) {
            ctx.runOnClient(mc -> HeadcountSettings.setRangesPosition(pos));
            ctx.waitTicks(2);
            ctx.takeScreenshot("ranges-position-" + pos.name().toLowerCase());
        }
        ctx.runOnClient(mc -> HeadcountSettings.setRangesPosition(HeadcountSettings.HudPosition.TOP_LEFT));
    }
}
