package com.mygolem.storage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public record BackpackSnapshot(String[] slots) {

    public static final int SIZE = 9;

    public BackpackSnapshot {
        if (slots == null || slots.length != SIZE) {
            throw new IllegalArgumentException("Golem backpack must contain exactly 9 slots.");
        }
        slots = Arrays.copyOf(slots, SIZE);
    }

    public static BackpackSnapshot empty() {
        return new BackpackSnapshot(new String[SIZE]);
    }

    public String serialize() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < slots.length; index++) {
            if (index > 0) {
                builder.append(';');
            }
            String value = slots[index];
            if (value != null) {
                builder.append('v');
                builder.append(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
            }
        }
        return builder.toString();
    }

    public static BackpackSnapshot deserialize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return empty();
        }
        String[] parts = raw.split(";", -1);
        if (parts.length != SIZE) {
            throw new IllegalArgumentException("Stored golem backpack does not contain 9 slots.");
        }
        String[] slots = new String[SIZE];
        for (int index = 0; index < parts.length; index++) {
            if (parts[index].startsWith("v")) {
                slots[index] = new String(Base64.getDecoder().decode(parts[index].substring(1)), StandardCharsets.UTF_8);
            }
        }
        return new BackpackSnapshot(slots);
    }
}
