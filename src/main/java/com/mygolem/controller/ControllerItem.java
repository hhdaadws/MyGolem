package com.mygolem.controller;

import com.mygolem.config.MyGolemConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.UUID;

public class ControllerItem {

    private final MyGolemConfig config;
    private final NamespacedKey markerKey;
    private final NamespacedKey selectedKey;

    public ControllerItem(Plugin plugin, MyGolemConfig config) {
        this.config = config;
        this.markerKey = new NamespacedKey(plugin, "controller");
        this.selectedKey = new NamespacedKey(plugin, "selected_golem");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(config.controllerMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6傀儡控制器");
        meta.setLore(List.of("§7右键地面召唤傀儡", "§7右键傀儡打开管理", "§7右键箱子绑定仓库"));
        meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isController(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != config.controllerMaterial() || !itemStack.hasItemMeta()) {
            return false;
        }
        return itemStack.getItemMeta().getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public UUID selected(ItemStack itemStack) {
        if (!isController(itemStack)) {
            return null;
        }
        String raw = itemStack.getItemMeta().getPersistentDataContainer().get(selectedKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void select(Player player, ItemStack itemStack, UUID golemId) {
        if (!isController(itemStack)) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(selectedKey, PersistentDataType.STRING, golemId.toString());
        meta.setLore(List.of("§7已选择傀儡", "§f" + golemId, "§7右键傀儡/箱子/地面进行管理"));
        itemStack.setItemMeta(meta);
        player.getInventory().setItemInMainHand(itemStack);
    }
}
