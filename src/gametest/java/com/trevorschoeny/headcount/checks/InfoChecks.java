package com.trevorschoeny.headcount.checks;

import com.trevorschoeny.headcount.inspect.InspectTarget;
import com.trevorschoeny.headcount.inspect.ServerFacts;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import testgallery.SceneWorld;

/** Checks for the entity info scene (test-gallery/scenes/headcount/entity-info.json). */
public class InfoChecks implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "headcount", "entity-info")) {
            waxedGolemReadsWaxed(ctx, scene);
        }
    }

    /**
     * Station "headcount:info_waxed": the client can't see wax, so a waxed golem reading
     * waxed proves the ask and answer round trip with the server half.
     */
    private static void waxedGolemReadsWaxed(ClientGameTestContext ctx, SceneWorld.Opened scene) {
        CrammingChecks.aimAt(ctx, scene, "hi_golem", 0);
        ctx.waitTicks(10);
        ServerFacts facts = ctx.computeOnClient(mc -> InspectTarget.factsFor(InspectTarget.target()));
        ctx.takeScreenshot("headcount-info-waxed");
        if (facts == null) throw new AssertionError("no answer from the server about the golem");
        if (!facts.waxed()) throw new AssertionError("the waxed golem should read waxed");
    }
}
