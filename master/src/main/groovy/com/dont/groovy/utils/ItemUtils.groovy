package com.dont.groovy.utils

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ItemUtils {

    static void removeItemFromHand(Player player, int amount) {
        ItemStack item = player.getItemInHand();
        if (item.getAmount() > amount) {
            item.setAmount(item.getAmount() - amount);
        } else {
            player.setItemInHand(new ItemStack(Material.AIR));
        }
    }

    static void removeItemFromHand(Player player) {
        removeItemFromHand(player, 1);
    }

}
