package com.trevorschoeny.headcount;

import com.trevorschoeny.headcount.hud.CrammingReadout;

import net.fabricmc.api.ClientModInitializer;

/**
 * Headcount's client half: the cramming count and scroll to pick, both client-only.
 * The server half (range counts, the real maxEntityCramming value) gets its own
 * main entrypoint when it is built.
 */
public final class HeadcountClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The cramming count below the crosshair. Scrolling through the crammed entities
        // needs no registration: it is the two mixins in mixin/.
        CrammingReadout.register();
    }
}
