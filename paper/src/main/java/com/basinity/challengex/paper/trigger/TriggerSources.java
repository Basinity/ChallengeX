package com.basinity.challengex.paper.trigger;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every trigger source the adapter registers, keyed by the catalog trigger it
 * watches for. A trigger not listed here is simply absent and no rule using it
 * ever fires.
 *
 * <p>Keyed rather than a plain list, unlike the Fabric side: sources are
 * push-based and never dispatched by id, but this map is the honest answer to
 * what the Paper adapter supports, which is what the platform-availability
 * export reads and what keeps a silently unwired trigger from shipping.
 *
 * <p>Sources register unconditionally, whatever the loaded challenge uses. A
 * source for a trigger nobody configured costs an unfired listener, while
 * gating registration on the rule list would have to be redone on every import.
 */
public final class TriggerSources {

    private TriggerSources() {
    }

    public static Map<String, TriggerSource> byId() {
        Map<String, TriggerSource> sources = new LinkedHashMap<>();
        sources.put("trigger.block_broken", new BlockBrokenTriggerSource());
        sources.put("trigger.block_placed", new BlockPlacedTriggerSource());
        sources.put("trigger.mob_killed", new MobKilledTriggerSource());
        sources.put("trigger.kill_player", new KillPlayerTriggerSource());
        sources.put("trigger.player_died", new PlayerDeathTriggerSource());
        sources.put("trigger.damage_taken", new DamageTakenTriggerSource());
        sources.put("trigger.damage_dealt", new DamageDealtTriggerSource());
        sources.put("trigger.shield_blocked", new ShieldBlockedTriggerSource());
        sources.put("trigger.crit_landed", new CritLandedTriggerSource());
        sources.put("trigger.dimension_changed", new DimensionChangedTriggerSource());
        sources.put("trigger.slept", new SleepTriggerSource());
        sources.put("trigger.chat_message", new ChatMessageTriggerSource());
        sources.put("trigger.sneaked", new SneakTriggerSource());
        sources.put("trigger.started_gliding", new StartedGlidingTriggerSource());
        sources.put("trigger.jumped", new JumpedTriggerSource());
        sources.put("trigger.biome_changed", new BiomeChangedTriggerSource());
        sources.put("trigger.height_crossed", new HeightCrossedTriggerSource());
        sources.put("trigger.health_below", new HealthBelowTriggerSource());
        sources.put("trigger.hunger_below", new HungerBelowTriggerSource());
        sources.put("trigger.level_reached", new LevelReachedTriggerSource());
        sources.put("trigger.level_interval", new LevelIntervalTriggerSource());
        sources.put("trigger.xp_gained", new XpGainedTriggerSource());
        sources.put("trigger.item_used", new ItemUsedTriggerSource());
        sources.put("trigger.block_interacted", new BlockInteractedTriggerSource());
        sources.put("trigger.effect_gained", new EffectGainedTriggerSource());
        sources.put("trigger.container_opened", new ContainerOpenedTriggerSource());
        sources.put("trigger.game_beaten", new GameBeatenTriggerSource());
        sources.put("trigger.advancement_earned", new AdvancementEarnedTriggerSource());
        sources.put("trigger.item_crafted", new ItemCraftedTriggerSource());
        sources.put("trigger.item_smelted", new ItemSmeltedTriggerSource());
        sources.put("trigger.item_dropped", new ItemDroppedTriggerSource());
        sources.put("trigger.item_picked_up", new ItemPickedUpTriggerSource());
        sources.put("trigger.food_eaten", new FoodEatenTriggerSource());
        sources.put("trigger.fish_caught", new FishCaughtTriggerSource());
        sources.put("trigger.mob_bred", new MobBredTriggerSource());
        sources.put("trigger.mob_tamed", new MobTamedTriggerSource());
        sources.put("trigger.mounted", new MountedTriggerSource());
        sources.put("trigger.projectile_shot", new ProjectileShotTriggerSource());
        sources.put("trigger.tool_broke", new ToolBrokeTriggerSource());
        sources.put("trigger.villager_traded", new VillagerTradedTriggerSource());
        sources.put("trigger.enchantment_applied", new EnchantmentAppliedTriggerSource());
        sources.put("trigger.weather_changed", new WeatherChangeTriggerSource());
        sources.put("trigger.time_of_day", new TimeOfDayTriggerSource());
        sources.put("trigger.fixed_interval", new FixedIntervalTriggerSource());
        return Map.copyOf(sources);
    }
}
