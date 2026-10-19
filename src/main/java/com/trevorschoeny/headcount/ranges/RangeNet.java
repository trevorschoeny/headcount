package com.trevorschoeny.headcount.ranges;

import com.trevorschoeny.headcount.Headcount;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Range counts across the network. The client says which ranges it wants counted
 * ({@link Watch}); the server counts them around that player once a second and sends
 * the numbers back ({@link Counts}). A client on a server without Headcount can't send
 * the watch (the server never registered it), which is how it knows to grey the section.
 *
 * <p>Each watch carries a revision the counts echo, so counts for a list the player
 * has since edited are never shown against the new list.
 */
public final class RangeNet {

    private RangeNet() {}

    private static final int INTERVAL_TICKS = 20;
    // A client can't make the server count more than this many ranges.
    public static final int MAX_RANGES = 16;

    /** Client to server: count these ranges around me (an empty list stops counting). */
    public record Watch(int revision, List<RangeSpec> ranges) implements CustomPacketPayload {
        public static final Type<Watch> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Headcount.MOD_ID, "watch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Watch> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Watch::revision,
                RangeSpec.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_RANGES)), Watch::ranges,
                Watch::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: one count per watched range, in order. */
    public record Counts(int revision, List<Integer> counts) implements CustomPacketPayload {
        public static final Type<Counts> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Headcount.MOD_ID, "counts"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Counts> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Counts::revision,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_RANGES)), Counts::counts,
                Counts::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // Who wants what counted. Touched only on the server thread.
    private static final Map<UUID, Watch> watching = new HashMap<>();

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(Watch.TYPE, Watch.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Counts.TYPE, Counts.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Watch.TYPE,
                (watch, context) -> context.server().execute(() -> watch(context.player(), watch)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                server.execute(() -> watching.remove(handler.player.getUUID())));
        ServerTickEvents.END_SERVER_TICK.register(RangeNet::tick);
    }

    private static void watch(ServerPlayer player, Watch watch) {
        if (watch.ranges().isEmpty()) watching.remove(player.getUUID());
        else watching.put(player.getUUID(), watch);
        send(player, watch);   // answer at once, so a new range doesn't sit blank for a second
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % INTERVAL_TICKS != 0) return;
        for (Map.Entry<UUID, Watch> e : watching.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player != null) send(player, e.getValue());
        }
    }

    private static void send(ServerPlayer player, Watch watch) {
        ServerPlayNetworking.send(player, new Counts(watch.revision(), RangeCounter.count(player, watch.ranges())));
    }
}
