package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.cramming.Cramming;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.client.Minecraft;

/**
 * HUD: with the crosshair on two or more crammed entities, one white line just under the
 * crosshair says how many ("18"). While the wheel has picked one, the same line says which
 * ("3 / 18"). One entity on its own shows nothing. No "/ 24": anyone cramming knows the
 * limit (Trev, 2026-10-18).
 */
public final class CrammingReadout {

    private CrammingReadout() {}

    // MenuKit's CENTER spot starts 16px below the centre; this lifts the line to just
    // under the crosshair (which reaches 7px below the centre).
    private static final int NUDGE = -7;

    public static void register() {
        HudPanel.builder("headcount:cramming")
                .region(InsideRegion.CENTER).offset(0, NUDGE)
                .autoSize().padding(0)
                .style(PanelStyle.NONE)
                .visibleWhen(() -> Minecraft.getInstance().gui.screen() == null
                        && HeadcountSettings.crammingCount() && Cramming.count() >= 2)
                .text(0, 0, CrammingReadout::line)
                .build();
    }

    private static String line() {
        int n = Cramming.count(), pick = Cramming.pickNumber();
        return pick == 0 ? String.valueOf(n) : pick + " / " + n;
    }
}
