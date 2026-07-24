package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.OptionalDouble;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

/**
 * {@code modifier.scale_hostile_mobs}: playerless, so it scales every hostile
 * mob rather than targeting a player. The multiplier applies to both health and
 * attack damage, and cuts both ways: above one makes hostiles harder, below one
 * makes them easier, and one leaves them alone.
 *
 * <p>Rides {@link ServerEntityEvents#ENTITY_LOAD} for mobs spawning or loading
 * back in from a saved chunk after the modifier is already active, and
 * separately sweeps every currently loaded hostile mob the instant the modifier
 * transitions from inactive to active, since a mob already standing in an
 * already-loaded chunk never fires an entity-load event of its own and would
 * otherwise go unscaled until it happened to unload and reload. The attribute
 * modifiers are added or updated under fixed ids rather than stacked, so
 * neither path ever scales the same mob twice.
 */
public final class ScaleHostileMobsModifierSource implements ModifierSource {

    private static final String ID = "modifier.scale_hostile_mobs";

    private static final Identifier HEALTH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("challengex", "scale_hostile_mobs_health");
    private static final Identifier DAMAGE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("challengex", "scale_hostile_mobs_damage");

    private boolean wasActive;

    @Override
    public void register(ModifierContext context) {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
                multiplier(context).ifPresent(scale -> scaleIfHostile(entity, scale)));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            OptionalDouble scale = multiplier(context);
            if (scale.isPresent() && !wasActive) {
                for (ServerLevel level : server.getAllLevels()) {
                    for (Entity entity : level.getAllEntities()) {
                        scaleIfHostile(entity, scale.getAsDouble());
                    }
                }
            }
            wasActive = scale.isPresent();
        });
    }

    /** The configured multiplier while the modifier is active, else empty. */
    private OptionalDouble multiplier(ModifierContext context) {
        Modifier modifier = context.find("", ID).orElse(null);
        if (modifier == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(CatalogBounds.clampDouble(ID, "multiplier",
                ModifierParams.decimal(modifier, "multiplier", 1.0)));
    }

    private void scaleIfHostile(Entity entity, double multiplier) {
        if (entity instanceof Enemy && entity instanceof LivingEntity living) {
            scale(living, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID, multiplier);
            scale(living, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID, multiplier);
            // Health is set after the ceiling moves, so a scaled-up mob arrives
            // at its new full health rather than at its old one, and a
            // scaled-down mob is not left above a ceiling it can no longer hold.
            living.setHealth(living.getMaxHealth());
        }
    }

    private void scale(LivingEntity living, Holder<Attribute> attribute, Identifier modifierId,
            double multiplier) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) {
            // ADD_MULTIPLIED_TOTAL takes the change rather than the multiplier,
            // so a multiplier of 1 adds nothing and one of 0.5 halves.
            instance.addOrUpdateTransientModifier(new AttributeModifier(
                    modifierId, multiplier - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
