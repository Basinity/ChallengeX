package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.common.modifier.RandomizedSubstitution;
import com.basinity.challengex.core.model.Modifier;
import java.util.List;
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

    private RandomizedItemSubstitution() {
    }

    /**
     * The substitute for {@code originalId} under the given modifier's {@code
     * seed} and {@code per_player} (default false) parameters. With {@code
     * per_player} on, {@code playerId} salts the mapping so different players
     * see different substitutes for the same original item.
     */
    public static Identifier substituteFor(Identifier originalId, Modifier modifier, String playerId) {
        List<Item> items = pool();
        int index = RandomizedSubstitution.substituteIndex(
                originalId.toString(), modifier, playerId, items.size());
        return BuiltInRegistries.ITEM.getKey(items.get(index));
    }

    private static List<Item> pool() {
        if (pool == null) {
            pool = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).toList();
        }
        return pool;
    }
}
