package com.mygolem.golem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkStoragePolicyTest {

    @Test
    void harvestUsesBackpackWhenBackpackStillHasSpace() {
        assertEquals(
                WorkStoragePolicy.Action.FARM_WITH_BACKPACK,
                WorkStoragePolicy.actionFor(WorkTarget.Type.HARVEST, true)
        );
    }

    @Test
    void harvestMovesToChestUnloadBeforeWorkingWhenBackpackIsFull() {
        assertEquals(
                WorkStoragePolicy.Action.UNLOAD_BACKPACK_TO_CHEST,
                WorkStoragePolicy.actionFor(WorkTarget.Type.HARVEST, false)
        );
    }

    @Test
    void plantingStillUsesBackpackOnlyWhenBackpackIsFull() {
        assertEquals(
                WorkStoragePolicy.Action.FARM_WITH_BACKPACK,
                WorkStoragePolicy.actionFor(WorkTarget.Type.PLANT, false)
        );
    }
}
