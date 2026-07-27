package com.basinity.challengex.common.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.Random;

/**
 * A deterministic item substitution shared by {@code randomize_block_drops},
 * {@code randomize_mob_drops} and {@code randomize_crafting}: the same original
 * item always maps to the same substitute for a given seed, rather than being
 * re-rolled on every drop, so the run stays learnable rather than reading as
 * pure noise.
 *
 * <p>Only the arithmetic lives here. Which items are in the pool, and in what
 * order, is each adapter's own, and the two do not agree: a seed picks the same
 * index on both platforms but that index is not the same item, since the
 * platforms enumerate their item registries differently.
 */
public final class RandomizedSubstitution {

    private RandomizedSubstitution() {
    }

    /**
     * The index into a pool of {@code poolSize} items that {@code originalId}
     * maps to, under the modifier's {@code seed} and {@code per_player}
     * (default false) parameters. With {@code per_player} on, {@code playerId}
     * salts the mapping so different players see different substitutes for the
     * same original item.
     */
    public static int substituteIndex(String originalId, Modifier modifier, String playerId, int poolSize) {
        int seed = ModifierParams.seed(modifier);
        boolean perPlayer = ModifierParams.bool(modifier, "per_player", false);
        long salt = originalId.hashCode();
        if (perPlayer) {
            salt = salt * 31 + playerId.hashCode();
        }
        return new Random(seed * 1_000_003L + salt).nextInt(poolSize);
    }
}
