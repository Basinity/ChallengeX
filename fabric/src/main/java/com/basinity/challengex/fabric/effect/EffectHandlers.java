package com.basinity.challengex.fabric.effect;

import java.util.HashMap;
import java.util.Map;

/**
 * The effect id to handler map the executor dispatches through. Effects not yet
 * wired are simply absent; the executor logs and skips an unknown id. Handlers
 * land here as the building-block-library phase implements them.
 */
public final class EffectHandlers {

    private EffectHandlers() {
    }

    public static Map<String, EffectHandler> byId() {
        Map<String, EffectHandler> handlers = new HashMap<>();
        handlers.put("effect.apply_status_effect", new ApplyStatusEffectHandler());
        handlers.put("effect.heal", new HealHandler());
        handlers.put("effect.clear_effects", new ClearEffectsHandler());
        handlers.put("effect.ignite", new IgniteHandler());
        handlers.put("effect.kill", new KillHandler());
        handlers.put("effect.damage", new DamageHandler());
        handlers.put("effect.give_item", new GiveItemHandler());
        handlers.put("effect.drop_held_item", new DropHeldItemHandler());
        handlers.put("effect.remove_item_slot", new RemoveItemSlotHandler());
        handlers.put("effect.drop_inventory", new DropInventoryHandler());
        handlers.put("effect.drain_hunger", new DrainHungerHandler());
        handlers.put("effect.change_xp", new ChangeXpHandler());
        handlers.put("effect.teleport_up", new TeleportUpHandler());
        handlers.put("effect.launch", new LaunchHandler());
        handlers.put("effect.play_sound", new PlaySoundHandler());
        handlers.put("effect.broadcast", new BroadcastHandler());
        handlers.put("effect.give_random_item", new GiveRandomItemHandler());
        handlers.put("effect.replace_held_random", new ReplaceHeldRandomHandler());
        handlers.put("effect.shuffle_hotbar", new ShuffleHotbarHandler());
        handlers.put("effect.spawn_mob", new SpawnMobHandler());
        handlers.put("effect.lightning", new LightningHandler());
        handlers.put("effect.falling_anvil", new FallingAnvilHandler());
        handlers.put("effect.change_weather", new ChangeWeatherHandler());
        handlers.put("effect.change_time", new ChangeTimeHandler());
        handlers.put("effect.teleport_random", new TeleportRandomHandler());
        handlers.put("effect.swap_inventory", new SwapInventoryHandler());
        handlers.put("effect.swap_position", new SwapPositionHandler());
        handlers.put("effect.random_effect", new RandomEffectHandler());
        handlers.put("effect.freeze", new FreezeHandler());
        handlers.put("effect.knockback", new KnockbackHandler());
        handlers.put("effect.explode", new ExplodeHandler());
        handlers.put("effect.clear_inventory", new ClearInventoryHandler());
        handlers.put("effect.repair_held_item", new RepairHeldItemHandler());
        handlers.put("effect.damage_held_item", new DamageHeldItemHandler());
        return Map.copyOf(handlers);
    }
}
