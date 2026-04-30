package com.mygolem.chunk;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkAreaCalculatorTest {

    @Test
    void returnsUniqueChunksCoveredByRadius() {
        Set<ChunkCoord> chunks = ChunkAreaCalculator.coveredChunks(0, 0, 20);

        assertEquals(16, chunks.size());
        assertTrue(chunks.contains(new ChunkCoord(-2, -2)));
        assertTrue(chunks.contains(new ChunkCoord(1, 1)));
    }

    @Test
    void enforcesChunkLimit() {
        Set<ChunkCoord> chunks = ChunkAreaCalculator.limitedChunks(0, 0, 20, 9);

        assertEquals(0, chunks.size());
    }
}
