package com.basinity.challengex.paper.effect;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

/**
 * Turns the catalog's namespaced id strings into Bukkit registry entries. Every
 * lookup returns null rather than throwing for an id the server does not know,
 * so a handler can log and skip the way the Fabric adapter's registry lookups
 * already do.
 *
 * <p>Centralized so the registry access sits in one file: the way Bukkit
 * exposes its registries has moved around across versions, and a handler should
 * not have to care.
 */
public final class GameIds {

    /**
     * Every material that is actually an item, which is what the random-item
     * effects draw from. Built once: the registry does not change at runtime,
     * and walking it per player per trigger would be wasteful.
     */
    private static final class ItemPool {
        private static final java.util.List<Material> ALL = Registry.MATERIAL.stream()
                .filter(Material::isItem)
                .toList();
    }

    private GameIds() {
    }

    /** Every obtainable item, for the effects that hand out a random one. */
    public static java.util.List<Material> allItems() {
        return ItemPool.ALL;
    }

    /** Parses a namespaced id, defaulting the namespace to {@code minecraft}. */
    public static NamespacedKey key(String id) {
        return id == null ? null : NamespacedKey.fromString(id.toLowerCase(java.util.Locale.ROOT));
    }

    public static PotionEffectType effect(String id) {
        NamespacedKey parsed = key(id);
        return parsed == null ? null : Registry.EFFECT.get(parsed);
    }

    public static Material item(String id) {
        NamespacedKey parsed = key(id);
        if (parsed == null) {
            return null;
        }
        Material material = Registry.MATERIAL.get(parsed);
        return material != null && material.isItem() ? material : null;
    }

    public static EntityType entity(String id) {
        NamespacedKey parsed = key(id);
        return parsed == null ? null : Registry.ENTITY_TYPE.get(parsed);
    }

    public static Sound sound(String id) {
        NamespacedKey parsed = key(id);
        return parsed == null ? null : Registry.SOUND_EVENT.get(parsed);
    }
}
