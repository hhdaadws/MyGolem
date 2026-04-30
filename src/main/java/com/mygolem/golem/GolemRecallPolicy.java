package com.mygolem.golem;

import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;

import java.util.UUID;

public final class GolemRecallPolicy {

    private GolemRecallPolicy() {
    }

    public static GolemRecord recall(GolemRecord record) {
        return record.withActive(false).withEntityUuid(null);
    }

    public static GolemRecord respawn(GolemRecord record, StoredLocation location, UUID entityUuid) {
        return record.withLocation(location).withEntityUuid(entityUuid).withActive(false);
    }

    public static boolean shouldSpawnOnLoad(GolemRecord record) {
        return record.entityUuid() != null;
    }
}
