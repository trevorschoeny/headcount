package com.trevorschoeny.headcount.checks;

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
            CrammingChecks.aimAt(ctx, scene, "hi_cram", 0);
            ctx.waitTicks(10);
            ctx.takeScreenshot("info-cram-before-scroll");
            ctx.getInput().scroll(-1);
            ctx.waitTicks(10);
            ctx.takeScreenshot("info-cram-picked");
        }
    }
}
