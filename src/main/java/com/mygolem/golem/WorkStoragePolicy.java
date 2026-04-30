package com.mygolem.golem;

public final class WorkStoragePolicy {

    private WorkStoragePolicy() {
    }

    public static Action actionFor(WorkTarget.Type targetType, boolean backpackHasSpace) {
        if (targetType == WorkTarget.Type.HARVEST && !backpackHasSpace) {
            return Action.UNLOAD_BACKPACK_TO_CHEST;
        }
        return Action.FARM_WITH_BACKPACK;
    }

    public enum Action {
        FARM_WITH_BACKPACK,
        UNLOAD_BACKPACK_TO_CHEST
    }
}
