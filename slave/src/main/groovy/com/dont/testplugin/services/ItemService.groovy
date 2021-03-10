package com.dont.testplugin.services

import com.dont.groovy.models.annotations.Inject
import com.dont.testplugin.Terminal
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

@Inject
class ItemService {

    Terminal main
    TestService testService
    int times

    void init() {
        log "ihuuul"
        log "seria eu o ${testService.test}"
    }

    ItemStack getPedra() {
        def item = new ItemStack(Material.STONE, times++)
        log "cu"
        def meta = item.getItemMeta()
        meta.setDisplayName("§6${main.getName()}")
        item.setItemMeta(meta)
        item
    }

    boolean isPedra(itemStack) {
        itemStack.getType() == Material.STONE
    }

}
