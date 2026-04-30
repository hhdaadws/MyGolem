package com.mygolem.listener;

import com.mygolem.config.MyGolemConfig;
import com.mygolem.golem.BackpackHolder;
import com.mygolem.golem.GolemManager;
import com.mygolem.golem.MenuHolder;
import com.mygolem.model.GolemRecord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class MenuListener implements Listener {

    private final MyGolemConfig config;
    private final GolemManager manager;

    public MenuListener(MyGolemConfig config, GolemManager manager) {
        this.config = config;
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        UUID golemId = holder.golemId();
        GolemRecord record = manager.get(golemId).orElse(null);
        if (record == null || !record.owner().equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        switch (event.getRawSlot()) {
            case 0 -> {
                if (record.active()) {
                    manager.stop(golemId, true);
                    player.sendMessage(config.message("傀儡已停止。"));
                } else if (manager.start(golemId, player)) {
                    player.sendMessage(config.message("傀儡已启动。"));
                }
                Inventory refreshed = manager.createMenu(golemId);
                if (refreshed != null) {
                    player.openInventory(refreshed);
                } else {
                    player.closeInventory();
                }
            }
            case 2 -> {
                Inventory backpack = manager.createBackpackInventory(golemId);
                if (backpack != null) {
                    player.openInventory(backpack);
                } else {
                    player.closeInventory();
                }
            }
            case 4 -> {
                Location location = player.getLocation().getBlock().getLocation();
                manager.setCenter(golemId, location);
                player.sendMessage(config.message("已把当前位置设为工作中心。"));
            }
            case 6 -> {
                manager.bindChest(golemId, null);
                player.sendMessage(config.message("已解绑箱子。"));
            }
            case 8 -> {
                manager.recall(golemId);
                player.closeInventory();
                player.sendMessage(config.message("已收回傀儡，再次右键地面可召唤。"));
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof BackpackHolder holder) {
            manager.saveBackpack(holder.golemId(), inventory);
        }
    }
}
