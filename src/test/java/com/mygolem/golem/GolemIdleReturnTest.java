package com.mygolem.golem;

import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;
import com.mygolem.storage.BackpackSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GolemIdleReturnTest {

    @Test
    void stopsWhenAlreadyInsideIdleAnchorRange() {
        assertEquals(
                GolemIdleReturnPolicy.Action.STOP,
                GolemIdleReturnPolicy.action("world", "world", 2.0D, 2.5D, 6)
        );
    }

    @Test
    void movesBackWhenIdleAnchorIsNearby() {
        assertEquals(
                GolemIdleReturnPolicy.Action.MOVE,
                GolemIdleReturnPolicy.action("world", "world", 8.0D, 2.5D, 6)
        );
    }

    @Test
    void teleportsBackWhenIdleAnchorIsTooFarOrInAnotherWorld() {
        assertEquals(
                GolemIdleReturnPolicy.Action.TELEPORT,
                GolemIdleReturnPolicy.action("world", "world", 13.0D, 2.5D, 6)
        );
        assertEquals(
                GolemIdleReturnPolicy.Action.TELEPORT,
                GolemIdleReturnPolicy.action("world", "nether", 1.0D, 2.5D, 6)
        );
    }

    @Test
    void settingCenterAlsoUpdatesRespawnLocationToBlockCenter() {
        GolemRecord record = new GolemRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                new StoredLocation("world", 1.5D, 64.0D, 1.5D, 0.0F, 0.0F),
                new StoredLocation("world", 1.0D, 64.0D, 1.0D, 0.0F, 0.0F),
                null,
                false,
                BackpackSnapshot.empty()
        );
        StoredLocation center = new StoredLocation("world", 20.0D, 65.0D, -4.0D, 90.0F, 5.0F);

        GolemRecord updated = GolemIdleReturnPolicy.withCenterAndIdleLocation(record, center);

        assertEquals(center, updated.center());
        assertEquals(new StoredLocation("world", 20.5D, 65.0D, -3.5D, 90.0F, 5.0F), updated.location());
    }
}
