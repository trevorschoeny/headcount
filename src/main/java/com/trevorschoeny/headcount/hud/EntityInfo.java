package com.trevorschoeny.headcount.hud;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.inspect.InspectTarget;
import com.trevorschoeny.headcount.inspect.ServerFacts;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.Icon;
import com.trevlar.menukit.api.element.ItemDisplay;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.hud.HudPanel;
import com.trevlar.menukit.api.panel.PanelStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;

/**
 * HUD: what an entity is and what's on it, at the top centre of the screen (or a corner,
 * by setting) on a faint backdrop,
 * modelled on Jade's entity readout (Trev, 2026-10-18) plus Headcount's additions:
 *
 * <pre>
 *   Copper Golem                    name
 *   ♥♥♥♥♥♥                          health (hearts up to 20, "♥ 75 / 300" past that)
 *   ▣▣▣▣▣                           armor (server)
 *   [helmet][sword]                 equipment, as items (Headcount)
 *   (icon) Speed II 1:30            effects (server)
 *   Exposed, waxed                  details: villager job, golem stage and wax,
 *   Can breed in 4:00                 growth and breeding, owner, horse stats,
 *   Won't despawn                     egg timer, cure timer, despawn (mostly server)
 *   Minecraft                       the mod it comes from, blue italic
 * </pre>
 *
 * Lines that need the server (marked above) are left out when the server lacks
 * Headcount. The target is {@link InspectTarget#target()}: in a cram, only a picked one.
 */
public final class EntityInfo {

    private EntityInfo() {}

    private static final int WHITE = 0xFFFFFFFF;
    private static final int MAX_EFFECTS = 6, MAX_DETAILS = 8;
    // Up to 20 hearts (two rows) are drawn; past that, a number reads better.
    private static final float MAX_HEARTS_HEALTH = 40;

    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier ARMOR_FULL = Identifier.withDefaultNamespace("hud/armor_full");
    private static final Identifier ARMOR_HALF = Identifier.withDefaultNamespace("hud/armor_half");
    private static final Identifier ARMOR_EMPTY = Identifier.withDefaultNamespace("hud/armor_empty");

