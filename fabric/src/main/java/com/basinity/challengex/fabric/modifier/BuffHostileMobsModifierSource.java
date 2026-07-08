package com.basinity.challengex.fabric.modifier;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

/**
 * {@code modifier.buff_hostile_mobs}: playerless, so it buffs every hostile
 * mob rather than targeting a player. Rides {@link ServerEntityEvents#ENTITY_LOAD},
 * which fires for a freshly spawned mob and one loaded back in from a saved
 * chunk alike, so mobs already in the world when the modifier activates get
 * buffed too as their chunks load. The attribute modifiers are added or
 * updated under fixed ids rather than stacked, so a mob re-entering a chunk
 * is never buffed twice.
 */
public final class BuffHostileMobsModifierSource implements ModifierSource {

    private static final Identifier HEALTH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("challengex", "buff_hostile_mobs_health");
    private static final Identifier DAMAGE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("challengex", "buff_hostile_mobs_damage");
    private static final double BUFF_MULTIPLIER = 0.5;

    @Override
    public void register(ModifierContext context) {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Enemy && entity instanceof LivingEntity living
                    && context.isGloballyActive("modifier.buff_hostile_mobs")) {
                buff(living, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID);
                buff(living, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID);
                living.setHealth(living.getMaxHealth());
            }
        });
    }

    private void buff(LivingEntity living, Holder<Attribute> attribute, Identifier modifierId) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(modifierId, BUFF_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
