package com.mygolem.model;

import com.mygolem.storage.BackpackSnapshot;

import java.util.UUID;

public record GolemRecord(
        UUID id,
        UUID owner,
        UUID entityUuid,
        StoredLocation location,
        StoredLocation center,
        StoredLocation chest,
        boolean active,
        BackpackSnapshot backpack
) {

    public GolemRecord withEntityUuid(UUID uuid) {
        return new GolemRecord(id, owner, uuid, location, center, chest, active, backpack);
    }

    public GolemRecord withLocation(StoredLocation newLocation) {
        return new GolemRecord(id, owner, entityUuid, newLocation, center, chest, active, backpack);
    }

    public GolemRecord withCenter(StoredLocation newCenter) {
        return new GolemRecord(id, owner, entityUuid, location, newCenter, chest, active, backpack);
    }

    public GolemRecord withChest(StoredLocation newChest) {
        return new GolemRecord(id, owner, entityUuid, location, center, newChest, active, backpack);
    }

    public GolemRecord withActive(boolean newActive) {
        return new GolemRecord(id, owner, entityUuid, location, center, chest, newActive, backpack);
    }

    public GolemRecord withBackpack(BackpackSnapshot newBackpack) {
        return new GolemRecord(id, owner, entityUuid, location, center, chest, active, newBackpack);
    }
}
