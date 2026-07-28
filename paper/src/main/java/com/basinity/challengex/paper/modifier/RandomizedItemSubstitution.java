package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.RandomizedSubstitution;
import com.basinity.challengex.core.model.Modifier;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Registry;

/**
 * The Paper half of the randomizers: which items can come out. The arithmetic
 * that decides the mapping is {@link RandomizedSubstitution}'s and is shared
 * with the Fabric adapter.
 *
 * <p>The pool is sorted by id rather than left in registry order, so a given
 * seed maps the same way on every server and across game versions. Fabric takes
 * its registry's own order, which means a seed does not name the same
 * substitutes on both platforms; the mapping is self-consistent within a run,
 * which is what makes a randomizer learnable, rather than portable between them.
 */
final class RandomizedItemSubstitution {

    private static final class Pool {
        // Air is excluded for the same reason the mod excludes it: substituting
        // it would delete the drop rather than randomize it.
        private static final List<Material> ALL = Registry.MATERIAL.stream()
                .filter(material -> material.isItem() && !material.isAir())
                .sorted(Comparator.comparing(material -> material.getKey().toString()))
                .toList();
    }

    private RandomizedItemSubstitution() {
    }

    /** The substitute for this item under the modifier's seed and per-player settings. */
    static Material substituteFor(Material original, Modifier modifier, String playerId) {
        List<Material> pool = Pool.ALL;
        if (pool.isEmpty()) {
            return original;
        }
        int index = RandomizedSubstitution.substituteIndex(
                original.getKey().toString(), modifier, playerId, pool.size());
        return pool.get(index);
    }
}
