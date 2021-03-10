package com.dont.testplugin.models

import com.dont.groovy.models.annotations.Inject
import org.bukkit.inventory.ItemStack

@Inject(initialize = false)
class CategoriaItem {

    String key
    ItemStack item
    int slot

    void init() {
        slot = item.slot
    }


    @Override
    public String toString() {
        return "CategoriaItem{" +
                "key='" + key + '\'' +
                ", item=" + item +
                ", slot=" + slot +
                ", commands=" + item.commands +
                '}';
    }
}
