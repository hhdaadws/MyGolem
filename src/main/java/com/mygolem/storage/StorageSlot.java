package com.mygolem.storage;

import org.bukkit.inventory.ItemStack;

public record StorageSlot(Object owner, int index, ItemStack itemStack) {
}
