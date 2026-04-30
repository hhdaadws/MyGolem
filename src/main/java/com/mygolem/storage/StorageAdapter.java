package com.mygolem.storage;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public interface StorageAdapter {

    Optional<StorageSlot> findFirstCustomItem(List<String> priority, Function<ItemStack, String> idResolver);

    Optional<StorageSlot> findFirstMatching(Predicate<ItemStack> matcher);

    boolean removeOne(StorageSlot slot);

    void update(StorageSlot slot, ItemStack itemStack);

    List<ItemStack> addItems(Collection<ItemStack> items);

    boolean owns(StorageSlot slot);
}
