package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.SharedInventory;
import com.basinity.challengex.fabric.modifier.SharedInventoryAccess;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Wires {@code modifier.share_inventory}'s by-reference sharing into {@code
 * Inventory}. Reassigning the private-final {@code items} list and swapping the
 * equipment's inner map is exactly the internal access the public-API route
 * can't reach; a {@link Mutable} shadow and the {@link EntityEquipmentAccessor}
 * are how it is reached here without an access-widener.
 */
@Mixin(Inventory.class)
public abstract class SharedInventoryMixin implements SharedInventoryAccess {

    @Mutable
    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Shadow
    @Final
    private EntityEquipment equipment;

    @Override
    public void challengex$seedInto(SharedInventory shared) {
        NonNullList<ItemStack> main = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) {
            main.set(slot, items.get(slot).copy());
        }
        shared.main = main;
        shared.equipment = copyEquipment(((EntityEquipmentAccessor) (Object) equipment).challengex$getItems());
    }

    @Override
    public void challengex$share(SharedInventory shared) {
        this.items = shared.main;
        ((EntityEquipmentAccessor) (Object) equipment).challengex$setItems(shared.equipment);
    }

    @Override
    public void challengex$detach() {
        NonNullList<ItemStack> privateItems = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < items.size(); slot++) {
            privateItems.set(slot, items.get(slot).copy());
        }
        this.items = privateItems;
        EntityEquipmentAccessor accessor = (EntityEquipmentAccessor) (Object) equipment;
        accessor.challengex$setItems(copyEquipment(accessor.challengex$getItems()));
    }

    private static EnumMap<EquipmentSlot, ItemStack> copyEquipment(EnumMap<EquipmentSlot, ItemStack> source) {
        EnumMap<EquipmentSlot, ItemStack> copy = new EnumMap<>(EquipmentSlot.class);
        for (Map.Entry<EquipmentSlot, ItemStack> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }
}
