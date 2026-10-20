package com.trevorschoeny.headcount.settings;

import com.trevorschoeny.headcount.HeadcountSettings;
import com.trevorschoeny.headcount.HeadcountSettings.RangeEntry;
import com.trevorschoeny.headcount.ranges.Category;
import com.trevorschoeny.headcount.ranges.RangeClient;
import com.trevorschoeny.headcount.ranges.RangeNet;
import com.trevorschoeny.headcount.ranges.RangeSpec;
import com.trevlar.menukit.api.element.Dropdown;
import com.trevlar.menukit.api.element.DropdownMulti;
import com.trevlar.menukit.api.element.Flow;
import com.trevlar.menukit.api.element.ListView;
import com.trevlar.menukit.api.element.PanelElement;
import com.trevlar.menukit.api.element.TextField;
import com.trevlar.menukit.api.element.TextLabel;
import com.trevlar.menukit.api.element.TextRole;
import com.trevlar.menukit.api.element.Toggle;
import com.trevlar.menukit.api.layout.CrossAlign;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * The range list on Headcount's settings screen, on MenuKit's ListView with list
 * editing: "+ Add range" in the toolbar, and up, down and a remove cross in each row.
 * Each row is one range, all in one line:
 *
 * <pre>
 *   Within [ 32 ] blocks  [Hostile v] [All types v]      HUD [ON]  | ^ v x
 * </pre>
 *
 * Rows are keyed by the entry's id and read its values by id, so a row and a number
 * field being typed in survive every edit (the list itself is replaced on each one).
 * The whole list greys, with the reason above it, on a server without Headcount.
 */
final class RangeList {

    private RangeList() {}

    // What's typed in each row's number field, by entry id, so a half-typed or empty
    // field isn't overwritten by the saved radius between keystrokes.
    private static final Map<Integer, String> radiusDrafts = new HashMap<>();

    // Each category's entity types, A to Z, for the second dropdown. Types are fixed
    // once the game has loaded, so worked out once per category.
    private static final Map<Category, List<EntityType<?>>> typesByCategory = new EnumMap<>(Category.class);

    static ListView<RangeEntry> build() {
        return ListView.<RangeEntry>builder()
                .items(HeadcountSettings::ranges)
                .key(RangeEntry::id)
                .row(entry -> row(entry.id()))
                .title(Component.literal("Range counts"))
                .add(Component.literal("+ Add range"), HeadcountSettings::addRange)
                .addDisabledWhen(() -> HeadcountSettings.ranges().size() >= RangeNet.MAX_RANGES)
                .remove(HeadcountSettings::removeRange)
                .move(HeadcountSettings::moveRange)
                .controlsInRows()
                .empty(TextLabel.builder().role(TextRole.MUTED).text(Component.literal("No ranges. Add one to count the entities around you.")).build())
                // Only a server with Headcount can count. Out of a world the list stays
                // editable: the next server with Headcount gets it.
                .disabledWhen(() -> Minecraft.getInstance().getConnection() != null && !RangeClient.serverCounts())
                .disabledReason(Component.literal("This server doesn't have Headcount, so it can't count ranges."))
                .build();
    }

    private static PanelElement row(int id) {
        return Flow.builder().boxed().gap(4).crossAlign(CrossAlign.CENTER)
                .add(TextLabel.builder().text(Component.literal("Within")).build())
                .add(TextField.builder().size(32, 20).maxLength(3)
                        .state(() -> radiusDrafts.getOrDefault(id, String.valueOf(spec(id).radius())),
                                text -> setRadius(id, text))
                        .build())
                .add(TextLabel.builder().text(Component.literal("blocks")).build())
                .add(Dropdown.<Category>builder().size(70, 20)
                        .items(List.of(Category.values()))
                        .label(c -> Component.literal(c.label))
                        .state(() -> spec(id).category(), c -> setCategory(id, c))
                        .build())
                .add(DropdownMulti.<EntityType<?>>builder().size(84, 20)
                        .items(() -> types(spec(id).category()))
                        .label(EntityType::getDescription)
                        .searchable()
                        .selectAllRow(Component.literal("Select all"))
                        .clearAllRow(Component.literal("Clear all"))
                        .state(() -> ticked(id), set -> setTicked(id, set))
                        .triggerLabel(set -> Component.literal(set.size() == types(spec(id).category()).size()
                                ? "All types" : set.size() + " of " + types(spec(id).category()).size()))
                        .build())
                .add(Flow.spacer())
                .add(TextLabel.builder().text(Component.literal("HUD")).build())
                .add(Toggle.builder().size(40, 20)
                        .label(() -> showOnHud(id) ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF)
                        .state(() -> showOnHud(id), on -> HeadcountSettings.updateRange(id, r -> r.withShowOnHud(on)))
                        .build())
                .build();
    }

    // ── Reading an entry by id (a removed entry's row may draw one more frame) ──

    private static RangeSpec spec(int id) {
        RangeEntry r = HeadcountSettings.range(id);
        return r == null ? RangeSpec.of(16, Category.ALL) : r.spec();
    }

    private static boolean showOnHud(int id) {
        RangeEntry r = HeadcountSettings.range(id);
        return r != null && r.showOnHud();
    }

    // ── Edits ──────────────────────────────────────────────────────────────

    // Keeps whatever is typed; saves it once it is a whole number from 1 to 256.
    private static void setRadius(int id, String text) {
        radiusDrafts.put(id, text);
        try {
            int radius = Integer.parseInt(text.trim());
            if (radius < 1 || radius > RangeSpec.MAX_RADIUS) return;
            HeadcountSettings.updateRange(id, r -> r.withSpec(new RangeSpec(radius, r.spec().category(), r.spec().excluded())));
        } catch (NumberFormatException notYet) {
            // Empty or not a number yet: nothing to save.
        }
    }

    // A new category starts with all its types ticked.
    private static void setCategory(int id, Category category) {
        HeadcountSettings.updateRange(id, r -> r.withSpec(new RangeSpec(r.spec().radius(), category, List.of())));
    }

    // The second dropdown speaks ticked types; the spec keeps the unticked ones.
    private static Set<EntityType<?>> ticked(int id) {
        RangeSpec s = spec(id);
        Set<EntityType<?>> out = new HashSet<>();
        for (EntityType<?> t : types(s.category())) if (!s.excluded().contains(key(t))) out.add(t);
        return out;
    }

    private static void setTicked(int id, Set<EntityType<?>> ticked) {
        HeadcountSettings.updateRange(id, r -> {
            List<Identifier> excluded = types(r.spec().category()).stream()
                    .filter(t -> !ticked.contains(t)).map(RangeList::key).toList();
            return r.withSpec(new RangeSpec(r.spec().radius(), r.spec().category(), excluded));
        });
    }

    private static List<EntityType<?>> types(Category category) {
        return typesByCategory.computeIfAbsent(category, c -> BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(c::includes)
                .sorted(Comparator.comparing(t -> t.getDescription().getString()))
                .<EntityType<?>>map(t -> t)
                .toList());
    }

    private static Identifier key(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }
}
