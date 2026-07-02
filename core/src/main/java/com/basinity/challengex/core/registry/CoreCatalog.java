package com.basinity.challengex.core.registry;

import static com.basinity.challengex.core.registry.ParamBinding.fromGoalParam;
import static com.basinity.challengex.core.registry.ParamSpec.optional;
import static com.basinity.challengex.core.registry.ParamSpec.required;
import static com.basinity.challengex.core.registry.ParamType.BOOL;
import static com.basinity.challengex.core.registry.ParamType.DECIMAL;
import static com.basinity.challengex.core.registry.ParamType.INT;
import static com.basinity.challengex.core.registry.ParamType.STRING;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The starter catalogs. Every id and parameter name registered here is frozen
 * forever once presets circulate: renames break shared presets with no
 * migration path, by design. Additions are fine; renames and removals are not.
 */
public final class CoreCatalog {

    /** Ends the run as a loss; handled by the engine itself, never dispatched to an adapter. */
    public static final String EFFECT_LOSE_CHALLENGE = "effect.lose_challenge";

    private CoreCatalog() {
    }

    public static Registries createRegistries() {
        Registry<TriggerDefinition> triggers = new Registry<>("trigger");
        Registry<EffectDefinition> effects = new Registry<>("effect");
        Registry<GoalDefinition> goals = new Registry<>("goal");
        Registry<ModifierDefinition> modifiers = new Registry<>("modifier");
        registerTriggers(triggers);
        registerEffects(effects);
        registerGoals(goals);
        registerModifiers(modifiers);
        return new Registries(triggers, effects, goals, modifiers);
    }

    private static void registerTriggers(Registry<TriggerDefinition> registry) {
        trigger(registry, "block_broken", true, optional("block", STRING));
        trigger(registry, "block_placed", true, optional("block", STRING));
        trigger(registry, "mob_killed", true, optional("mob", STRING));
        trigger(registry, "player_death", true);
        trigger(registry, "damage_taken", true, optional("source", STRING));
        trigger(registry, "damage_dealt", true);
        trigger(registry, "item_crafted", true, optional("item", STRING));
        trigger(registry, "item_picked_up", true, optional("item", STRING));
        trigger(registry, "item_dropped", true);
        trigger(registry, "food_eaten", true, optional("item", STRING));
        trigger(registry, "xp_gained", true);
        trigger(registry, "advancement_earned", true);
        trigger(registry, "dimension_entered", true, optional("dimension", STRING));
        trigger(registry, "biome_entered", true, optional("biome", STRING));
        trigger(registry, "height_crossed", true, required("y", INT));
        trigger(registry, "health_below", true, required("hearts", DECIMAL));
        trigger(registry, "hunger_below", true, required("points", INT));
        trigger(registry, "sleep", true);
        trigger(registry, "jump", true);
        trigger(registry, "sneak", true);
        trigger(registry, "fishing_catch", true);
        trigger(registry, "villager_trade", true);
        trigger(registry, "enchantment_applied", true);
        trigger(registry, "item_smelted", true);
        trigger(registry, "projectile_shot", true);
        trigger(registry, "fall_damage_taken", true);
        trigger(registry, "mob_tamed", true);
        trigger(registry, "mob_bred", true);
        trigger(registry, "container_opened", true);
        trigger(registry, "weather_change", false);
        trigger(registry, "time_of_day", false, required("time", STRING));
        trigger(registry, "fixed_interval", false, required("seconds", INT));
        trigger(registry, "chat_message", true,  optional("message", STRING));
        trigger(registry, "game_beat", true);
    }

