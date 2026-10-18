package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;
import com.trevorschoeny.headcount.ranges.RangeClient;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * HUD: the counts of the ranges marked "Show on HUD", one line each:
 * "Hostile within 32: 14". Top left by default, or any corner or top centre (setting).
 * Shown only when the server has Headcount, since only the server can count.
 */
public final class RangeReadout {

    private RangeReadout() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int MAX_LINES = 16;

    public static void register() {
        // One panel per position: a HUD panel's spot is fixed at registration, so the
        // setting picks which of these shows.
        for (HeadcountSettings.HudPosition pos : HeadcountSettings.HudPosition.values()) {
            // A vertical Flow, so lines past the last range take no room (in a bottom
            // corner the text sits on the bottom edge however many lines there are).
            Flow.Builder lines = Flow.builder().vertical().gap(2);
            for (int i = 0; i < MAX_LINES; i++) {
                int n = i;
                lines.add(TextLabel.builder().text(() -> Component.literal(line(n))).color(WHITE).shadow(true)
                        .visibleWhen(() -> !line(n).isEmpty()).build());
            }
            HudPanel.builder("headcount:ranges-" + pos.name().toLowerCase())
                    .region(pos.region)
                    .style(PanelStyle.NONE)
                    .visibleWhen(() -> HeadcountSettings.rangesPosition() == pos
                            && Minecraft.getInstance().gui.screen() == null && RangeClient.serverCounts())
                    .element(lines.build())
                    .build();
        }
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
