package com.mygolem.golem;

import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;
import com.mygolem.storage.BackpackSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemRecallPolicyTest {

    @Test
    void recallKeepsDurableStateAndClearsRuntimeEntity() {
        GolemRecord record = recordWithBackpack();

        GolemRecord recalled = GolemRecallPolicy.recall(record);

        assertEquals(record.id(), recalled.id());
        assertEquals(record.owner(), recalled.owner());
        assertEquals(record.location(), recalled.location());
        assertEquals(record.center(), recalled.center());
        assertEquals(record.chest(), recalled.chest());
        assertArrayEquals(record.backpack().slots(), recalled.backpack().slots());
        assertFalse(recalled.active());
        assertNull(recalled.entityUuid());
    }

    @Test
    void respawnUpdatesOnlyRuntimeEntityAndBodyLocation() {
        GolemRecord recalled = GolemRecallPolicy.recall(recordWithBackpack());
        StoredLocation newLocation = new StoredLocation("world", 30.5D, 70.0D, -9.5D, 180.0F, 0.0F);
        UUID newEntity = UUID.randomUUID();

        GolemRecord respawned = GolemRecallPolicy.respawn(recalled, newLocation, newEntity);

        assertEquals(recalled.id(), respawned.id());
        assertEquals(recalled.owner(), respawned.owner());
        assertEquals(newEntity, respawned.entityUuid());
        assertEquals(newLocation, respawned.location());
        assertEquals(recalled.center(), respawned.center());
        assertEquals(recalled.chest(), respawned.chest());
        assertArrayEquals(recalled.backpack().slots(), respawned.backpack().slots());
        assertFalse(respawned.active());
    }

    @Test
    void recalledRecordsDoNotSpawnOnPluginLoadUntilPlayerSummonsThem() {
        GolemRecord record = recordWithBackpack();
        GolemRecord recalled = GolemRecallPolicy.recall(record);

        assertTrue(GolemRecallPolicy.shouldSpawnOnLoad(record));
        assertFalse(GolemRecallPolicy.shouldSpawnOnLoad(recalled));
    }

    private static GolemRecord recordWithBackpack() {
        return new GolemRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new StoredLocation("world", 10.5D, 64.0D, -2.5D, 90.0F, 0.0F),
                new StoredLocation("world", 8.0D, 63.0D, -1.0D, 0.0F, 0.0F),
                new StoredLocation("world", 12.0D, 63.0D, -1.0D, 0.0F, 0.0F),
                true,
                new BackpackSnapshot(new String[]{
                        "seed:one", null, null, null, "tool:hoe", null, null, null, "crop:two"
                })
        );
    }
}
