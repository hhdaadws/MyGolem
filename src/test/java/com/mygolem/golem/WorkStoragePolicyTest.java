package com.mygolem.golem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkStoragePolicyTest {

    @Test
    void usesBackpackWhenBackpackStillHasSpace() {
        assertEquals(
                WorkStoragePolicy.Action.FARM_WITH_BACKPACK,
                WorkStoragePolicy.actionFor(true)
        );
    }

    @Test
    void movesToChestUnloadWhenBackpackIsFull() {
        assertEquals(
                WorkStoragePolicy.Action.UNLOAD_BACKPACK_TO_CHEST,
                WorkStoragePolicy.actionFor(false)
        );
    }

    @Test
    void unloadsWhenEmptySlotsBelowThresholdAndChestBound() {
        assertEquals(
                WorkStoragePolicy.Action.UNLOAD_BACKPACK_TO_CHEST,
                WorkStoragePolicy.actionFor(1, true)
        );
    }

    @Test
    void keepsFarmingWhenEmptySlotsBelowThresholdButNoChest() {
        assertEquals(
                WorkStoragePolicy.Action.FARM_WITH_BACKPACK,
                WorkStoragePolicy.actionFor(1, false)
        );
    }

    @Test
    void unloadsWhenZeroEmptySlotsRegardlessOfChest() {
        assertEquals(
                WorkStoragePolicy.Action.UNLOAD_BACKPACK_TO_CHEST,
                WorkStoragePolicy.actionFor(0, false)
        );
        assertEquals(
                WorkStoragePolicy.Action.UNLOAD_BACKPACK_TO_CHEST,
                WorkStoragePolicy.actionFor(0, true)
        );
    }

    @Test
    void keepsFarmingAtOrAboveThreshold() {
        assertEquals(
                WorkStoragePolicy.Action.FARM_WITH_BACKPACK,
                WorkStoragePolicy.actionFor(WorkStoragePolicy.LOW_SPACE_THRESHOLD, true)
        );
        assertEquals(
                WorkStoragePolicy.Action.FARM_WITH_BACKPACK,
                WorkStoragePolicy.actionFor(WorkStoragePolicy.LOW_SPACE_THRESHOLD + 1, true)
        );
    }
}
