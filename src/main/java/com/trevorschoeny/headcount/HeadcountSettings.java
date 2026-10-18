package com.trevorschoeny.headcount;

/**
 * Headcount's settings, in one place so only this class changes when ConfigKit
 * lands. Until then each setting is a plain field at its default. With ConfigKit
 * it becomes declarations on the mod's own page. The range list joins it once
 * MenuKit's list editing (Emerald) is out.
 */
public final class HeadcountSettings {

    private HeadcountSettings() {}

    // Vanilla's maxEntityCramming default. The game rule isn't sent to the client,
    // so without Headcount on the server the cramming count measures against this.
    // The setting's tooltip will say so when ConfigKit gives it a page.
    public static final int DEFAULT_CRAMMING_LIMIT = 24;

    // Looking at two or more overlapping entities shows "n / limit" near the crosshair.
    private static volatile boolean crammingCount = true;

    // Looking at crammed entities takes the mouse wheel from the hotbar to step through them.
    private static volatile boolean scrollToPick = true;

    public static boolean crammingCount() { return crammingCount; }
    public static boolean scrollToPick()  { return scrollToPick; }

    // Setters are not saved anywhere until ConfigKit, so a toggle lasts the session.
    public static void setCrammingCount(boolean v) { crammingCount = v; }
    public static void setScrollToPick(boolean v) { scrollToPick = v; }
}
