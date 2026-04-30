package com.mygolem.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackpackSnapshotTest {

    @Test
    void serializesExactlyNineSlotsWithoutDroppingContents() {
        BackpackSnapshot snapshot = new BackpackSnapshot(new String[]{
                "seed:tomato", null, "crop:wheat", "", "tool:water", null, null, "rare:item", "stone"
        });

        BackpackSnapshot restored = BackpackSnapshot.deserialize(snapshot.serialize());

        assertEquals(9, restored.slots().length);
        assertArrayEquals(snapshot.slots(), restored.slots());
    }

    @Test
    void rejectsNonNineSlotBackpacks() {
        assertThrows(IllegalArgumentException.class, () -> new BackpackSnapshot(new String[]{"too-small"}));
    }
}
