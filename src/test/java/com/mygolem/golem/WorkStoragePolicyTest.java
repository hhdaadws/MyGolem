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
}
