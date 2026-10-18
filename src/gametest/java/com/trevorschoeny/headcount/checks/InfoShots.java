package com.trevorschoeny.headcount.checks;

import com.trevorschoeny.headcount.HeadcountSettings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import testgallery.SceneWorld;

/** Screenshots of the entity info readout, one per mob in the entity-info scene. Asserts nothing. */
public class InfoShots implements FabricClientGameTest {
    private static final String[] TAGS = {"hi_zombie", "hi_golem", "hi_horse", "hi_calf", "hi_villager", "hi_chicken"};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "headcount", "entity-info")) {
            scene.world().getServer().runCommand("gamerule send_command_feedback false");
            ctx.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(false));
            for (String tag : TAGS) {
                CrammingChecks.aimAt(ctx, scene, tag, 0);
                ctx.waitTicks(10);   // the server's answer
                ctx.takeScreenshot("info-" + tag.substring(3));
            }
            // The zombie's readout in each position the setting offers.
            CrammingChecks.aimAt(ctx, scene, "hi_zombie", 0);
            for (HeadcountSettings.InfoPosition pos : HeadcountSettings.InfoPosition.values()) {
                ctx.runOnClient(mc -> HeadcountSettings.setInfoPosition(pos));
                ctx.waitTicks(2);
                ctx.takeScreenshot("info-position-" + pos.name().toLowerCase());
            }
            ctx.runOnClient(mc -> HeadcountSettings.setInfoPosition(HeadcountSettings.InfoPosition.TOP_CENTER));

            CrammingChecks.aimAt(ctx, scene, "hi_cram", 0);
            ctx.waitTicks(10);
            ctx.takeScreenshot("info-cram-before-scroll");
            ctx.getInput().scroll(-1);
            ctx.waitTicks(10);
            ctx.takeScreenshot("info-cram-picked");
        }
    }
}
