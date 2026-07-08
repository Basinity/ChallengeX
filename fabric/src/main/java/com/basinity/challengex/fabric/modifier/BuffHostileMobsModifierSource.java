package com.basinity.challengex.fabric.modifier;

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
 * {@code modifier.buff_hostile_mobs}: playerless, so it buffs every hostile
 * mob rather than targeting a player. Rides {@link ServerEntityEvents#ENTITY_LOAD}
 * for mobs spawning or loading back in from a saved chunk after the modifier is
 * already active, and separately sweeps every currently loaded hostile mob the
 * instant the modifier transitions from inactive to active, since a mob already
 * standing in an already-loaded chunk never fires an entity-load event of its
 * own and would otherwise go unbuffed until it happened to unload and reload.
 * The attribute modifiers are added or updated under fixed ids rather than
 * stacked, so neither path ever buffs the same mob twice.
 */
public final class BuffHostileMobsModifierSource implements ModifierSource {

    private static final Identifier HEALTH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("challengex", "buff_hostile_mobs_health");
    private static final Identifier DAMAGE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("challengex", "buff_hostile_mobs_damage");
    private static final double BUFF_MULTIPLIER = 0.5;

    private boolean wasActive;

    @Override
    public void register(ModifierContext context) {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (context.isGloballyActive("modifier.buff_hostile_mobs")) {
                buffIfHostile(entity);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean active = context.isGloballyActive("modifier.buff_hostile_mobs");
            if (active && !wasActive) {
                for (ServerLevel level : server.getAllLevels()) {
                    for (Entity entity : level.getAllEntities()) {
                        buffIfHostile(entity);
                    }
                }
            }
            wasActive = active;
        });
    }

    private void buffIfHostile(Entity entity) {
        if (entity instanceof Enemy && entity instanceof LivingEntity living) {
            buff(living, Attributes.MAX_HEALTH, HEALTH_MODIFIER_ID);
            buff(living, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER_ID);
            living.setHealth(living.getMaxHealth());
        }
    }

    private void buff(LivingEntity living, Holder<Attribute> attribute, Identifier modifierId) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(modifierId, BUFF_MULTIPLIER, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
