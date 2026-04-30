package com.mygolem.golem;

public final class WorkStoragePolicy {

    public static final int LOW_SPACE_THRESHOLD = 2;

    private WorkStoragePolicy() {
    }

    public static Action actionFor(int emptySlots, boolean chestBindable) {
        if (emptySlots <= 0) {
            return Action.UNLOAD_BACKPACK_TO_CHEST;
        }
        if (chestBindable && emptySlots < LOW_SPACE_THRESHOLD) {
            return Action.UNLOAD_BACKPACK_TO_CHEST;
        }
        return Action.FARM_WITH_BACKPACK;
    }

    public static Action actionFor(boolean backpackHasSpace) {
        return actionFor(backpackHasSpace ? Integer.MAX_VALUE : 0, false);
    }

    public enum Action {
        FARM_WITH_BACKPACK,
        UNLOAD_BACKPACK_TO_CHEST
    }
}
