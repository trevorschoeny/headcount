package com.trevorschoeny.headcount.checks;

import com.trevorschoeny.headcount.cramming.Cramming;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.phys.Vec3;
import testgallery.SceneWorld;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Checks for the cramming scene (test-gallery/scenes/headcount/cramming.json). */
public class CrammingChecks implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try { runChecks(ctx); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void runChecks(ClientGameTestContext ctx) throws ReflectiveOperationException {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "headcount", "cramming")) {
            countsCrammedPigs(ctx, scene);
            countsOnlyOverlappingNeighbours(ctx, scene);
            honeycombWaxesThePickedGolem(ctx, scene);
            attackHitsOnlyThePickedPig(ctx, scene);
            lookingAwayGivesTheWheelBack(ctx, scene);
        }
    }

    /**
     * Station "headcount:count": 18 pigs in one block read 18 against the default limit of
     * 24, and the wheel picks one of them instead of moving the hotbar.
     */
    private static void countsCrammedPigs(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        aimAt(ctx, scene, "hc_count", 0);
        int count = ctx.computeOnClient(mc -> Cramming.count());
        int limit = ctx.computeOnClient(Cramming::limit);
        ctx.takeScreenshot("headcount-count");
        if (count != 18 || limit != 24) throw new AssertionError("18 crammed pigs should read 18 / 24; read " + count + " / " + limit);

        int slot = selectedSlot(ctx);
        ctx.getInput().scroll(-1);
        ctx.waitTicks(2);
        int pick = ctx.computeOnClient(mc -> Cramming.pickNumber());
        if (pick == 0) throw new AssertionError("scrolling on crammed pigs should pick one");
        if (selectedSlot(ctx) != slot) throw new AssertionError("scrolling on crammed pigs moved the hotbar");
    }

    /**
     * Station "headcount:row": ten pigs 0.8 apart, each overlapping only its neighbours.
     * The fifth from the west end reads 3: itself and the two touching it.
     */
    private static void countsOnlyOverlappingNeighbours(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        aimAt(ctx, scene, "hc_row", 4);
        int count = ctx.computeOnClient(mc -> Cramming.count());
        if (count != 3) throw new AssertionError("the middle pig of the row should read 3; read " + count);
    }

    /**
     * Station "headcount:wax": with five golems crammed together, one scroll picks a golem
     * other than the crosshair's, and honeycomb waxes that one and no other. This is the
     * end-to-end proof that the server accepts a click on the picked entity.
     */
    private static void honeycombWaxesThePickedGolem(ClientGameTestContext ctx, SceneWorld.Opened scene) throws ReflectiveOperationException {
        ctx.getInput().pressKey(options -> options.keyHotbarSlots[1]);   // the scene's honeycomb
        aimAt(ctx, scene, "hc_golem", 0);
        int looked = ctx.computeOnClient(mc -> mc.crosshairPickEntity == null ? -1 : mc.crosshairPickEntity.getId());
        ctx.getInput().scroll(-1);
        ctx.waitTicks(2);
        int picked = ctx.computeOnClient(mc -> {
            Entity e = Cramming.picked();
            return e == null ? -1 : e.getId();
        });
        if (looked == -1 || picked == -1 || picked == looked)
            throw new AssertionError("one scroll should pick a golem other than the crosshair's; looked " + looked + ", picked " + picked);

        ctx.getInput().pressKey(options -> options.keyUse);
        ctx.waitTicks(5);
        ctx.takeScreenshot("headcount-wax");
        boolean pickedWaxed = waxed(scene, picked), lookedWaxed = waxed(scene, looked);
        if (!pickedWaxed || lookedWaxed)
            throw new AssertionError("honeycomb should wax the picked golem only; picked waxed " + pickedWaxed + ", crosshair's waxed " + lookedWaxed);
    }

    /**
     * Station "headcount:attack": with six pigs crammed together, one scroll and one punch
     * hurt the picked pig and no other. Then, after looking away and back, the next scroll
     * carries on from that pig's number rather than starting over.
     */
    private static void attackHitsOnlyThePickedPig(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        ctx.getInput().pressKey(options -> options.keyHotbarSlots[0]);   // empty hand: a punch, no sweep
        aimAt(ctx, scene, "hc_attack", 0);
        ctx.getInput().scroll(-1);
        ctx.waitTicks(2);
        int picked = ctx.computeOnClient(mc -> Cramming.picked() == null ? -1 : Cramming.picked().getId());
        int number = ctx.computeOnClient(mc -> Cramming.pickNumber());
        ctx.waitTicks(20);   // full attack strength
        ctx.getInput().pressKey(options -> options.keyAttack);
        ctx.waitTicks(5);

        List<String> hurt = scene.world().getServer().computeOnServer(server -> {
            List<String> out = new ArrayList<>();
            for (Entity e : server.overworld().getAllEntities())
                if (e.entityTags().contains("hc_attack") && e instanceof LivingEntity l && l.getHealth() < l.getMaxHealth())
                    out.add(String.valueOf(e.getId()));
            return out;
        });
        if (!hurt.equals(List.of(String.valueOf(picked))))
            throw new AssertionError("a punch should hurt only the picked pig (" + picked + "); hurt: " + hurt);

        scene.standAt(scene.station("headcount:look_away"), 60);
        ctx.waitTicks(5);
        aimAt(ctx, scene, "hc_attack", 0);
        ctx.getInput().scroll(-1);
        ctx.waitTicks(2);
        int next = ctx.computeOnClient(mc -> Cramming.pickNumber());
        if (next != number % 6 + 1)
            throw new AssertionError("after looking away, the next scroll should go from " + number + " to " + (number % 6 + 1) + "; went to " + next);
    }

    /** Station "headcount:look_away": with no entity under the crosshair the wheel moves the hotbar. */
    private static void lookingAwayGivesTheWheelBack(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        scene.standAt(scene.station("headcount:look_away"), 60);   // crosshair on the floor
        ctx.waitTicks(5);
        int slot = selectedSlot(ctx);
        ctx.getInput().scroll(-1);
        ctx.waitTicks(2);
        if (selectedSlot(ctx) == slot) throw new AssertionError("scrolling at the floor should move the hotbar");
    }

    /**
     * Stands two blocks north of one of a station's mobs (the scene tags them) and looks at
     * it, well within reach. `index` counts the tagged mobs from the west end. Aimed at
     * where the mob really is, not where the scene summoned it, since crammed mobs shift.
     */
    static void aimAt(ClientGameTestContext ctx, SceneWorld.Opened scene, String tag, int index) {
        Vec3 at = scene.world().getServer().computeOnServer(server -> {
            List<Vec3> spots = new ArrayList<>();
            for (Entity e : server.overworld().getAllEntities())
                if (e.entityTags().contains(tag)) spots.add(e.position());
            spots.sort(Comparator.comparingDouble(Vec3::x));
            if (spots.size() <= index) throw new AssertionError("scene has " + spots.size() + " mobs tagged " + tag);
            return spots.get(index);
        });
        // Facing south (yaw 0), pitched down from eye height (1.62) to 0.4 up the mob.
        float pitch = (float) Math.toDegrees(Math.atan2(1.62 - 0.4, 2.0));
        scene.world().getServer().runCommand("tp @p " + at.x + " " + at.y + " " + (at.z - 2.0) + " 0 " + pitch);
        ctx.waitTicks(5);
    }

    private static int selectedSlot(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
    }

    // A waxed copper golem stops weathering: its private nextWeatheringTick is set to -2
    // (IGNORE_WEATHERING_TICK). Read on the server, where the honeycomb took effect.
    private static boolean waxed(SceneWorld.Opened scene, int entityId) throws ReflectiveOperationException {
        return scene.world().getServer().computeOnServer(server -> {
            if (!(server.overworld().getEntity(entityId) instanceof CopperGolem golem)) return false;
            Field f = CopperGolem.class.getDeclaredField("nextWeatheringTick");
            f.setAccessible(true);
            return f.getLong(golem) == -2L;
        });
    }
}
