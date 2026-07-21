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

    /** Counts the run clock down and ends it as a loss on expiry; read by the engine, never enforced per player. */
    public static final String MODIFIER_TIME_LIMIT = "modifier.time_limit";

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
        trigger(registry, "block_broken", true, optional("block", STRING).suggesting("block"));
        trigger(registry, "block_placed", true, optional("block", STRING).suggesting("block"));
        trigger(registry, "mob_killed", true, optional("mob", STRING).suggesting("mob"));
        trigger(registry, "kill_player", true, optional("name", STRING).suggesting("player"));
        trigger(registry, "player_died", true, optional("source", STRING).suggesting("damage_type"));
        trigger(registry, "damage_taken", true, optional("source", STRING).suggesting("damage_type"));
        trigger(registry, "damage_dealt", true, optional("source", STRING).suggesting("damage_type"), optional("target", STRING).suggesting("entity"));
        trigger(registry, "item_crafted", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "item_picked_up", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "item_dropped", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "food_eaten", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "xp_gained", true);
        trigger(registry, "advancement_earned", true, optional("advancement", STRING).suggesting("advancement"));
        trigger(registry, "dimension_changed", true, optional("dimension", STRING).suggesting("dimension"));
        trigger(registry, "biome_changed", true, optional("biome", STRING).suggesting("biome"));
        trigger(registry, "height_crossed", true, required("y", INT));
        trigger(registry, "health_below", true, required("hearts", DECIMAL));
        trigger(registry, "hunger_below", true, required("points", INT));
        trigger(registry, "level_reached", true, required("level", INT));
        trigger(registry, "level_interval", true, required("level", INT).atLeast(1));
        trigger(registry, "slept", true);
        trigger(registry, "jumped", true);
        trigger(registry, "sneaked", true);
        trigger(registry, "fish_caught", true);
        trigger(registry, "villager_traded", true);
        trigger(registry, "enchantment_applied", true, optional("enchantment", STRING).suggesting("enchantment"), optional("level", INT));
        trigger(registry, "item_smelted", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "projectile_shot", true, optional("projectile", STRING).suggesting("entity"));
        trigger(registry, "mob_tamed", true, optional("mob", STRING).suggesting("mob"));
        trigger(registry, "mob_bred", true, optional("mob", STRING).suggesting("mob"));
        trigger(registry, "container_opened", true, optional("container", STRING).suggesting("container"));
        trigger(registry, "item_used", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "block_interacted", true, optional("block", STRING).suggesting("block"));
        trigger(registry, "started_gliding", true);
        trigger(registry, "mounted", true, optional("mob", STRING).suggesting("entity"));
        trigger(registry, "effect_gained", true, optional("effect", STRING).suggesting("effect"));
        trigger(registry, "tool_broke", true, optional("item", STRING).suggesting("item"));
        trigger(registry, "crit_landed", true);
        trigger(registry, "shield_blocked", true);
        trigger(registry, "weather_changed", false, optional("weather", STRING).suggesting("weather"));
        trigger(registry, "time_of_day", false, required("time", STRING).suggesting("time"));
        trigger(registry, "fixed_interval", false, required("seconds", INT).bounded(1, 24 * 60 * 60));
        trigger(registry, "chat_message", true, optional("message", STRING));
        trigger(registry, "game_beaten", true);
    }

    private static void registerEffects(Registry<EffectDefinition> registry) {
        effect(registry, "apply_status_effect", true, required("effect", STRING).suggesting("effect"), optional("duration", INT), optional("amplifier", INT).bounded(1, 256));
        effect(registry, "remove_item_slot", true);
        effect(registry, "drop_held_item", true);
        effect(registry, "drop_inventory", true);
        effect(registry, "give_random_item", true);
        effect(registry, "give_item", true, required("item", STRING).suggesting("item"), optional("amount", INT).bounded(1, 64));
        effect(registry, "teleport_random", true, optional("radius", INT).atLeast(1));
        effect(registry, "teleport_up", true, optional("blocks", INT).atLeast(0));
        effect(registry, "spawn_mob", true, required("mob", STRING).suggesting("entity"), optional("count", INT).bounded(1, 100), optional("baby", BOOL));
        effect(registry, "ignite", true, optional("seconds", INT).atLeast(0));
        effect(registry, "damage", true, optional("hearts", DECIMAL).atLeast(0));
        effect(registry, "heal", true, optional("hearts", DECIMAL).atLeast(0));
        effect(registry, "change_max_health", true, required("hearts", DECIMAL));
        effect(registry, "drain_hunger", true, optional("amount", INT).atLeast(0));
        effect(registry, "restore_hunger", true, optional("amount", INT).atLeast(0));
        effect(registry, "change_xp", true, required("amount", INT), optional("set", BOOL), optional("levels", BOOL));
        effect(registry, "shuffle_hotbar", true);
        effect(registry, "swap_inventory", true);
        effect(registry, "swap_position", true);
        effect(registry, "clear_effects", true);
        effect(registry, "lightning", true);
        effect(registry, "falling_anvil", true, optional("height", INT).bounded(1, 128));
        effect(registry, "launch", true, optional("strength", DECIMAL).atLeast(0));
        effect(registry, "broadcast", true, required("text", STRING));
        effect(registry, "play_sound", true, required("sound", STRING).suggesting("sound"));
        effect(registry, "change_time", false, required("value", STRING).suggesting("time"));
        effect(registry, "change_weather", false, required("value", STRING).suggesting("weather"));
        effect(registry, "replace_held_random", true);
        effect(registry, "random_effect", true, optional("type", STRING).suggesting("effect_kind"), optional("seconds", DECIMAL));
        effect(registry, "freeze", true, optional("seconds", INT).atLeast(1));
        effect(registry, "knockback", true, optional("strength", DECIMAL).atLeast(0));
        effect(registry, "explode", true, optional("power", DECIMAL).atLeast(0));
        effect(registry, "clear_inventory", true);
        effect(registry, "repair_held_item", true, optional("amount", INT).atLeast(0));
        effect(registry, "damage_held_item", true, optional("amount", INT).atLeast(0));
        effect(registry, "kill", true);
        effect(registry, "lose_challenge", false);
    }

    private static void registerGoals(Registry<GoalDefinition> registry) {
        goal(registry, "kill_mob", List.of(new GoalRequirement(Set.of("trigger.mob_killed"), Map.of("mob", fromGoalParam("mob")))), required("mob", STRING).suggesting("mob"));
        goal(registry, "obtain_item", List.of(new GoalRequirement(Set.of("trigger.item_picked_up", "trigger.item_crafted"), Map.of("item", fromGoalParam("item")))), required("item", STRING).suggesting("item"));
        goal(registry, "earn_advancement", List.of(new GoalRequirement(Set.of("trigger.advancement_earned"), Map.of("advancement", fromGoalParam("advancement")))), required("advancement", STRING).suggesting("advancement"));
        goal(registry, "beat_game", List.of(new GoalRequirement(Set.of("trigger.game_beaten"), Map.of())));
    }

    private static void registerModifiers(Registry<ModifierDefinition> registry) {
        modifier(registry, "disable_jump", true);
        modifier(registry, "disable_item_use", true, optional("item", STRING).suggesting("item"));
        modifier(registry, "disable_interaction", true, required("target", STRING).suggesting("block"));
        modifier(registry, "no_natural_regen", true);
        modifier(registry, "time_limit", false, required("minutes", INT));
        modifier(registry, "randomize_block_drops", true, optional("seed", INT), optional("per_player", BOOL));
        modifier(registry, "randomize_mob_drops", true, optional("seed", INT), optional("per_player", BOOL));
        modifier(registry, "buff_hostile_mobs", false);
        modifier(registry, "status_effect", true, required("effect", STRING).suggesting("effect"), optional("amplifier", INT).bounded(1, 256));
        modifier(registry, "keep_inventory", true);
        modifier(registry, "no_hunger_drain", true);
        modifier(registry, "share_inventory", true);
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