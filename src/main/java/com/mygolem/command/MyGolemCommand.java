package com.mygolem.command;

import com.mygolem.config.MyGolemConfig;
import com.mygolem.controller.ControllerItem;
import com.mygolem.golem.GolemManager;
import com.mygolem.model.GolemRecord;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MyGolemCommand implements CommandExecutor, TabCompleter {

    private final MyGolemConfig config;
    private final ControllerItem controllerItem;
    private final GolemManager manager;

    public MyGolemCommand(MyGolemConfig config, ControllerItem controllerItem, GolemManager manager) {
        this.config = config;
        this.controllerItem = controllerItem;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(config.message("/mygolem give <player> | reload | list [player] | remove <id> | debug <id>"));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> give(sender, args);
            case "reload" -> reload(sender);
            case "list" -> list(sender, args);
            case "remove" -> remove(sender, args);
            case "debug" -> debug(sender, args);
            default -> sender.sendMessage(config.message("未知命令。"));
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mygolem.admin")) {
            sender.sendMessage(config.message("你没有权限。"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.message("用法：/mygolem give <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(config.message("玩家不在线。"));
            return;
        }
        target.getInventory().addItem(controllerItem.create());
        sender.sendMessage(config.message("已给予控制器。"));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("mygolem.admin")) {
            sender.sendMessage(config.message("你没有权限。"));
            return;
        }
        config.reload();
        sender.sendMessage(config.message("配置已重载。"));
    }

    private void list(CommandSender sender, String[] args) {
        UUID filter = null;
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                filter = target.getUniqueId();
            }
        }
        UUID finalFilter = filter;
        manager.all().stream()
                .filter(record -> finalFilter == null || record.owner().equals(finalFilter))
                .forEach(record -> sender.sendMessage(config.message(record.id() + " owner=" + record.owner() + " active=" + record.active())));
    }

    private void remove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mygolem.admin")) {
            sender.sendMessage(config.message("你没有权限。"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.message("用法：/mygolem remove <golemId>"));
            return;
        }
        try {
            manager.remove(UUID.fromString(args[1]));
            sender.sendMessage(config.message("已移除。"));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(config.message("傀儡 ID 不正确。"));
        }
    }

    private void debug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mygolem.admin")) {
            sender.sendMessage(config.message("你没有权限。"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.message("用法：/mygolem debug <golemId>"));
            return;
        }
        try {
            GolemRecord record = manager.get(UUID.fromString(args[1])).orElse(null);
            sender.sendMessage(config.message(record == null ? "找不到傀儡。" : record.toString()));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(config.message("傀儡 ID 不正确。"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give", "reload", "list", "remove", "debug");
        }
        return new ArrayList<>();
    }
}
