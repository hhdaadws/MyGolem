package com.mygolem.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StackTransferPlanTest {

    @Test
    void addsItemsIntoEmptySlotsWithoutReportingChestFull() {
        List<StackTransferPlan.Stack> contents = new ArrayList<>();

        StackTransferPlan.Result result = StackTransferPlan.addItems(contents, 9, List.of(stack("wheat", 32, 64)));

        assertTrue(result.leftovers().isEmpty());
        assertEquals(32, result.contents().get(0).amount());
    }

    @Test
    void mergesSimilarItemsBeforeReportingLeftovers() {
        List<StackTransferPlan.Stack> contents = new ArrayList<>(List.of(stack("wheat", 40, 64)));

        StackTransferPlan.Result result = StackTransferPlan.addItems(contents, 9, List.of(stack("wheat", 20, 64)));

        assertTrue(result.leftovers().isEmpty());
        assertEquals(60, result.contents().get(0).amount());
    }

    @Test
    void returnsOnlyRealLeftoversWhenNoSlotCanAcceptItems() {
        List<StackTransferPlan.Stack> contents = new ArrayList<>(List.of(stack("wheat", 64, 64)));

        StackTransferPlan.Result result = StackTransferPlan.addItems(contents, 1, List.of(stack("carrot", 3, 64)));

        assertEquals(List.of(stack("carrot", 3, 64)), result.leftovers());
    }

    @Test
    void detectsAnyEmptyOrMergeableBackpackSpace() {
        assertFalse(StackTransferPlan.hasAvailableSpace(List.of(stack("wheat", 64, 64)), 1));
        assertTrue(StackTransferPlan.hasAvailableSpace(List.of(stack("wheat", 63, 64)), 1));
        assertTrue(StackTransferPlan.hasAvailableSpace(List.of(), 1));
    }

    private static StackTransferPlan.Stack stack(String key, int amount, int maxStackSize) {
        return new StackTransferPlan.Stack(key, amount, maxStackSize);
    }
}
