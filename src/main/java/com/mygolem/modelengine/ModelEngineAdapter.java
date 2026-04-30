package com.mygolem.modelengine;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

public class ModelEngineAdapter {

    public boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("ModelEngine");
    }

    public boolean apply(Entity entity, String modelId) {
        if (!available() || entity == null || modelId == null || modelId.isBlank()) {
            return false;
        }
        try {
            ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(entity);
            ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
            modeledEntity.addModel(activeModel, true);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    public void remove(Entity entity) {
        if (!available() || entity == null) {
            return;
        }
        try {
            ModelEngineAPI.removeModeledEntity(entity);
        } catch (Throwable ignored) {
        }
    }
}
