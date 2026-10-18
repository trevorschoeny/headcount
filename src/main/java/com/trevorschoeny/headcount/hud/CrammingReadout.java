package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.cramming.Cramming;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevlar.menukit.api.panel.PanelStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * HUD: with the crosshair on two or more crammed entities, a line below the crosshair
 * counts them against maxEntityCramming ("18 / 24"). Once the wheel has picked one, a
 * second line says which ("3 / 18"). One entity on its own shows nothing.
 */
public final class CrammingReadout {

    private CrammingReadout() {}

    // Colours: gold from three quarters of the limit, red past it (vanilla is hurting them).
    private static final int NORMAL = 0xFFFFFF;
    private static final int NEAR   = 0xFFAA00;
    private static final int OVER   = 0xFF5555;

    public static void register() {
        HudPanel.builder("headcount:cramming")
                .region(InsideRegion.CENTER).offset(0, 20)   // just below the crosshair
                .autoSize().padding(4)
                .style(PanelStyle.NONE)
                .visibleWhen(() -> Minecraft.getInstance().gui.screen() == null
                        && HeadcountSettings.crammingCount() && Cramming.count() >= 2)
                .text(0, 0).component(CrammingReadout::countLine).done()
                .text(0, 12, () -> Cramming.pickNumber() == 0 ? ""
                        : Cramming.pickNumber() + " / " + Cramming.count())
                .build();
    }

    private static Component countLine() {
        int n = Cramming.count();
        int limit = Cramming.limit(Minecraft.getInstance());
        // maxEntityCramming 0 turns cramming off: there is no limit to count against.
        if (limit <= 0) return Component.literal(String.valueOf(n));
        // Vanilla hurts an entity once more than `limit` are crammed together.
        int colour = n > limit ? OVER : n * 4 >= limit * 3 ? NEAR : NORMAL;
        return Component.literal(n + " / " + limit).withColor(colour);
    }
}
