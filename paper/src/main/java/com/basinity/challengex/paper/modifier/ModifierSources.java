package com.basinity.challengex.paper.modifier;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The modifiers enforced by cancelling a game action, keyed by modifier id.
 * This is where the Paper adapter gains most on the Fabric one: stopping an
 * action is what a cancellable event is for, so several modifiers that needed a
 * Mixin over there are a handler and a {@code setCancelled} here.
 *
 * <p>Keyed, like the trigger sources and unlike the Fabric list, because
 * together with {@link ModifierEnforcers} this is the honest answer to which
 * modifiers the adapter supports, which the availability export reads.
 *
 * <p>{@code modifier.item_lock} appears in both maps: the enforcer decides who
 * holds what, and the source refuses the acquisitions that would otherwise have
 * to be undone.
 */
public final class ModifierSources {

    private ModifierSources() {
    }

    public static Map<String, ModifierSource> byId() {
        Map<String, ModifierSource> sources = new LinkedHashMap<>();
        sources.put("modifier.disable_jump", new DisableJumpModifierSource());
        sources.put("modifier.disable_item_use", new DisableItemUseModifierSource());
        sources.put("modifier.disable_interaction", new DisableInteractionModifierSource());
        sources.put("modifier.disable_item_drop", new DisableItemDropModifierSource());
        sources.put("modifier.disable_item_pickup", new DisableItemPickupModifierSource());
        sources.put("modifier.no_natural_regen", new NoNaturalRegenModifierSource());
        sources.put("modifier.keep_inventory", new KeepInventoryModifierSource());
        sources.put("modifier.scale_hostile_mobs", new ScaleHostileMobsModifierSource());
        sources.put("modifier.randomize_block_drops", new RandomizeBlockDropsModifierSource());
        sources.put("modifier.randomize_mob_drops", new RandomizeMobDropsModifierSource());
        sources.put("modifier.randomize_crafting", new RandomizeCraftingModifierSource());
        sources.put("modifier.item_lock", new ItemLockModifierSource());
        return Map.copyOf(sources);
    }
}
