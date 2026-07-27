package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.OptionalDouble;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.Plugin;

/**
 * {@code modifier.scale_hostile_mobs}: playerless, so it scales every hostile
 * mob rather than targeting a player. The multiplier applies to both health and
 * attack damage, and cuts both ways: above one makes hostiles harder, below one
 * makes them easier, and one leaves them alone.
 *
 * <p>Two paths, as on Fabric. Spawning mobs are caught by their spawn event,
 * and every already-loaded hostile is swept the instant the modifier turns on,
 * since a mob already standing in a loaded chunk never spawns again and would
 * otherwise go unscaled. The attribute modifiers are keyed, so neither path
 * ever scales the same mob twice.
 */
public final class ScaleHostileMobsModifierSource extends EventModifierSource {

    private static final String ID = "modifier.scale_hostile_mobs";

    private static final NamespacedKey HEALTH_KEY =
            new NamespacedKey("challengex", "scale_hostile_mobs_health");
    private static final NamespacedKey DAMAGE_KEY =
            new NamespacedKey("challengex", "scale_hostile_mobs_damage");

    private boolean wasActive;

    @Override
    protected void onRegistered(Plugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            OptionalDouble scale = multiplier();
            if (scale.isPresent() && !wasActive) {
                for (World world : plugin.getServer().getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        scaleIfHostile(entity, scale.getAsDouble());
                    }
                }
            }
            wasActive = scale.isPresent();
        }, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        multiplier().ifPresent(scale -> scaleIfHostile(event.getEntity(), scale));
    }

    /** The configured multiplier while the modifier is active, else empty. */
    private OptionalDouble multiplier() {
        // Playerless: the engine ignores the player id entirely for a modifier
        // with no scope, so any id resolves it.
        Modifier modifier = context().find("", ID).orElse(null);
        if (modifier == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(CatalogBounds.clampDouble(ID, "multiplier",
                ModifierParams.decimal(modifier, "multiplier", 1.0)));
    }

    private static void scaleIfHostile(Entity entity, double multiplier) {
        if (!(entity instanceof Monster monster)) {
            return;
        }
        scale(monster, Attribute.MAX_HEALTH, HEALTH_KEY, multiplier);
        scale(monster, Attribute.ATTACK_DAMAGE, DAMAGE_KEY, multiplier);
        // Health is set after the ceiling moves, so a scaled-up mob arrives at
        // its new full health rather than at its old one, and a scaled-down mob
        // is not left above a ceiling it can no longer hold.
        AttributeInstance maxHealth = monster.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            monster.setHealth(maxHealth.getValue());
        }
    }

    private static void scale(LivingEntity living, Attribute attribute, NamespacedKey key,
            double multiplier) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        // Re-applying has to replace rather than stack, so any earlier one goes
        // first: the sweep and the spawn path can both reach the same mob.
        instance.getModifiers().stream()
                .filter(existing -> existing.getKey().equals(key))
                .forEach(instance::removeModifier);
        // ADD_MULTIPLIED_TOTAL takes the change rather than the multiplier, so a
        // multiplier of 1 adds nothing and one of 0.5 halves.
        instance.addTransientModifier(new AttributeModifier(key, multiplier - 1.0,
                AttributeModifier.Operation.ADD_SCALAR));
    }
}
