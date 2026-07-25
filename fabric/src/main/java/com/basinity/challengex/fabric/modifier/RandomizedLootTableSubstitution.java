package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A deterministic loot table substitution for {@code randomize_chest_loot}: the
 * same container table always maps to the same other container table for a
 * given seed, so a village chest holding desert temple loot holds desert temple
 * loot every time rather than something new on each chest.
 *
 * <p>Swapping the table rather than the items it produced is what keeps the
 * result readable as loot. A table decides how many stacks to roll, how large
 * they are and what enchantments they carry, so a substituted table still gives
 * a chest's worth of a coherent structure's loot; substituting the items one by
 * one instead would leave a village chest's shape filled with unrelated items.
 *
 * <p>The pool is every container table the server has loaded, which includes
 * any a datapack adds, sorted by id so the mapping a seed produces does not
 * depend on registry iteration order. It is recomputed whenever a datapack
 * reload hands out a new registry.
 */
public final class RandomizedLootTableSubstitution {

    private static final String CONTAINER_PREFIX = "chests/";

    private static HolderLookup.Provider pooledFrom;
    private static List<ResourceKey<LootTable>> pool;

    private RandomizedLootTableSubstitution() {
    }

    /** Whether this is a container's loot table, the only category this substitution covers. */
    public static boolean isContainerTable(ResourceKey<LootTable> key) {
        return key.identifier().getPath().startsWith(CONTAINER_PREFIX);
    }

    /**
     * The table to roll in place of {@code originalKey} under the given
     * modifier's {@code seed} and {@code per_player} (default false) parameters.
     * With {@code per_player} on, {@code playerId} salts the mapping so
     * different players find different loot in the same kind of chest.
     */
    public static ResourceKey<LootTable> substituteFor(
            ResourceKey<LootTable> originalKey, Modifier modifier, String playerId, MinecraftServer server) {
        List<ResourceKey<LootTable>> tables = pool(server);
        if (tables.isEmpty()) {
            return originalKey;
        }
        long salt = originalKey.identifier().toString().hashCode();
        if (ModifierParams.bool(modifier, "per_player", false)) {
            salt = salt * 31 + playerId.hashCode();
        }
        Random random = new Random(ModifierParams.seed(modifier) * 1_000_003L + salt);
        return tables.get(random.nextInt(tables.size()));
    }

    private static List<ResourceKey<LootTable>> pool(MinecraftServer server) {
        HolderLookup.Provider lookup = server.reloadableRegistries().lookup();
        if (pool == null || pooledFrom != lookup) {
            pooledFrom = lookup;
            pool = lookup.lookupOrThrow(Registries.LOOT_TABLE).listElementIds()
                    .filter(RandomizedLootTableSubstitution::isContainerTable)
                    .sorted(Comparator.comparing(key -> key.identifier().toString()))
                    .toList();
        }
        return pool;
    }
}
