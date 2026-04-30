package com.mygolem.listener;

import com.mygolem.customcrops.GolemDropRouter;
import net.momirealms.customcrops.api.event.DropItemActionEvent;
import net.momirealms.customcrops.api.event.QualityCropActionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DropRedirectListener implements Listener {

    @EventHandler
    public void onDropItemAction(DropItemActionEvent event) {
        if (GolemDropRouter.redirect(event.location(), event.item())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQualityCropAction(QualityCropActionEvent event) {
        if (GolemDropRouter.redirect(event.location(), event.items())) {
            event.setCancelled(true);
        }
    }
}
