package com.mygolem.storage;

import org.bukkit.inventory.ItemStack;

public interface ItemStackCodec {

    String encode(ItemStack itemStack);

    ItemStack decode(String encoded);
}
