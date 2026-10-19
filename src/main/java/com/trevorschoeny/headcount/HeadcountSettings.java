package com.trevorschoeny.headcount;

import com.trevlar.menukit.api.panel.InsideRegion;
import com.trevorschoeny.headcount.ranges.Category;
import com.trevorschoeny.headcount.ranges.RangeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

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
    /**
     * One range in the list: what it counts, and whether its count shows on the HUD.
     * {@code id} is stable for the entry's life, so the settings list keeps its row
     * (and a number field being typed in) while the entry's values change.
     */
    public record RangeEntry(int id, RangeSpec spec, boolean showOnHud) {
        public RangeEntry withSpec(RangeSpec s)  { return new RangeEntry(id, s, showOnHud); }
        public RangeEntry withShowOnHud(boolean v) { return new RangeEntry(id, spec, v); }
    }

    private static final AtomicInteger nextId = new AtomicInteger();

    /** A new entry with a fresh id. */
    public static RangeEntry entry(RangeSpec spec, boolean showOnHud) {
        return new RangeEntry(nextId.incrementAndGet(), spec, showOnHud);
    }

    // The range list. Three defaults tied to mob spawning: all within 16; hostile
    // within 32 (past 32, hostile mobs can despawn at random); hostile within 128 (the
    // spawn and instant-despawn edge). Replaced whole on every edit, never mutated, so
    // RangeClient can tell an edit happened by identity.
    private static volatile List<RangeEntry> ranges = List.of(
            entry(RangeSpec.of(16, Category.ALL), true),
            entry(RangeSpec.of(32, Category.HOSTILE), true),
            entry(RangeSpec.of(128, Category.HOSTILE), true));

    /** Where the range counts sit: any corner, or top centre. */
    public enum HudPosition {
        TOP_LEFT(InsideRegion.TOP_LEFT), TOP_CENTER(InsideRegion.TOP_CENTER), TOP_RIGHT(InsideRegion.TOP_RIGHT),
        BOTTOM_LEFT(InsideRegion.BOTTOM_LEFT), BOTTOM_RIGHT(InsideRegion.BOTTOM_RIGHT);

        public final InsideRegion region;
        HudPosition(InsideRegion region) { this.region = region; }
    }

    private static volatile HudPosition rangesPosition = HudPosition.TOP_LEFT;

    public static boolean crammingCount() { return crammingCount; }
    public static HudPosition rangesPosition() { return rangesPosition; }
    public static List<RangeEntry> ranges() { return ranges; }
    public static boolean scrollToPick()  { return scrollToPick; }

    // Setters are not saved anywhere until ConfigKit, so a toggle lasts the session.
    public static void setCrammingCount(boolean v) { crammingCount = v; }
    public static void setScrollToPick(boolean v) { scrollToPick = v; }
    public static void setRanges(List<RangeEntry> v) { ranges = List.copyOf(v); }

    // ── Editing the range list by id (the settings screen's list) ──────────
    // Each builds a new list and swaps it in whole.

    /** The entry with this id, or null once it has been removed. */
    public static RangeEntry range(int id) {
        for (RangeEntry r : ranges) if (r.id() == id) return r;
        return null;
    }

    /** Adds "All entities within 16 blocks", shown on the HUD, at the end; returns its id. */
    public static int addRange() {
        RangeEntry added = entry(RangeSpec.of(16, Category.ALL), true);
        List<RangeEntry> next = new ArrayList<>(ranges);
        next.add(added);
        ranges = List.copyOf(next);
        return added.id();
    }

    public static void removeRange(int id) {
        ranges = ranges.stream().filter(r -> r.id() != id).toList();
    }

    /** Moves an entry to {@code toIndex} (its place in the list, as the list shows it). */
    public static void moveRange(int id, int toIndex) {
        RangeEntry moving = range(id);
        if (moving == null) return;
        List<RangeEntry> next = new ArrayList<>(ranges);
        next.remove(moving);
        next.add(Math.clamp(toIndex, 0, next.size()), moving);
        ranges = List.copyOf(next);
    }

    public static void updateRange(int id, UnaryOperator<RangeEntry> change) {
        ranges = ranges.stream().map(r -> r.id() == id ? change.apply(r) : r).toList();
    }
    public static void setRangesPosition(HudPosition v) { rangesPosition = v; }
}
