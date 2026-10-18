package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;
import com.trevorschoeny.headcount.ranges.RangeClient;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.client.Minecraft;

/**
 * HUD: the counts of the ranges marked "Show on HUD", one line each, top left:
 * "Hostile within 32: 14". Shown only when the server has Headcount, since only the
 * server can count.
 */
public final class RangeReadout {

    private RangeReadout() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int MAX_LINES = 16;

    public static void register() {
        HudPanel.Builder panel = HudPanel.builder("headcount:ranges")
                .region(InsideRegion.TOP_LEFT)
                .style(PanelStyle.NONE)
                .visibleWhen(() -> Minecraft.getInstance().gui.screen() == null && RangeClient.serverCounts());
        for (int i = 0; i < MAX_LINES; i++) {
            int n = i;
            panel.text(0, i * 10).text(() -> line(n)).color(WHITE).done();
        }
        panel.build();
    }

    // The n-th HUD line: the n-th range with "Show on HUD", or empty past the last.
    private static String line(int n) {
        var ranges = HeadcountSettings.ranges();
        int seen = 0;
        for (int i = 0; i < ranges.size(); i++) {
            RangeEntry r = ranges.get(i);
            if (!r.showOnHud()) continue;
            if (seen++ == n) {
                int count = RangeClient.count(i);
                return r.spec().label() + ": " + (count < 0 ? "..." : count);
            }
        }
        return "";
    }
}
