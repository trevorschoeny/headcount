package com.trevorschoeny.headcount;

import com.trevlar.menukit.api.panel.InsideRegion;

/**
 * Headcount's settings, in one place so only this class changes when ConfigKit
 * lands. Until then each setting is a plain field at its default. With ConfigKit
 * it becomes declarations on the mod's own page. The range list joins it once
 * MenuKit's list editing (Emerald) is out.
 */
public final class HeadcountSettings {

    private HeadcountSettings() {}

    // Looking at two or more overlapping entities shows how many just under the crosshair.
    private static volatile boolean crammingCount = true;

    // Looking at crammed entities takes the mouse wheel from the hotbar to step through them.
    private static volatile boolean scrollToPick = true;

    // What the entity under the crosshair (or picked in a cram) is and has, at the top centre.
    private static volatile boolean entityInfo = true;

    /** Where the entity info sits: top centre (like Jade) or any corner. */
    public enum InfoPosition {
        TOP_CENTER(InsideRegion.TOP_CENTER), TOP_LEFT(InsideRegion.TOP_LEFT), TOP_RIGHT(InsideRegion.TOP_RIGHT),
        BOTTOM_LEFT(InsideRegion.BOTTOM_LEFT), BOTTOM_RIGHT(InsideRegion.BOTTOM_RIGHT);

        public final InsideRegion region;
        InfoPosition(InsideRegion region) { this.region = region; }
    }

    private static volatile InfoPosition infoPosition = InfoPosition.TOP_CENTER;

    public static boolean crammingCount() { return crammingCount; }
    public static InfoPosition infoPosition() { return infoPosition; }
    public static boolean entityInfo()    { return entityInfo; }
    public static boolean scrollToPick()  { return scrollToPick; }

    // Setters are not saved anywhere until ConfigKit, so a toggle lasts the session.
    public static void setCrammingCount(boolean v) { crammingCount = v; }
    public static void setScrollToPick(boolean v) { scrollToPick = v; }
    public static void setEntityInfo(boolean v)   { entityInfo = v; }
    public static void setInfoPosition(InfoPosition v) { infoPosition = v; }
}
