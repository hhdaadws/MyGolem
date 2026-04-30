package com.mygolem.listener;

import com.mygolem.config.MyGolemConfig;
import com.mygolem.controller.ControllerItem;
import com.mygolem.golem.GolemManager;
import com.mygolem.model.GolemRecord;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GolemEntityListener implements Listener {

    private final MyGolemConfig config;
    private final ControllerItem controllerItem;
    private final GolemManager manager;

    public GolemEntityListener(MyGolemConfig config, ControllerItem controllerItem, GolemManager manager) {
        this.config = config;
        this.controllerItem = controllerItem;
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        UUID golemId = manager.golemId(event.getRightClicked());
        if (golemId == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        GolemRecord record = manager.get(golemId).orElse(null);
        if (record == null || !record.owner().equals(player.getUniqueId())) {
            player.sendMessage(config.message("这不是你的傀儡。"));
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (controllerItem.isController(item)) {
            controllerItem.select(player, item, golemId);
        }
        player.openInventory(manager.createMenu(golemId));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (manager.golemId(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }
}
