package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * A deterministic item substitution shared by {@code randomize_block_drops} and
 * {@code randomize_mob_drops}: the same original item always maps to the same
 * substitute for a given seed, rather than being re-rolled on every drop, so
 * the run stays learnable rather than reading as pure noise. The substitute
 * pool is every registered item except air.
 */
public final class RandomizedItemSubstitution {

    private static List<Item> pool;
    private static final Map<Modifier, Integer> rolledSeeds = new IdentityHashMap<>();

    private RandomizedItemSubstitution() {
    }

    /**
     * The substitute for {@code originalId} under the given modifier's {@code
     * seed} and {@code per_player} (default false) parameters. With {@code
     * per_player} on, {@code playerId} salts the mapping so different players
     * see different substitutes for the same original item.
     *
     * <p>An unset {@code seed} rolls a fresh random one the first time this
     * particular modifier activation is used, then keeps it for the rest of
     * that activation (identified by the {@link Modifier} instance itself,
     * which a preset import or dev-command reload creates fresh each time), so
     * every drop in one run stays consistent with each other but a later
     * activation with no seed still given gets its own new mapping.
     */
    public static Identifier substituteFor(Identifier originalId, Modifier modifier, String playerId) {
        int seed = resolveSeed(modifier);
        boolean perPlayer = ModifierParams.bool(modifier, "per_player", false);
        long salt = originalId.toString().hashCode();
        if (perPlayer) {
            salt = salt * 31 + playerId.hashCode();
        }
        List<Item> items = pool();
        Random random = new Random(seed * 1_000_003L + salt);
        return BuiltInRegistries.ITEM.getKey(items.get(random.nextInt(items.size())));
    }

    private static int resolveSeed(Modifier modifier) {
        if (ModifierParams.has(modifier, "seed")) {
            return ModifierParams.integer(modifier, "seed", 0);
        }
        return rolledSeeds.computeIfAbsent(modifier, ignored -> new Random().nextInt());
    }

    private static List<Item> pool() {
        if (pool == null) {
            pool = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).toList();
        }
        return pool;
    }
}
