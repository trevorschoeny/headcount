package com.trevorschoeny.headcount;

import com.trevorschoeny.headcount.hud.CrammingReadout;
import com.trevorschoeny.headcount.hud.RangeReadout;
import com.trevorschoeny.headcount.ranges.RangeClient;
import com.trevorschoeny.headcount.ranges.RangeNet;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Headcount's client half: the cramming count and scroll to pick (client-only), and the
 * range counts' readout (counted by the common half, Headcount, on the server).
 */
public final class HeadcountClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The cramming count below the crosshair. Scrolling through the crammed entities
        // needs no registration: it is the two mixins in mixin/.
        CrammingReadout.register();

        // Range counts: the server counts (Headcount's common half), the client keeps it
        // watching the current list and shows the counts.
        RangeReadout.register();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> RangeClient.tick());
        ClientPlayNetworking.registerGlobalReceiver(RangeNet.Counts.TYPE, (counts, context) -> RangeClient.received(counts));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> RangeClient.joined());
    }
}
