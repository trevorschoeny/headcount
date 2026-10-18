package com.trevorschoeny.headcount.ranges;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;

import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * The client's side of range counts: keeps the server watching the player's current
 * range list, and holds the latest counts for it.
 */
public final class RangeClient {

    private RangeClient() {}

    // The list the server was last told about (null: tell it again), and its revision.
    private static List<RangeEntry> sent;
    private static int revision;
    private static List<Integer> counts = List.of();
    private static int countsRevision = -1;

    /** True when the server has Headcount and so can count; otherwise the section greys. */
    public static boolean serverCounts() {
        return ClientPlayNetworking.canSend(RangeNet.Watch.TYPE);
    }

    /** Each client tick: when the list has changed since it was sent, send it again. */
    public static void tick() {
        if (!serverCounts()) { sent = null; return; }
        List<RangeEntry> now = HeadcountSettings.ranges();
        if (now == sent) return;   // settings replace the list on every edit
        sent = now;
        revision++;
        ClientPlayNetworking.send(new RangeNet.Watch(revision, now.stream().map(RangeEntry::spec).toList()));
    }

    /** Joining a world or server: whatever was sent before went to another server. */
    public static void joined() { sent = null; counts = List.of(); countsRevision = -1; }

    public static void received(RangeNet.Counts answer) {
        if (answer.revision() != revision) return;   // for a list since edited
        counts = answer.counts();
        countsRevision = answer.revision();
    }

    /** The count for range {@code i} of the current list, or -1 while it hasn't arrived. */
    public static int count(int i) {
        return countsRevision == revision && i < counts.size() ? counts.get(i) : -1;
    }
}
