package com.mygolem.storage;

import com.mygolem.golem.GolemRecallPolicy;
import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void reloadsSavedGolemWithBackpackAndBindings() throws Exception {
        Path db = tempDir.resolve("mygolem.db");
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        BackpackSnapshot backpack = new BackpackSnapshot(new String[]{
                "seed:one", null, null, null, null, null, null, null, "crop:two"
        });
        GolemRecord record = new GolemRecord(
                id,
                owner,
                null,
                new StoredLocation("world", 10.5, 64, -2.5, 90, 0),
                new StoredLocation("world", 8, 63, -1, 0, 0),
                new StoredLocation("world", 12, 63, -1, 0, 0),
                true,
                backpack
        );

        try (GolemRepository repository = GolemRepository.open(db)) {
            repository.save(record);
        }

        try (GolemRepository repository = GolemRepository.open(db)) {
            List<GolemRecord> loaded = repository.loadAll();
            assertEquals(1, loaded.size());
            GolemRecord restored = loaded.get(0);
            assertEquals(record.id(), restored.id());
            assertEquals(record.owner(), restored.owner());
            assertEquals(record.center(), restored.center());
            assertEquals(record.chest(), restored.chest());
            assertTrue(restored.active());
            assertEquals("seed:one", restored.backpack().slots()[0]);
            assertEquals("crop:two", restored.backpack().slots()[8]);
        }
    }

    @Test
    void keepsBackpackAcrossRecallAndRespawnPersistence() throws Exception {
        Path db = tempDir.resolve("mygolem-recall.db");
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        GolemRecord record = new GolemRecord(
                id,
                owner,
                UUID.randomUUID(),
                new StoredLocation("world", 10.5, 64, -2.5, 90, 0),
                new StoredLocation("world", 8, 63, -1, 0, 0),
                new StoredLocation("world", 12, 63, -1, 0, 0),
                true,
                new BackpackSnapshot(new String[]{
                        "seed:one", null, null, null, "tool:hoe", null, null, null, "crop:two"
                })
        );

        try (GolemRepository repository = GolemRepository.open(db)) {
            repository.save(record);
            repository.save(GolemRecallPolicy.recall(record));
        }

        GolemRecord recalled;
        try (GolemRepository repository = GolemRepository.open(db)) {
            recalled = repository.loadAll().get(0);
            assertEquals("seed:one", recalled.backpack().slots()[0]);
            assertEquals("tool:hoe", recalled.backpack().slots()[4]);
            assertEquals("crop:two", recalled.backpack().slots()[8]);
        }

        StoredLocation respawnLocation = new StoredLocation("world", 30.5, 70, -9.5, 180, 0);
        UUID entityUuid = UUID.randomUUID();
        try (GolemRepository repository = GolemRepository.open(db)) {
            repository.save(GolemRecallPolicy.respawn(recalled, respawnLocation, entityUuid));
        }

        try (GolemRepository repository = GolemRepository.open(db)) {
            GolemRecord respawned = repository.loadAll().get(0);
            assertEquals(entityUuid, respawned.entityUuid());
            assertEquals(respawnLocation, respawned.location());
            assertEquals(record.center(), respawned.center());
            assertEquals(record.chest(), respawned.chest());
            assertEquals("seed:one", respawned.backpack().slots()[0]);
            assertEquals("tool:hoe", respawned.backpack().slots()[4]);
            assertEquals("crop:two", respawned.backpack().slots()[8]);
        }
    }
}
