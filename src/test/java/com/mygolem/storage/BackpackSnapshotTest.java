package com.mygolem.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void corruptBase64InOneSlotIsResetToNullWithoutFailingOtherSlots() {
        BackpackSnapshot good = new BackpackSnapshot(new String[]{
                "seed:tomato", null, "crop:wheat", null, null, null, null, null, null
        });
        String raw = good.serialize();
        String[] parts = raw.split(";", -1);
        parts[2] = "v!!not-base64!!";
        String corrupted = String.join(";", parts);

        BackpackSnapshot restored = BackpackSnapshot.deserialize(corrupted);

        assertEquals(9, restored.slots().length);
        assertEquals("seed:tomato", restored.slots()[0]);
        assertNull(restored.slots()[2]);
    }
}
