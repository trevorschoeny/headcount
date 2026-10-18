package com.trevorschoeny.headcount;

import net.fabricmc.api.ClientModInitializer;

/**
 * Headcount's client half: the crowd count and scroll to pick, both client-only.
 * The server half (range counts, the real maxEntityCramming value) gets its own
 * main entrypoint when it is built.
 */
public final class HeadcountClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Features register here as they land.
    }
}
