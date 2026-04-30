package com.mygolem.storage;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class InventoryStacks {

    private InventoryStacks() {
    }

    public static boolean hasAvailableSpace(ItemStack[] contents, int maxSlots) {
        int limit = limit(contents, maxSlots);
        for (int index = 0; index < limit; index++) {
            ItemStack item = contents[index];
            if (isEmpty(item) || item.getAmount() < item.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> addItems(ItemStack[] contents, int maxSlots, Collection<ItemStack> incoming) {
        int limit = limit(contents, maxSlots);
        List<ItemStack> leftovers = new ArrayList<>();
        if (incoming == null || incoming.isEmpty()) {
            return leftovers;
        }
        for (ItemStack item : incoming) {
            if (isEmpty(item)) {
                continue;
            }
            ItemStack remaining = item.clone();
            mergeIntoExistingStacks(contents, limit, remaining);
            splitIntoEmptySlots(contents, limit, remaining);
            if (!isEmpty(remaining)) {
                leftovers.add(remaining);
            }
        }
        return leftovers;
    }

    public static List<ItemStack> storedItems(ItemStack[] contents, int maxSlots) {
        int limit = limit(contents, maxSlots);
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < limit; index++) {
            ItemStack item = contents[index];
            if (!isEmpty(item)) {
                items.add(item.clone());
            }
        }
        return items;
    }

    private static void mergeIntoExistingStacks(ItemStack[] contents, int limit, ItemStack remaining) {
        for (int index = 0; index < limit && !isEmpty(remaining); index++) {
            ItemStack current = contents[index];
            if (isEmpty(current) || !current.isSimilar(remaining)) {
                continue;
            }
            int space = current.getMaxStackSize() - current.getAmount();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining.getAmount());
            current.setAmount(current.getAmount() + moved);
            remaining.setAmount(remaining.getAmount() - moved);
        }
    }

    private static void splitIntoEmptySlots(ItemStack[] contents, int limit, ItemStack remaining) {
        for (int index = 0; index < limit && !isEmpty(remaining); index++) {
            if (!isEmpty(contents[index])) {
                continue;
            }
            ItemStack placed = remaining.clone();
            int moved = Math.min(placed.getMaxStackSize(), remaining.getAmount());
            placed.setAmount(moved);
            contents[index] = placed;
            remaining.setAmount(remaining.getAmount() - moved);
        }
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private static int limit(ItemStack[] contents, int maxSlots) {
        if (contents == null || maxSlots <= 0) {
            return 0;
        }
        return Math.min(maxSlots, contents.length);
    }
}
