package com.mygolem.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class StackTransferPlan {

    private StackTransferPlan() {
    }

    public static boolean hasAvailableSpace(List<Stack> contents, int maxSlots) {
        if (maxSlots <= 0) {
            return false;
        }
        for (int index = 0; index < Math.min(contents.size(), maxSlots); index++) {
            Stack stack = contents.get(index);
            if (stack.amount() < stack.maxStackSize()) {
                return true;
            }
        }
        return contents.size() < maxSlots;
    }

    public static Result addItems(List<Stack> contents, int maxSlots, Collection<Stack> incoming) {
        List<Stack> planned = new ArrayList<>(contents == null ? List.of() : contents);
        List<Stack> leftovers = new ArrayList<>();
        for (Stack stack : incoming == null ? List.<Stack>of() : incoming) {
            Stack remaining = stack;
            remaining = merge(planned, maxSlots, remaining);
            remaining = fillEmptySlots(planned, maxSlots, remaining);
            if (remaining.amount() > 0) {
                leftovers.add(remaining);
            }
        }
        return new Result(planned, leftovers);
    }

    private static Stack merge(List<Stack> planned, int maxSlots, Stack remaining) {
        for (int index = 0; index < Math.min(planned.size(), maxSlots) && remaining.amount() > 0; index++) {
            Stack current = planned.get(index);
            if (!current.key().equals(remaining.key()) || current.amount() >= current.maxStackSize()) {
                continue;
            }
            int moved = Math.min(current.maxStackSize() - current.amount(), remaining.amount());
            planned.set(index, new Stack(current.key(), current.amount() + moved, current.maxStackSize()));
            remaining = new Stack(remaining.key(), remaining.amount() - moved, remaining.maxStackSize());
        }
        return remaining;
    }

    private static Stack fillEmptySlots(List<Stack> planned, int maxSlots, Stack remaining) {
        while (planned.size() < maxSlots && remaining.amount() > 0) {
            int moved = Math.min(remaining.maxStackSize(), remaining.amount());
            planned.add(new Stack(remaining.key(), moved, remaining.maxStackSize()));
            remaining = new Stack(remaining.key(), remaining.amount() - moved, remaining.maxStackSize());
        }
        return remaining;
    }

    public record Stack(String key, int amount, int maxStackSize) {
    }

    public record Result(List<Stack> contents, List<Stack> leftovers) {
    }
}
