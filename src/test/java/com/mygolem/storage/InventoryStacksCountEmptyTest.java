package com.mygolem.storage;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryStacksCountEmptyTest {

    @Test
    void nullContentsReturnsZero() {
        assertEquals(0, InventoryStacks.countEmptySlots(null, 9));
    }

    @Test
    void emptyArrayReturnsZero() {
        assertEquals(0, InventoryStacks.countEmptySlots(new ItemStack[0], 9));
    }

    @Test
    void allNullSlotsCountAsEmpty() {
        ItemStack[] contents = new ItemStack[9];
        assertEquals(9, InventoryStacks.countEmptySlots(contents, 9));
    }

    @Test
    void limitClampedToMaxSlots() {
        ItemStack[] contents = new ItemStack[16];
        assertEquals(9, InventoryStacks.countEmptySlots(contents, 9));
    }

    @Test
    void zeroOrNegativeMaxSlotsReturnsZero() {
        ItemStack[] contents = new ItemStack[9];
        assertEquals(0, InventoryStacks.countEmptySlots(contents, 0));
        assertEquals(0, InventoryStacks.countEmptySlots(contents, -1));
    }
}
