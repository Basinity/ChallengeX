package com.basinity.challengex.fabric.mixin;

import java.util.EnumMap;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read/write access to {@code EntityEquipment}'s otherwise-private {@code items}
 * map, so {@code modifier.share_inventory} can point every group member's
 * equipment at one shared armor/offhand map. A player keeps their own {@code
 * PlayerEquipment} object, whose mainhand override still routes to their private
 * selected slot; only the inner map is swapped, which is why sharing armor and
 * offhand needs no proxy over the equipment object itself.
 */
@Mixin(EntityEquipment.class)
public interface EntityEquipmentAccessor {

    @Accessor("items")
    EnumMap<EquipmentSlot, ItemStack> challengex$getItems();

    @Mutable
    @Accessor("items")
    void challengex$setItems(EnumMap<EquipmentSlot, ItemStack> items);
}
