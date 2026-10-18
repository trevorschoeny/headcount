package com.trevorschoeny.headcount;

import com.trevorschoeny.headcount.ranges.RangeNet;

import net.fabricmc.api.ModInitializer;

/**
 * Headcount's common half, which runs on both sides. On a server (dedicated or the
 * single-player one) it counts the entities around each player for their range counts:
 * the client can't, since it only knows the entities within tracking distance.
 */
public final class Headcount implements ModInitializer {

    public static final String MOD_ID = "headcount";

    @Override
    public void onInitialize() {
        RangeNet.register();
    }
}
