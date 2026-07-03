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
                "trigger.player_death", "trigger.damage_taken", "trigger.damage_dealt",
                "trigger.item_crafted", "trigger.item_picked_up", "trigger.item_dropped",
                "trigger.food_eaten", "trigger.xp_gained", "trigger.advancement_earned",
                "trigger.dimension_entered", "trigger.biome_entered", "trigger.height_crossed",
                "trigger.health_below", "trigger.hunger_below", "trigger.sleep",
                "trigger.jump", "trigger.sneak",
                "trigger.fishing_catch", "trigger.villager_trade", "trigger.enchantment_applied",
                "trigger.item_smelted", "trigger.projectile_shot", "trigger.fall_damage_taken",
                "trigger.mob_tamed", "trigger.mob_bred", "trigger.container_opened",
                "trigger.weather_change", "trigger.time_of_day", "trigger.fixed_interval",
                "trigger.chat_message", "trigger.game_beat"),
                registries.triggers().ids());
    }

    @Test
    void effectIdsAreFrozen() {
        assertEquals(Set.of(
                "effect.apply_status_effect", "effect.remove_item_slot",
                "effect.drop_held_item", "effect.drop_inventory", "effect.give_random_item",
                "effect.give_item", "effect.teleport_random", "effect.teleport_up",
                "effect.spawn_mob", "effect.ignite", "effect.damage", "effect.heal",
                "effect.drain_hunger", "effect.change_xp", "effect.shuffle_hotbar",
                "effect.swap_inventory", "effect.swap_position", "effect.clear_effects", "effect.lightning",
                "effect.falling_anvil", "effect.launch", "effect.broadcast", "effect.play_sound",
                "effect.change_time", "effect.change_weather", "effect.replace_held_random",
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
                "modifier.disable_action", "modifier.block_interaction", "modifier.no_natural_regen",
                "modifier.time_limit", "modifier.randomize_recipes", "modifier.randomize_block_drops",
                "modifier.randomize_mob_drops", "modifier.buff_hostile_mobs", "modifier.impair_sense",
                "modifier.keep_inventory", "modifier.no_hunger_drain", "modifier.night_vision"),
                registries.modifiers().ids());
    }

    @Test
    void playerlessTriggersAreFrozen() {
        assertEquals(Set.of("trigger.weather_change", "trigger.time_of_day", "trigger.fixed_interval"),
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
