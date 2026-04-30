package com.mygolem.chunk;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ChunkAreaCalculator {

    private ChunkAreaCalculator() {
    }

    public static Set<ChunkCoord> coveredChunks(int centerBlockX, int centerBlockZ, int radius) {
        int minX = Math.floorDiv(centerBlockX - radius, 16);
        int maxX = Math.floorDiv(centerBlockX + radius, 16);
        int minZ = Math.floorDiv(centerBlockZ - radius, 16);
        int maxZ = Math.floorDiv(centerBlockZ + radius, 16);
        Set<ChunkCoord> chunks = new LinkedHashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                chunks.add(new ChunkCoord(x, z));
            }
        }
        return chunks;
    }

    public static Set<ChunkCoord> limitedChunks(int centerBlockX, int centerBlockZ, int radius, int maxChunks) {
        Set<ChunkCoord> chunks = coveredChunks(centerBlockX, centerBlockZ, radius);
        return chunks.size() > maxChunks ? Set.of() : chunks;
    }
}
