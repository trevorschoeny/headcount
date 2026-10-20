package com.trevorschoeny.headcount.checks;

import com.trevorschoeny.headcount.cramming.Cramming;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.CopperGolem;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * The design's key assumption (`mods/headcount/design.md`, "Scroll to pick"), checked
 * against a real {@code net.minecraft.server.dedicated.DedicatedServer}, not the
 * integrated singleplayer server {@link CrammingChecks} runs against: the client sends
 * an interact packet naming the picked entity by id, and the server (vanilla code,
 * untouched by any Headcount mixin) accepts it on reach alone, with no check that the
 * named entity is what the crosshair is on.
 *
 * <p>Fabric's {@code createServer()} boots the genuine {@code DedicatedServer} class
 * through its real {@code Main} entrypoint and the client joins it over a real loopback
 * socket connection ({@code ConnectScreen.startConnecting}), the same path a player
 * joining a real server takes. It still shares this JVM and mod classpath (there is no
 * way to boot a separate, Headcount-free server process from inside a client gametest),
 * so this does not prove a mod-free "vanilla" server accepts the packet. It does prove
 * the vanilla server code that decides does, over a real connection: Headcount carries
 * no server-side mixin on entity interaction or attack, so a plain dedicated server runs
 * the identical code path this exercises.
 */
public class DedicatedServerChecks implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        try { runChecks(ctx); } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void runChecks(ClientGameTestContext ctx) throws ReflectiveOperationException {
        Properties props = new Properties();
        props.setProperty("level-name", "headcount-dedicated-check");
        try (TestDedicatedServerContext server = ctx.worldBuilder().createServer(props)) {
            try (TestDedicatedServerConnection conn = server.connect()) {
                pickOverNetworkWaxesOnlyThePicked(ctx, server, conn);
            }
        }
    }

    /**
     * Five golems crammed together; one scroll picks one that is not the crosshair's
     * target, then honeycomb (sent as a real {@code ServerboundInteractPacket} over the
     * loopback socket) waxes that golem and no other.
     */
    private static void pickOverNetworkWaxesOnlyThePicked(ClientGameTestContext ctx,
            TestDedicatedServerContext server, TestDedicatedServerConnection conn) throws ReflectiveOperationException {
        // Spectator has no gravity, so the player holds still at y 70 while the golems
        // (NoGravity, floating clear of whatever the flat world preset put underneath)
        // sync to the client. Vanilla's /data command refuses to target a player, so
        // there is no way to NoGravity the player directly; creative mode alone does not
        // stop falling either (that needs an actual flight toggle). Switching to creative
        // only right before the click, and re-teleporting in the same breath, keeps the
        // whole fall to the one or two ticks between that switch and the click: at most a
        // few hundredths of a block, well inside the golems' hitbox.
        server.runCommand("gamerule doMobSpawning false");
        server.runCommand("gamemode spectator @p");
        server.runCommand("tp @p 0.5 70 0.5 0 " + pitchLevel());
        server.runCommand("give @p minecraft:honeycomb 16");
        double[][] offsets = {{0, 0}, {0.15, 0}, {-0.15, 0}, {0, 0.15}, {0, -0.15}};
        for (double[] o : offsets) {
            server.runCommand("summon minecraft:copper_golem " + (0.5 + o[0]) + " 70 " + (2.5 + o[1])
                    + " {NoAI:1b,NoGravity:1b,Silent:1b,Rotation:[0f,0f]}");
        }
        conn.waitForClientboundEntityUpdates(EntityTypes.COPPER_GOLEM);
        conn.waitForChunksRender();
        ctx.waitTicks(5);

        server.runCommand("gamemode creative @p");
        server.runCommand("tp @p 0.5 70 0.5 0 " + pitchLevel());
        ctx.getInput().pressKey(options -> options.keyHotbarSlots[0]);   // honeycomb, given into the first open slot
        ctx.waitTicks(2);
        int looked = ctx.computeOnClient(mc -> mc.crosshairPickEntity == null ? -1 : mc.crosshairPickEntity.getId());
        ctx.getInput().scroll(-1);
        ctx.waitTicks(2);
        int picked = ctx.computeOnClient(mc -> {
            var e = Cramming.picked();
            return e == null ? -1 : e.getId();
        });
        if (looked == -1 || picked == -1 || picked == looked)
            throw new AssertionError("one scroll should pick a golem other than the crosshair's over the network; looked "
                    + looked + ", picked " + picked);

        ctx.getInput().pressKey(options -> options.keyUse);
        ctx.waitTicks(5);
        conn.waitForServerboundPackets();
        conn.waitForClientboundPackets();
        ctx.waitTicks(5);

        boolean pickedWaxed = waxed(server, picked), lookedWaxed = waxed(server, looked);
        if (!pickedWaxed || lookedWaxed)
            throw new AssertionError("over a real dedicated-server connection, honeycomb should wax only the picked golem; "
                    + "picked waxed " + pickedWaxed + ", crosshair's waxed " + lookedWaxed);
    }

    // Looking down enough that golems floating two blocks ahead, at the player's feet
    // height, sit under the crosshair: matches the pitch CrammingChecks.aimAt uses.
    private static float pitchLevel() {
        return (float) Math.toDegrees(Math.atan2(1.62 - 0.4, 2.0));
    }

    private static boolean waxed(TestDedicatedServerContext server, int entityId) throws ReflectiveOperationException {
        return server.computeOnServer(s -> {
            if (!(s.overworld().getEntity(entityId) instanceof CopperGolem golem)) return false;
            try {
                Field f = CopperGolem.class.getDeclaredField("nextWeatheringTick");
                f.setAccessible(true);
                return (long) f.get(golem) == -2L;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
