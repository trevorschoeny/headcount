package com.trevorschoeny.headcount.inspect;

import com.trevorschoeny.headcount.Headcount;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * The info readout's network half. The client asks about the entity it is showing
 * ({@link Ask}); a server with Headcount answers with what only it knows
 * ({@link Facts}). A client on a server without Headcount can't send the ask at all
 * (Fabric knows the server never registered it), so it shows the client-side lines only.
 */
public final class InspectNet {

    private InspectNet() {}

    // Only entities near the asker are answered, so the readout can't be used to
    // probe mobs across the map. Generous next to reach, since crammed entities
    // and horses you ride up to are a few blocks out.
    private static final double MAX_DISTANCE = 32;

    /** Client to server: tell me about entity {@code entityId}. */
    public record Ask(int entityId) implements CustomPacketPayload {
        public static final Type<Ask> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Headcount.MOD_ID, "ask"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Ask> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, Ask::entityId, Ask::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: the facts about the entity asked about. */
    public record Facts(ServerFacts facts) implements CustomPacketPayload {
        public static final Type<Facts> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Headcount.MOD_ID, "facts"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Facts> STREAM_CODEC =
                StreamCodec.composite(ServerFacts.STREAM_CODEC, Facts::facts, Facts::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Ask.TYPE, Ask.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Facts.TYPE, Facts.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Ask.TYPE,
                (ask, context) -> context.server().execute(() -> answer(context.player(), ask.entityId())));
    }

    private static void answer(ServerPlayer player, int entityId) {
        ServerLevel level = player.level();
        Entity e = level.getEntity(entityId);
        if (e == null || e.distanceTo(player) > MAX_DISTANCE) return;
        ServerPlayNetworking.send(player, new Facts(ServerFacts.of(e, level)));
    }
}
