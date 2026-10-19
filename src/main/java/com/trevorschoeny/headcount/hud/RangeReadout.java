package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;
import com.trevorschoeny.headcount.ranges.Category;
import com.trevorschoeny.headcount.ranges.RangeClient;
import com.trevorschoeny.headcount.ranges.RangeSpec;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * HUD: the counts of the ranges marked "Show on HUD", one line each:
 * "Hostile within 32 blocks: 14". Top left by default, or any corner or top centre (setting).
 * Shown only when the server has Headcount, since only the server can count.
 */
public final class RangeReadout {

    private RangeReadout() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int MAX_LINES = 16;

    private static int hudLines() {
        return (int) HeadcountSettings.ranges().stream().filter(RangeEntry::showOnHud).count();
    }

    public static void register() {
        // One panel per position: a HUD panel's spot is fixed at registration, so the
        // setting picks which of these shows.
        for (HeadcountSettings.HudPosition pos : HeadcountSettings.HudPosition.values()) {
            // A vertical Flow, so lines past the last range take no room (in a bottom
            // corner the text sits on the bottom edge however many lines there are).
            Flow.Builder lines = Flow.builder().vertical().gap(2);
            for (int i = 0; i < MAX_LINES; i++) {
                int n = i;
                lines.add(TextLabel.builder().text(() -> line(n)).color(WHITE).shadow(true)
                        .visibleWhen(() -> n < hudLines()).build());
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

    // Colours (Trev, 2026-10-18): the category word tinted (hostile soft red, passive
    // soft green, the rest white), "within N blocks" grey so the count is the brightest
    // thing on the line, and a whole line dimmed while its range is empty.
    private static final int HOSTILE = 0xFFFF7F7F;
    private static final int PASSIVE = 0xFF8FE08F;
    private static final int MUTED = 0xFFAAAAAA;
    private static final int EMPTY = 0xFF7A7A7A;

    // The n-th HUD line: the n-th range with "Show on HUD", or empty past the last.
    // "Hostile within 32 blocks: 6", with "(some)" after the category when types are unticked.
    private static Component line(int n) {
        var ranges = HeadcountSettings.ranges();
        int seen = 0;
        for (int i = 0; i < ranges.size(); i++) {
            RangeEntry r = ranges.get(i);
            if (!r.showOnHud()) continue;
            if (seen++ == n) return line(r.spec(), RangeClient.count(i));
        }
        return Component.empty();
    }

    private static Component line(RangeSpec spec, int count) {
        boolean empty = count == 0;
        String name = spec.category().hudName + (spec.excluded().isEmpty() ? "" : " (some)");
        return Component.empty()
                .append(Component.literal(name).withColor(empty ? EMPTY : tint(spec.category())))
                .append(Component.literal(" within " + spec.radius() + " blocks: ").withColor(empty ? EMPTY : MUTED))
                // Still waiting for the server's first count: a grey "...".
                .append(Component.literal(count < 0 ? "..." : String.valueOf(count))
                        .withColor(empty ? EMPTY : count < 0 ? MUTED : WHITE));
    }

    private static int tint(Category c) {
        return switch (c) {
            case HOSTILE -> HOSTILE;
            case PASSIVE -> PASSIVE;
            default -> WHITE;
        };
    }
}
