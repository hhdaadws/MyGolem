package com.mygolem.chunk;

import com.mygolem.model.GolemRecord;
import com.mygolem.model.StoredLocation;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkTicketManager {

    private final Plugin plugin;
    private final Map<UUID, Set<ChunkCoord>> tickets = new ConcurrentHashMap<>();

    public ChunkTicketManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean acquire(GolemRecord record, int radius, int maxChunks) {
        StoredLocation center = record.center();
        World world = Bukkit.getWorld(center.world());
        if (world == null) {
            return false;
        }
        Set<ChunkCoord> chunks = ChunkAreaCalculator.limitedChunks(center.blockX(), center.blockZ(), radius, maxChunks);
        if (chunks.isEmpty()) {
            return false;
        }
        release(record.id(), center.world());
        for (ChunkCoord chunk : chunks) {
            world.addPluginChunkTicket(chunk.x(), chunk.z(), plugin);
        }
        tickets.put(record.id(), new HashSet<>(chunks));
        return true;
    }

    public void release(UUID golemId, String worldName) {
        Set<ChunkCoord> chunks = tickets.remove(golemId);
        World world = Bukkit.getWorld(worldName);
        if (chunks == null || world == null) {
            return;
        }
        for (ChunkCoord chunk : chunks) {
            world.removePluginChunkTicket(chunk.x(), chunk.z(), plugin);
        }
    }

    public void releaseAll(String worldName) {
        for (UUID golemId : Set.copyOf(tickets.keySet())) {
            release(golemId, worldName);
        }
        tickets.clear();
    }
}
