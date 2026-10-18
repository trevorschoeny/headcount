package com.trevorschoeny.headcount;

import com.trevorschoeny.headcount.inspect.InspectNet;

import net.fabricmc.api.ModInitializer;

/**
 * Headcount's common half, which runs on both sides. On a server (dedicated or the
 * single-player one) it answers clients asking about an entity: the facts the game
 * never sends to clients, such as effects and whether a golem is waxed.
 */
public final class Headcount implements ModInitializer {

    public static final String MOD_ID = "headcount";

    @Override
    public void onInitialize() {
        InspectNet.register();
    }
}
