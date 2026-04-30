package com.mygolem.listener;

import com.mygolem.config.MyGolemConfig;
import com.mygolem.controller.ControllerItem;
import com.mygolem.golem.GolemManager;
import com.mygolem.model.GolemRecord;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ControllerListener implements Listener {

    private final MyGolemConfig config;
    private final ControllerItem controllerItem;
    private final GolemManager manager;

    public ControllerListener(MyGolemConfig config, ControllerItem controllerItem, GolemManager manager) {
        this.config = config;
        this.controllerItem = controllerItem;
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!controllerItem.isController(item)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedBlock() == null) {
            return;
        }
        UUID selected = controllerItem.selected(item);
        if (event.getClickedBlock().getState() instanceof Container) {
            if (selected == null || manager.get(selected).isEmpty()) {
                player.sendMessage(config.message("请先右键一个傀儡，或先用控制器召唤傀儡。"));
                return;
            }
            GolemRecord record = manager.get(selected).get();
            if (!record.owner().equals(player.getUniqueId())) {
                player.sendMessage(config.message("这不是你的傀儡。"));
                return;
            }
            manager.bindChest(selected, event.getClickedBlock().getLocation());
            player.sendMessage(config.message("已绑定箱子。"));
            return;
        }
        if (selected != null && manager.get(selected).isPresent() && player.isSneaking()) {
            manager.setCenter(selected, event.getClickedBlock().getLocation());
            player.sendMessage(config.message("已设置工作中心。"));
            return;
        }
        if (selected == null || manager.get(selected).isEmpty()) {
            Location spawn = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
            GolemRecord record = manager.create(player, spawn);
            if (record != null) {
                controllerItem.select(player, item, record.id());
                player.sendMessage(config.message("已召唤傀儡。"));
            }
            return;
        }
        GolemRecord record = manager.get(selected).get();
        if (!record.owner().equals(player.getUniqueId())) {
            player.sendMessage(config.message("这不是你的傀儡。"));
            return;
        }
        if (!manager.hasLiveEntity(selected)) {
            Location spawn = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);
            if (manager.summonExisting(selected, spawn) != null) {
                player.sendMessage(config.message("已重新召唤傀儡。"));
            } else {
                player.sendMessage(config.message("无法在这里召唤傀儡。"));
            }
            return;
        }
        player.openInventory(manager.createMenu(selected));
    }
}