    private static void registerEffects(Registry<EffectDefinition> registry) {
        effect(registry, "apply_status_effect", true, required("effect", STRING), optional("duration", INT), optional("amplifier", INT));
        effect(registry, "remove_item_slot", true);
        effect(registry, "lock_item_slot", true);
        effect(registry, "drop_held_item", true);
        effect(registry, "drop_inventory", true);
        effect(registry, "give_random_item", true);
        effect(registry, "give_item", true, required("item", STRING));
        effect(registry, "teleport_random", true, optional("radius", INT));
        effect(registry, "teleport_up", true, optional("blocks", INT));
        effect(registry, "spawn_mob", true, required("mob", STRING), optional("count", INT));
        effect(registry, "ignite", true, optional("seconds", INT));
        effect(registry, "damage", true, optional("hearts", DECIMAL));
        effect(registry, "heal", true);
        effect(registry, "drain_hunger", true);
        effect(registry, "change_xp", true, required("amount", INT));
        effect(registry, "shuffle_hotbar", true);
        effect(registry, "swap_inventory", true);
        effect(registry, "clear_effects", true);
        effect(registry, "lightning", true);
        effect(registry, "falling_anvil", true, required("y", INT));
        effect(registry, "launch", true, optional("strength", DECIMAL));
        effect(registry, "broadcast", false, required("text", STRING));
        effect(registry, "play_sound", true, required("sound", STRING));
        effect(registry, "change_time", false, required("value", STRING));
        effect(registry, "change_weather", false, required("value", STRING));
        effect(registry, "replace_held_random", true);
        effect(registry, "kill", true);
        effect(registry, "lose_challenge", false);
    }

    private static void registerGoals(Registry<GoalDefinition> registry) {
        goal(registry, "kill_mob", List.of(new GoalRequirement(Set.of("trigger.mob_killed"), Map.of("mob", fromGoalParam("mob")))), required("mob", STRING));
        goal(registry, "obtain_item", List.of(new GoalRequirement(Set.of("trigger.item_picked_up", "trigger.item_crafted"), Map.of("item", fromGoalParam("item")))), required("item", STRING));
        goal(registry, "earn_advancement", List.of(new GoalRequirement(Set.of("trigger.advancement_earned"), Map.of("advancement", fromGoalParam("advancement")))), required("advancement", STRING));
        goal(registry, "beat_game", List.of(new GoalRequirement(Set.of("trigger.game_beat"), Map.of())));
    }

    private static void registerModifiers(Registry<ModifierDefinition> registry) {
        modifier(registry, "disable_action", true, required("action", STRING));
        modifier(registry, "block_interaction", true, required("target", STRING));
        modifier(registry, "no_natural_regen", true);
        modifier(registry, "time_limit", false, required("minutes", INT));
        modifier(registry, "randomize_recipes", true, optional("seed", INT), optional("per_player", BOOL));
        modifier(registry, "randomize_block_drops", true, optional("seed", INT), optional("per_player", BOOL));
        modifier(registry, "randomize_mob_drops", true, optional("seed", INT), optional("per_player", BOOL));
        modifier(registry, "buff_hostile_mobs", false);
        modifier(registry, "impair_sense", true, required("sense", STRING));
        modifier(registry, "keep_inventory", true);
        modifier(registry, "no_hunger_drain", true);
        modifier(registry, "night_vision", true);
    }

    private static void trigger(Registry<TriggerDefinition> registry, String name, boolean scoped, ParamSpec... params) {
        registry.register(new TriggerDefinition("trigger." + name, scoped, List.of(params)));
    }

    private static void effect(Registry<EffectDefinition> registry, String name, boolean scoped, ParamSpec... params) {
        registry.register(new EffectDefinition("effect." + name, scoped, List.of(params)));
    }

    private static void goal(Registry<GoalDefinition> registry, String name, List<GoalRequirement> requirements, ParamSpec... params) {
        registry.register(new GoalDefinition("goal." + name, requirements, List.of(params)));
    }

    private static void modifier(Registry<ModifierDefinition> registry, String name, boolean scoped, ParamSpec... params) {
        registry.register(new ModifierDefinition("modifier." + name, scoped, List.of(params)));
    }
}