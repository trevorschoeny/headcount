package com.trevorschoeny.headcount.settings;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.HudPosition;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;
import com.trevlar.menukit.api.element.Button;
import com.trevlar.menukit.api.element.Divider;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.ListView;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.ScrollContainer;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.Toggle;
import com.trevlar.menukit.api.layout.CrossAlign;
import com.trevlar.menukit.api.panel.MKScreen;
import com.trevlar.menukit.api.panel.OutsideRegion;
import com.trevlar.menukit.api.panel.Panel;
import com.trevlar.menukit.api.panel.PanelPosition;
import com.trevlar.menukit.api.panel.PanelStyle;
import com.trevlar.menukit.api.panel.ScreenKind;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Headcount's settings, in the Default look (as Options, Video Settings and Controls
 * draw): the two cramming switches and where the range counts sit, then the range list,
 * which scrolls under its toolbar. Opened from Mod Menu (and so Mod Hub).
 *
 * <p>Until ConfigKit, settings last the session only (HeadcountSettings); this screen
 * becomes Headcount's ConfigKit page when ConfigKit lands.
 */
public final class HeadcountScreen extends MKScreen {

    public HeadcountScreen(Screen parent) {
        super(Component.literal("Headcount"), ScreenKind.DEFAULT, List.of(page(), done(parent)));
        setReturnAction(() -> Minecraft.getInstance().gui.setScreen(parent));
    }

    private static Panel page() {
        ListView<RangeEntry> ranges = RangeList.build();
        PanelElement body = Flow.builder().vertical().gap(4)
                .add(setting("Cramming count", toggle(HeadcountSettings::crammingCount, HeadcountSettings::setCrammingCount)))
                .add(setting("Scroll to select", toggle(HeadcountSettings::scrollToPick, HeadcountSettings::setScrollToPick)))
                .add(setting("Range counts on screen", Dropdown.ofEnum(HudPosition.class).size(100, 20)
                        .label(pos -> Component.literal(positionName(pos)))
                        .state(HeadcountSettings::rangesPosition, HeadcountSettings::setRangesPosition)
                        .build()))
                .add(Divider.horizontal().build())
                .add(ranges.toolbar())
                .add(ScrollContainer.builder().fitWidth().fillHeight().content(List.of(ranges)).build())
                .build();
        return Panel.builder("headcount:settings")
                .style(PanelStyle.NONE)
                .position(PanelPosition.main())
                .add(body)
                .build();
    }

    // Done at the bottom centre, the way every Options screen ends.
    private static Panel done(Screen parent) {
        return Panel.builder("headcount:settings-done")
                .style(PanelStyle.NONE)
                .position(PanelPosition.region(OutsideRegion.BOTTOM_CENTER))
                .add(Button.builder().label(CommonComponents.GUI_DONE).size(200, 20)
                        .onClick(() -> Minecraft.getInstance().gui.setScreen(parent)).build())
                .build();
    }

    // A setting: its name at the left, its control at the right edge.
    private static PanelElement setting(String name, PanelElement control) {
        return Flow.builder().crossAlign(CrossAlign.CENTER)
                .add(TextLabel.builder().text(Component.literal(name)).build())
                .add(Flow.spacer())
                .add(control)
                .build();
    }

    private static Toggle toggle(BooleanSupplier get, Consumer<Boolean> set) {
        return Toggle.builder().size(60, 20)
                .label(() -> get.getAsBoolean() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)
                .state(get, set)
                .build();
    }

    private static String positionName(HudPosition pos) {
        return switch (pos) {
            case TOP_LEFT -> "Top left";
            case TOP_CENTER -> "Top centre";
            case TOP_RIGHT -> "Top right";
            case BOTTOM_LEFT -> "Bottom left";
            case BOTTOM_RIGHT -> "Bottom right";
        };
    }
}
