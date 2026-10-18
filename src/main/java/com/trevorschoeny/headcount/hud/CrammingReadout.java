package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.cramming.Cramming;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.client.Minecraft;

/**
 * HUD: with the crosshair on two or more crammed entities, a line just under the
 * crosshair counts them against maxEntityCramming ("18 / 24"), in white. Once the wheel
 * has picked one, its number goes on a second line in blue ("3"). One entity on its own
 * shows nothing.
 */
public final class CrammingReadout {

    private CrammingReadout() {}

    // The pick's number, set apart from the white count (Trev, 2026-10-18).
    private static final int PICK = 0xFF55FFFF;   // ARGB: text colours carry alpha

    private static final int NUDGE = -7;

    public static void register() {
        // Two panels, so each line centres under the crosshair on its own. MenuKit's CENTER
        // spot starts 16px below the centre and stacks its panels (count first, by
        // priority); both move up by the same NUDGE so the count sits just under the
        // crosshair (7px tall below the centre) and the number stays right below it.
        HudPanel.builder("headcount:cramming")
                .region(InsideRegion.CENTER).offset(0, NUDGE).priority(0)
                .autoSize().padding(0)
                .style(PanelStyle.NONE)
                .visibleWhen(CrammingReadout::showCount)
                .text(0, 0, CrammingReadout::countLine)
                .build();
        HudPanel.builder("headcount:pick")
                .region(InsideRegion.CENTER).offset(0, NUDGE).priority(1).gap(1)
                .autoSize().padding(0)
                .style(PanelStyle.NONE)
                .visibleWhen(() -> showCount() && Cramming.pickNumber() > 0)
                .text(0, 0).text(() -> String.valueOf(Cramming.pickNumber())).color(PICK).done()
                .build();
    }

    private static boolean showCount() {
        return Minecraft.getInstance().gui.screen() == null
                && HeadcountSettings.crammingCount() && Cramming.count() >= 2;
    }

    private static String countLine() {
        int n = Cramming.count();
        int limit = Cramming.limit(Minecraft.getInstance());
        // maxEntityCramming 0 turns cramming off: there is no limit to count against.
        return limit <= 0 ? String.valueOf(n) : n + " / " + limit;
    }
}