    // Equipment, in the order Jade-style readers expect: armor head to feet, then hands,
    // then what a horse or wolf wears.
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.BODY, EquipmentSlot.SADDLE};

    public static void register() {
        // One panel per position: a HUD panel's spot is fixed at registration, so the
        // setting picks which of these shows. Each gets its own column of elements.
        for (HeadcountSettings.InfoPosition pos : HeadcountSettings.InfoPosition.values()) {
            HudPanel.builder("headcount:info-" + pos.name().toLowerCase())
                    .region(pos.region)
                    // A faint backdrop, so the text reads over mobs and terrain.
                    .style(PanelStyle.DARK).padding(4)
                    .visibleWhen(() -> HeadcountSettings.infoPosition() == pos
                            && Minecraft.getInstance().gui.screen() == null
                            && HeadcountSettings.entityInfo() && target() != null)
                    .element(column())
                    .build();
        }
    }

    // The readout's lines, top to bottom.
    private static Flow column() {
        Flow.Builder column = Flow.builder().vertical().gap(2);

        column.add(label(() -> target() == null ? Component.empty() : target().getDisplayName(), () -> true));

        column.add(new SpriteRow(EntityInfo::heartCount, EntityInfo::heart,
                () -> living() != null && living().getMaxHealth() <= MAX_HEARTS_HEALTH));
        column.add(label(() -> living() == null ? Component.empty()
                        : Component.literal("♥ " + (int) Math.ceil(living().getHealth()) + " / " + (int) living().getMaxHealth()),
                () -> living() != null && living().getMaxHealth() > MAX_HEARTS_HEALTH));

        column.add(new SpriteRow(EntityInfo::armorCount, EntityInfo::armorIcon, () -> facts() != null));

        Flow.Builder items = Flow.builder().gap(1);
        for (EquipmentSlot slot : SLOTS) items.add(item(() -> equipped(slot)));
        items.add(item(() -> target() instanceof ItemFrame f ? f.getItem() : ItemStack.EMPTY));
        column.add(items.visibleWhen(EntityInfo::hasItems).build());

        for (int i = 0; i < MAX_EFFECTS; i++) {
            int n = i;
            column.add(Flow.builder().gap(3)
                    .add(Icon.builder().sprite(() -> effect(n) == null ? null : Hud.getMobEffectSprite(effect(n).getEffect())).size(9, 9).build())
                    .add(TextLabel.builder().text(() -> effectLine(effect(n))).color(WHITE).shadow(true).build())
                    .visibleWhen(() -> effect(n) != null)
                    .build());
        }

        for (int i = 0; i < MAX_DETAILS; i++) {
            int n = i;
            column.add(label(() -> details().size() > n ? details().get(n) : Component.empty(), () -> details().size() > n));
        }

        column.add(label(EntityInfo::modName, () -> true));

        return column.build();
    }

    // ── The target and what's known about it ─────────────────────────────

    private static Entity target() { return InspectTarget.target(); }

    private static LivingEntity living() { return target() instanceof LivingEntity l ? l : null; }

    private static ServerFacts facts() { return InspectTarget.factsFor(target()); }

    private static ItemStack equipped(EquipmentSlot slot) {
        LivingEntity l = living();
        return l == null ? ItemStack.EMPTY : l.getItemBySlot(slot);
    }

    private static boolean hasItems() {
        for (EquipmentSlot slot : SLOTS) if (!equipped(slot).isEmpty()) return true;
        return target() instanceof ItemFrame f && !f.getItem().isEmpty();
    }

    private static MobEffectInstance effect(int n) {
        ServerFacts f = facts();
        return f == null || f.effects().size() <= n ? null : f.effects().get(n);
    }

    // ── Hearts and armor ─────────────────────────────────────────────────

    private static int heartCount() {
        LivingEntity l = living();
        return l == null ? 0 : (int) Math.ceil(l.getMaxHealth() / 2);
    }

    // Heart i is full from 2i+2 health, half from 2i+1, empty below; the container
    // always sits under it, as vanilla draws the player's.
    private static Identifier[] heart(int i) {
        if (living() == null) return new Identifier[0];
        int health = (int) Math.ceil(living().getHealth());
        Identifier fill = health >= 2 * i + 2 ? HEART_FULL : health == 2 * i + 1 ? HEART_HALF : null;
        return new Identifier[]{HEART_CONTAINER, fill};
    }

    // A full row of ten like the player's armor bar, one more row per 20 points past it.
    private static int armorCount() {
        ServerFacts f = facts();
        if (f == null || f.armor() <= 0) return 0;
        return Math.max(10, (f.armor() + 19) / 20 * 10);
    }

    private static Identifier[] armorIcon(int i) {
        if (facts() == null) return new Identifier[0];
        int armor = facts().armor();
        return new Identifier[]{armor >= 2 * i + 2 ? ARMOR_FULL : armor == 2 * i + 1 ? ARMOR_HALF : ARMOR_EMPTY};
    }

    // ── Text ─────────────────────────────────────────────────────────────

    private static Component effectLine(MobEffectInstance e) {
        if (e == null) return Component.empty();
        Component name = e.getEffect().value().getDisplayName().copy();
        if (e.getAmplifier() > 0) name = name.copy().append(" ").append(Component.translatable("enchantment.level." + (e.getAmplifier() + 1)));
        float tickRate = Minecraft.getInstance().level == null ? 20 : Minecraft.getInstance().level.tickRateManager().tickrate();
        return name.copy().append(" ").append(MobEffectUtil.formatDuration(e, 1.0f, tickRate));
    }

    /**
     * The detail lines for the target, in order. Rebuilt per read, which is a handful of
     * cheap checks.
     * ponytail: English literals; move to a lang file when Headcount gets translations.
     */
    private static List<Component> details() {
        Entity e = target();
        ServerFacts f = facts();
        List<Component> out = new ArrayList<>();
        if (e == null) return out;

        // A villager's name is already its job, so the detail is the level (Apprentice).
        if (e instanceof VillagerDataHolder v) {
            VillagerData d = v.getVillagerData();
            if (!d.profession().is(VillagerProfession.NONE) && !d.profession().is(VillagerProfession.NITWIT)) {
                out.add(Component.translatable("merchant.level." + d.level()));
            }
        }

        if (e instanceof CopperGolem g) {
            String stage = switch (g.getWeatherState()) {
                case UNAFFECTED -> "Unweathered";
                case EXPOSED -> "Exposed";
                case WEATHERED -> "Weathered";
                case OXIDIZED -> "Oxidized";
            };
            out.add(Component.literal(f != null && f.waxed() ? stage + ", waxed" : stage));
        }

        if (f != null && f.age() != ServerFacts.NO_AGE) {
            if (f.age() < 0) out.add(Component.literal("Grows up in " + time(-f.age())));
            else if (f.age() > 0) out.add(Component.literal("Can breed in " + time(f.age())));
        } else if (living() != null && living().isBaby()) {
            out.add(Component.literal("Baby"));
        }

        if (f != null) {
            if (!f.owner().isEmpty()) out.add(Component.literal("Owner: " + f.owner()));
            // Jade's conversions: speed attribute to blocks per second, jump strength to
            // the height in blocks the horse clears.
            if (f.speed() >= 0) out.add(Component.literal(String.format("Speed: %.1f blocks/s", f.speed() * 42.16)));
            if (f.jump() >= 0) {
                double j = f.jump();
                double height = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
                out.add(Component.literal(String.format("Jump: %.1f blocks", height)));
            }
            if (f.eggTicks() >= 0) out.add(Component.literal("Next egg in " + time(f.eggTicks())));
            if (f.cureTicks() >= 0) out.add(Component.literal("Cured in " + time(f.cureTicks())));
            if (f.persistent()) out.add(Component.literal("Won't despawn"));
        }
        return out.size() > MAX_DETAILS ? out.subList(0, MAX_DETAILS) : out;
    }

    private static String time(int ticks) { return StringUtil.formatTickDuration(ticks, 20f); }

    // The mod the entity comes from, as Jade shows it: blue italic, by mod name.
    private static Component modName() {
        if (target() == null) return Component.empty();
        String ns = BuiltInRegistries.ENTITY_TYPE.getKey(target().getType()).getNamespace();
        String name = FabricLoader.getInstance().getModContainer(ns).map(c -> c.getMetadata().getName()).orElse(ns);
        return Component.literal(name).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
    }

    // ── Elements ─────────────────────────────────────────────────────────

    private static TextLabel label(Supplier<Component> text, java.util.function.BooleanSupplier shown) {
        // A fixed colour: a label's default follows the menu look (dark grey), not the HUD.
        return TextLabel.builder().text(text).color(WHITE).shadow(true).visibleWhen(shown).build();
    }

    private static ItemDisplay item(Supplier<ItemStack> stack) {
        return ItemDisplay.builder().item(stack).size(16, 16).visibleWhen(() -> !stack.get().isEmpty()).build();
    }
}
