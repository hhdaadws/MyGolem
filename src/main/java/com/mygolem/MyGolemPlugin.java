package com.mygolem;

import com.mygolem.chunk.ChunkTicketManager;
import com.mygolem.command.MyGolemCommand;
import com.mygolem.config.MyGolemConfig;
import com.mygolem.controller.ControllerItem;
import com.mygolem.customcrops.BukkitCustomCropsFacade;
import com.mygolem.golem.GolemManager;
import com.mygolem.listener.ControllerListener;
import com.mygolem.listener.DropRedirectListener;
import com.mygolem.listener.GolemEntityListener;
import com.mygolem.listener.MenuListener;
import com.mygolem.modelengine.ModelEngineAdapter;
import com.mygolem.protection.ProtectionService;
import com.mygolem.storage.GolemRepository;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MyGolemPlugin extends JavaPlugin {

    private MyGolemConfig myConfig;
    private GolemRepository repository;
    private GolemManager golemManager;

    @Override
    public void onEnable() {
        myConfig = new MyGolemConfig(this);
        myConfig.reload();
        try {
            repository = GolemRepository.open(myConfig.sqliteFile());
        } catch (Exception exception) {
            getLogger().severe("Failed to open SQLite storage: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ControllerItem controllerItem = new ControllerItem(this, myConfig);
        BukkitCustomCropsFacade customCrops = new BukkitCustomCropsFacade(this);
        ModelEngineAdapter modelEngine = new ModelEngineAdapter();
        ChunkTicketManager chunkTickets = new ChunkTicketManager(this);
        ProtectionService protection = new ProtectionService(this, myConfig);
        golemManager = new GolemManager(this, myConfig, repository, customCrops, modelEngine, chunkTickets, protection);

        try {
            golemManager.load();
        } catch (Exception exception) {
            getLogger().severe("Failed to load golems: " + exception.getMessage());
        }

        getServer().getPluginManager().registerEvents(new ControllerListener(myConfig, controllerItem, golemManager), this);
        getServer().getPluginManager().registerEvents(new GolemEntityListener(myConfig, controllerItem, golemManager), this);
        getServer().getPluginManager().registerEvents(new MenuListener(myConfig, golemManager), this);
        getServer().getPluginManager().registerEvents(new DropRedirectListener(), this);

        MyGolemCommand command = new MyGolemCommand(myConfig, controllerItem, golemManager);
        PluginCommand pluginCommand = getCommand("mygolem");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
    }

    @Override
    public void onDisable() {
        if (golemManager != null) {
            golemManager.shutdown();
        }
        if (repository != null) {
            try {
                repository.close();
            } catch (Exception exception) {
                getLogger().warning("Failed to close SQLite storage: " + exception.getMessage());
            }
        }
    }
}
