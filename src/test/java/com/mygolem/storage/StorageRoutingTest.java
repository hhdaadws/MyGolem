package com.mygolem.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageRoutingTest {

    @Test
    void routesDropsToBackpackBeforeChest() {
        CountingStringStorage backpack = new CountingStringStorage(4);
        CountingStringStorage chest = new CountingStringStorage(10);
        CompositeStringStorage storage = new CompositeStringStorage(List.of(backpack, chest));

        List<String> leftovers = storage.addItems(List.of("a", "b", "c"));

        assertTrue(leftovers.isEmpty());
        assertEquals(List.of("a", "b", "c"), backpack.items);
        assertTrue(chest.items.isEmpty());
    }

    @Test
    void overflowsDropsIntoChestAndReturnsRealLeftovers() {
        CountingStringStorage backpack = new CountingStringStorage(1);
        CountingStringStorage chest = new CountingStringStorage(2);
        CompositeStringStorage storage = new CompositeStringStorage(List.of(backpack, chest));

        List<String> leftovers = storage.addItems(List.of("a", "b", "c", "d"));

        assertEquals(List.of("a"), backpack.items);
        assertEquals(List.of("b", "c"), chest.items);
        assertEquals(List.of("d"), leftovers);
    }

    private static final class CountingStringStorage implements StringStorage {
        private final int capacity;
        private final List<String> items = new ArrayList<>();

        private CountingStringStorage(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public List<String> addItems(Collection<String> incoming) {
            List<String> leftovers = new ArrayList<>();
            for (String item : incoming) {
                if (items.size() < capacity) {
                    items.add(item);
                } else {
                    leftovers.add(item);
                }
            }
            return leftovers;
        }
    }
}
