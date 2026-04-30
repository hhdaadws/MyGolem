package com.mygolem.storage;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class CompositeStorageAdapter implements StorageAdapter {

    private final List<StorageAdapter> delegates;

    public CompositeStorageAdapter(List<StorageAdapter> delegates) {
        this.delegates = delegates == null ? List.of() : delegates.stream().filter(item -> item != null).toList();
    }

    @Override
    public Optional<StorageSlot> findFirstCustomItem(List<String> priority, Function<ItemStack, String> idResolver) {
        for (StorageAdapter delegate : delegates) {
            Optional<StorageSlot> found = delegate.findFirstCustomItem(priority, idResolver);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<StorageSlot> findFirstMatching(Predicate<ItemStack> matcher) {
        for (StorageAdapter delegate : delegates) {
            Optional<StorageSlot> found = delegate.findFirstMatching(matcher);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean removeOne(StorageSlot slot) {
        for (StorageAdapter delegate : delegates) {
            if (delegate.owns(slot)) {
                return delegate.removeOne(slot);
            }
        }
        return false;
    }

    @Override
    public void update(StorageSlot slot, ItemStack itemStack) {
        for (StorageAdapter delegate : delegates) {
            if (delegate.owns(slot)) {
                delegate.update(slot, itemStack);
                return;
            }
        }
    }

    @Override
    public List<ItemStack> addItems(Collection<ItemStack> items) {
        List<ItemStack> leftovers = new ArrayList<>(items);
        for (StorageAdapter delegate : delegates) {
            if (leftovers.isEmpty()) {
                break;
            }
            leftovers = delegate.addItems(leftovers);
        }
        return leftovers;
    }

    @Override
    public boolean owns(StorageSlot slot) {
        return delegates.stream().anyMatch(delegate -> delegate.owns(slot));
    }
}
