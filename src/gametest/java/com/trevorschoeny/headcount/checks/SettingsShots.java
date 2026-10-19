package com.trevorschoeny.headcount.checks;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.ranges.Category;
import com.trevorschoeny.headcount.ranges.RangeSpec;
import com.trevorschoeny.headcount.settings.HeadcountScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import testgallery.SceneWorld;

import java.util.List;

/**
 * Screenshots of Headcount's settings screen, opened in the ranges scene so the server
 * counts. Asserts nothing: the list's behaviour is MenuKit's, checked in MenuKit's own
 * tests, and its feel is Trev's walk.
 */
public class SettingsShots implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try (SceneWorld.Opened scene = SceneWorld.open(ctx, "headcount", "ranges")) {
            // MenuKit's shot size: 2560 x 1440 at GUI scale 5, the size its screens must fit.
            ctx.getInput().resizeWindow(2560, 1440);
            ctx.runOnClient(mc -> {
                mc.options.guiScale().set(5);
                mc.resizeGui();
            });
            ctx.getInput().setCursorPos(2, 2);   // nothing hovered
            ctx.runOnClient(mc -> mc.gui.setScreen(new HeadcountScreen(null)));
            ctx.waitTicks(10);
            ctx.takeScreenshot("settings-defaults");

            // A fourth range, as the row's controls would leave it: passive within 64,
            // cows unticked, off the HUD.
            ctx.runOnClient(mc -> {
                int id = HeadcountSettings.addRange();
                HeadcountSettings.updateRange(id, r -> r.withSpec(new RangeSpec(64, Category.PASSIVE,
                        List.of(Identifier.withDefaultNamespace("cow")))).withShowOnHud(false));
            });
            ctx.waitTicks(10);
            ctx.takeScreenshot("settings-four-ranges");
            ctx.runOnClient(mc -> mc.gui.setScreen(null));
        }
    }
}
