package com.basinity.challengex.fabric.trigger;

import java.util.List;

/**
 * Every trigger source the adapter registers, the trigger-side counterpart of
 * the effect handler map. Sources are push-based rather than dispatched by id,
 * so this is a plain list; a trigger not yet wired is simply absent and no rule
 * using it ever fires.
 *
 * <p>Sources register unconditionally, whatever the loaded challenge uses. A
 * source for a trigger nobody configured costs an unfired listener, and gating
 * registration on the rule list would silently starve goals, which consume
 * trigger events of their own.
 */
public final class TriggerSources {

    private TriggerSources() {
    }

    public static List<TriggerSource> all() {
        return List.of(
                new BlockBrokenTriggerSource(),
                new MobKilledTriggerSource(),
                new PlayerDeathTriggerSource(),
                new DamageTakenTriggerSource(),
                new DamageDealtTriggerSource(),
                new DimensionChangedTriggerSource(),
                new SleepTriggerSource(),
                new ChatMessageTriggerSource(),
                new SneakTriggerSource(),
                new BiomeChangedTriggerSource(),
                new HeightCrossedTriggerSource(),
                new HealthBelowTriggerSource(),
                new HungerBelowTriggerSource(),
                new WeatherChangeTriggerSource(),
                new TimeOfDayTriggerSource(),
                new FixedIntervalTriggerSource());
    }
}
