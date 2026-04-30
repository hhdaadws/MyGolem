package com.mygolem.storage;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class BackpackInventory {

    private BackpackInventory() {
    }

    public static Inventory create(BackpackSnapshot snapshot, ItemStackCodec codec, String title) {
        Inventory inventory = Bukkit.createInventory(null, BackpackSnapshot.SIZE, title);
        String[] slots = snapshot.slots();
        for (int index = 0; index < slots.length; index++) {
            ItemStack item = codec.decode(slots[index]);
            inventory.setItem(index, item);
        }
        return inventory;
    }

    public static BackpackSnapshot snapshot(Inventory inventory, ItemStackCodec codec) {
        String[] slots = new String[BackpackSnapshot.SIZE];
        for (int index = 0; index < BackpackSnapshot.SIZE; index++) {
            slots[index] = codec.encode(inventory.getItem(index));
        }
        return new BackpackSnapshot(slots);
    }
}
