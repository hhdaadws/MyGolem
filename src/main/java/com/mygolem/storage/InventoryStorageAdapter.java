package com.mygolem.storage;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class InventoryStorageAdapter implements StorageAdapter {

    private final Inventory inventory;
    private final int maxSlots;
    private final Runnable onChange;

    public InventoryStorageAdapter(Inventory inventory, int maxSlots, Runnable onChange) {
        this.inventory = inventory;
        this.maxSlots = Math.min(maxSlots, inventory.getSize());
        this.onChange = onChange;
    }

    @Override
    public Optional<StorageSlot> findFirstCustomItem(List<String> priority, Function<ItemStack, String> idResolver) {
        List<String> normalized = priority == null ? List.of() : priority;
        return slots().stream()
                .filter(slot -> normalized.contains(resolve(slot, idResolver)))
                .min(Comparator.comparingInt(slot -> normalized.indexOf(resolve(slot, idResolver))));
    }

    @Override
    public Optional<StorageSlot> findFirstMatching(Predicate<ItemStack> matcher) {
        return slots().stream().filter(slot -> matcher.test(slot.itemStack())).findFirst();
    }

    @Override
    public boolean removeOne(StorageSlot slot) {
        if (!owns(slot)) {
            return false;
        }
        ItemStack current = inventory.getItem(slot.index());
        if (current == null || current.getType().isAir()) {
            return false;
        }
        current.setAmount(current.getAmount() - 1);
        if (current.getAmount() <= 0) {
            inventory.setItem(slot.index(), null);
        } else {
            inventory.setItem(slot.index(), current);
        }
        onChange.run();
        return true;
    }

    @Override
    public void update(StorageSlot slot, ItemStack itemStack) {
        if (!owns(slot)) {
            return;
        }
        inventory.setItem(slot.index(), itemStack);
        onChange.run();
    }

    @Override
    public List<ItemStack> addItems(Collection<ItemStack> items) {
        ItemStack[] contents = inventory.getContents();
        List<ItemStack> leftovers = InventoryStacks.addItems(contents, maxSlots, items);
        for (int index = 0; index < maxSlots; index++) {
            inventory.setItem(index, contents[index]);
        }
        onChange.run();
        return leftovers;
    }

    public boolean hasAvailableSpace() {
        return InventoryStacks.hasAvailableSpace(inventory.getContents(), maxSlots);
    }

    public List<ItemStack> storedItems() {
        return InventoryStacks.storedItems(inventory.getContents(), maxSlots);
    }

    public List<ItemStack> transferAllTo(InventoryStorageAdapter destination) {
        List<ItemStack> moving = storedItems();
        if (moving.isEmpty()) {
            return List.of();
        }
        List<ItemStack> leftovers = destination.addItems(moving);
        replaceContents(leftovers);
        return leftovers;
    }

    public List<ItemStack> replaceContents(Collection<ItemStack> items) {
        for (int index = 0; index < maxSlots; index++) {
            inventory.setItem(index, null);
        }
        return addItems(items);
    }

    @Override
    public boolean owns(StorageSlot slot) {
        return slot != null && slot.owner() == this;
    }

    private List<StorageSlot> slots() {
        List<StorageSlot> slots = new ArrayList<>();
        for (int index = 0; index < maxSlots; index++) {
            ItemStack item = inventory.getItem(index);
            if (item != null && !item.getType().isAir()) {
                slots.add(new StorageSlot(this, index, item));
            }
        }
        return slots;
    }

    private String resolve(StorageSlot slot, Function<ItemStack, String> idResolver) {
        String id = idResolver.apply(slot.itemStack());
        return id == null ? "" : id.toLowerCase();
    }
}
