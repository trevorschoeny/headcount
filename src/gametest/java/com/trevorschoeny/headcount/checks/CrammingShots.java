package com.trevorschoeny.headcount.checks;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import testgallery.SceneWorld;

/**
 * Screenshots of the cramming scene's stations that are left to Trev's eyes (the gold
 * count, the outline stepping through the sheep), for when he reviews away from the
 * game. Asserts nothing. Shots land in build/run/clientGameTest/screenshots/.
 */
public class CrammingShots implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "headcount", "cramming")) {
            // No "Teleported ..." chat lines over the readout.
            scene.world().getServer().runCommand("gamerule send_command_feedback false");
            ctx.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));

            CrammingChecks.aimAt(ctx, scene, "hc_near", 0);
            ctx.takeScreenshot("shot-1-near-limit-gold");

            CrammingChecks.aimAt(ctx, scene, "hc_sheep", 2);
            ctx.takeScreenshot("shot-2-sheep-before-scroll");
            for (int i = 1; i <= 3; i++) {
                ctx.getInput().scroll(-1);
                ctx.waitTicks(3);
                ctx.takeScreenshot("shot-" + (2 + i) + "-sheep-scroll-" + i);
            }
            scene.standAt(scene.station("headcount:look_away"), 60);
            ctx.waitTicks(5);
            ctx.takeScreenshot("shot-6-looked-away");
        }
    }
}
