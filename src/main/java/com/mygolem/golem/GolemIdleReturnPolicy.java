package com.mygolem.golem;

import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;

final class GolemIdleReturnPolicy {

    enum Action {
        STOP,
        MOVE,
        TELEPORT
    }

    private GolemIdleReturnPolicy() {
    }

    static Action action(String currentWorld, String anchorWorld, double distance, double actionDistance, int radius) {
        if (currentWorld == null || anchorWorld == null || !currentWorld.equals(anchorWorld)) {
            return Action.TELEPORT;
        }
        if (distance <= actionDistance) {
            return Action.STOP;
        }
        double teleportDistance = Math.max(radius * 2.0D, actionDistance * 4.0D);
        return distance > teleportDistance ? Action.TELEPORT : Action.MOVE;
    }

    static GolemRecord withCenterAndIdleLocation(GolemRecord record, StoredLocation center) {
        StoredLocation idle = idleStoredLocation(center);
        return record.withCenter(center).withLocation(idle);
    }

    static StoredLocation idleStoredLocation(StoredLocation center) {
        if (center == null) {
            return null;
        }
        return new StoredLocation(
                center.world(),
                center.blockX() + 0.5D,
                center.y(),
                center.blockZ() + 0.5D,
                center.yaw(),
                center.pitch()
        );
    }
}
