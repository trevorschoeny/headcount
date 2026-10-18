package com.trevorschoeny.headcount;

import com.trevorschoeny.headcount.ranges.Category;
import com.trevorschoeny.headcount.ranges.RangeSpec;

import java.util.List;

/**
 * Headcount's settings, in one place so only this class changes when ConfigKit
 * lands. Until then each setting is a plain field at its default. With ConfigKit
 * it becomes declarations on the mod's own page. The range list's editor waits for
 * MenuKit's list editing (Emerald); until then the three defaults are the list.
 */
public final class HeadcountSettings {

    private HeadcountSettings() {}

    // Looking at two or more overlapping entities shows how many just under the crosshair.
    private static volatile boolean crammingCount = true;

    // Looking at crammed entities takes the mouse wheel from the hotbar to step through them.
    private static volatile boolean scrollToPick = true;

    /** One range in the list: what it counts, and whether its count shows on the HUD. */
    public record RangeEntry(RangeSpec spec, boolean showOnHud) {}

    // The range list. Three defaults tied to mob spawning: all within 16; hostile
    // within 32 (past 32, hostile mobs can despawn at random); hostile within 128 (the
    // spawn and instant-despawn edge). Replaced whole on every edit, never mutated, so
    // RangeClient can tell an edit happened by identity.
    private static volatile List<RangeEntry> ranges = List.of(
            new RangeEntry(RangeSpec.of(16, Category.ALL), true),
            new RangeEntry(RangeSpec.of(32, Category.HOSTILE), true),
            new RangeEntry(RangeSpec.of(128, Category.HOSTILE), true));

    public static boolean crammingCount() { return crammingCount; }
    public static List<RangeEntry> ranges() { return ranges; }
    public static boolean scrollToPick()  { return scrollToPick; }

    // Setters are not saved anywhere until ConfigKit, so a toggle lasts the session.
    public static void setCrammingCount(boolean v) { crammingCount = v; }
    public static void setScrollToPick(boolean v) { scrollToPick = v; }
    public static void setRanges(List<RangeEntry> v) { ranges = List.copyOf(v); }
}
