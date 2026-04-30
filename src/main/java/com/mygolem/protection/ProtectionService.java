package com.mygolem.protection;

import com.mygolem.config.MyGolemConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

public class ProtectionService {

    private final Plugin plugin;
    private final MyGolemConfig config;

    public ProtectionService(Plugin plugin, MyGolemConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean canManage(Player player, java.util.UUID owner) {
        return player.getUniqueId().equals(owner) || player.hasPermission("mygolem.admin");
    }

    public boolean canWorkAt(Player owner, Location location) {
        if (owner == null || location == null || owner.hasPermission("mygolem.protection.bypass")) {
            return true;
        }
        boolean eventAllowed = !config.eventProtectionEnabled() || blockBreakEventAllows(owner, location.getBlock());
        return ProtectionDecision.fromChecks(eventAllowed).allowed();
    }

    private boolean blockBreakEventAllows(Player player, Block block) {
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        plugin.getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
