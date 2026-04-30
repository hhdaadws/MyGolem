package com.mygolem.storage;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class BukkitItemStackCodec implements ItemStackCodec {

    @Override
    public String encode(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeObject(itemStack);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize golem item.", exception);
        }
    }

    @Override
    public ItemStack decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            return (ItemStack) input.readObject();
        } catch (Exception exception) {
            Bukkit.getLogger().warning("[MyGolem] Failed to deserialize stored item, slot reset: " + exception.getMessage());
            return null;
        }
    }
}
