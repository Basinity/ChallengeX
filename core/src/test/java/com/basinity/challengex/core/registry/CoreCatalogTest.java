package com.basinity.challengex.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The freeze tripwire. These sets are the durable preset vocabulary: renaming
 * or removing an id breaks every shared preset with no migration path, so a
 * failure here means stop and reconsider. Additions belong in both the catalog
 * and these sets. Scope capability is part of the contract too: flipping an
 * entry between scoped and playerless changes what presets are valid, so the
 * playerless sets are pinned exactly as well.
 */
class CoreCatalogTest {

    private final Registries registries = CoreCatalog.createRegistries();

    @Test
    void triggerIdsAreFrozen() {
        assertEquals(Set.of(
                "trigger.block_broken", "trigger.block_placed", "trigger.mob_killed",
                "trigger.kill_player",
                "trigger.player_died", "trigger.damage_taken", "trigger.damage_dealt",
                "trigger.item_crafted", "trigger.item_picked_up", "trigger.item_dropped",
                "trigger.food_eaten", "trigger.xp_gained", "trigger.advancement_earned",
                "trigger.dimension_changed", "trigger.biome_changed", "trigger.height_crossed",
                "trigger.health_below", "trigger.hunger_below",
                "trigger.level_reached", "trigger.level_interval", "trigger.slept",
                "trigger.jumped", "trigger.sneaked",
                "trigger.fish_caught", "trigger.villager_traded", "trigger.enchantment_applied",
                "trigger.item_smelted", "trigger.projectile_shot",
                "trigger.mob_tamed", "trigger.mob_bred", "trigger.container_opened",
                "trigger.item_used", "trigger.block_interacted", "trigger.started_gliding",
                "trigger.mounted", "trigger.effect_gained", "trigger.tool_broke",
                "trigger.crit_landed", "trigger.shield_blocked",
                "trigger.weather_changed", "trigger.time_of_day", "trigger.fixed_interval",
                "trigger.chat_message", "trigger.game_beaten"),
                registries.triggers().ids());
    }

    @Test
    void effectIdsAreFrozen() {
        assertEquals(Set.of(
                "effect.apply_status_effect", "effect.remove_item_slot",
                "effect.drop_held_item", "effect.drop_inventory", "effect.give_random_item",
                "effect.give_item", "effect.teleport_random", "effect.teleport_up",
                "effect.spawn_mob", "effect.ignite", "effect.damage", "effect.heal",
                "effect.change_max_health",
                "effect.drain_hunger", "effect.change_xp", "effect.shuffle_hotbar",
                "effect.swap_inventory", "effect.swap_position", "effect.clear_effects", "effect.lightning",
                "effect.falling_anvil", "effect.launch", "effect.broadcast", "effect.play_sound",
                "effect.change_time", "effect.change_weather", "effect.replace_held_random",
                "effect.random_effect", "effect.freeze", "effect.knockback", "effect.explode",
                "effect.clear_inventory", "effect.repair_held_item", "effect.damage_held_item",
                "effect.kill", "effect.lose_challenge"),
                registries.effects().ids());
    }

    @Test
    void goalIdsAreFrozen() {
        assertEquals(Set.of(
                "goal.kill_mob", "goal.obtain_item", "goal.earn_advancement",
                "goal.beat_game"),
                registries.goals().ids());
    }

    @Test
    void modifierIdsAreFrozen() {
        assertEquals(Set.of(
                "modifier.disable_jump", "modifier.disable_item_use", "modifier.block_interaction",
                "modifier.no_natural_regen", "modifier.time_limit",
                "modifier.randomize_block_drops", "modifier.randomize_mob_drops", "modifier.buff_hostile_mobs",
                "modifier.status_effect", "modifier.keep_inventory", "modifier.no_hunger_drain"),
                registries.modifiers().ids());
    }

    @Test
    void playerlessTriggersAreFrozen() {
        assertEquals(Set.of("trigger.weather_changed", "trigger.time_of_day", "trigger.fixed_interval"),
                playerlessIds(registries.triggers()));
    }

    @Test
    void playerlessEffectsAreFrozen() {
        assertEquals(Set.of("effect.change_time", "effect.change_weather", "effect.lose_challenge"),
                playerlessIds(registries.effects()));
    }

    @Test
    void playerlessModifiersAreFrozen() {
        assertEquals(Set.of("modifier.time_limit", "modifier.buff_hostile_mobs"),
                playerlessIds(registries.modifiers()));
    }

    private static Set<String> playerlessIds(Registry<? extends Definition> registry) {
        return registry.all().stream()
                .filter(definition -> !definition.scoped())
                .map(Definition::id)
                .collect(Collectors.toSet());
    }
}
