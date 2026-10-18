package com.trevorschoeny.headcount;

import com.trevorschoeny.headcount.hud.CrammingReadout;
import com.trevorschoeny.headcount.hud.EntityInfo;
import com.trevorschoeny.headcount.inspect.InspectNet;
import com.trevorschoeny.headcount.inspect.InspectTarget;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Headcount's client half: the cramming count, scroll to pick, and the entity info
 * readout. The common half (Headcount) answers the info readout's questions on servers
 * that have the mod.
 */
public final class HeadcountClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The cramming count below the crosshair. Scrolling through the crammed entities
        // needs no registration: it is the two mixins in mixin/.
        CrammingReadout.register();

        // Entity info at the top centre. The server's half of it (effects, wax, timers)
        // arrives as answers to the client's asks, when the server has Headcount.
        EntityInfo.register();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> InspectTarget.tick());
        ClientPlayNetworking.registerGlobalReceiver(InspectNet.Facts.TYPE,
                (answer, context) -> InspectTarget.received(answer.facts()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> InspectTarget.reset());
    }
}
