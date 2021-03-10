package com.dont.testplugin.bukkit

import com.dont.groovy.models.annotations.EventSetting
import com.dont.groovy.models.annotations.Inject
import com.dont.testplugin.Terminal
import com.dont.testplugin.models.Machine
import com.dont.testplugin.models.User
import com.dont.testplugin.services.ItemService
import org.bukkit.Material
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

@Inject
class TestListener {

    Terminal main
    ItemService itemService

    def onJoin = { PlayerJoinEvent e ->
        e.getPlayer().sendMessage("ola tudo bem ${itemService.getPedra()}")
        log(e.getPlayer())
    }

    def onInteract = { PlayerInteractEvent e ->
        if (!e.hasBlock()) return
        if (!e.hasItem() || e.getItem().getType() != Material.STICK) return
        def clicked = e.getClickedBlock().getLocation()
        def player = e.player
        def machine = Machine.CACHE.getCached(clicked)
                ?: new Machine(owner: player.name, location: clicked)
        machine.timesClicked++
        player.sendMessage("$machine")
        machine.cache(true) // if not cached, cache it
    }

    @EventSetting(priority = EventPriority.HIGH)
    def onQuit = { PlayerQuitEvent e ->
        log("xau ${e.getPlayer()}")
    }

    def onBreak = { BlockBreakEvent e ->
        def player = e.player
        def user = User.findByName(e.player.name) ?: new User(name: e.player.name)
        user.setBlocks(user.getBlocks() + 1)
        user.test.put(player.itemInHand.type, user.test.getOrDefault(player.itemInHand.type, 0) + 1)
        user.houhou.put(player.itemInHand, user.houhou.getOrDefault(player.itemInHand, 0) + 1)
        user.me = player.location.block.location
        user.wuba.add(user.me)
        player.sendMessage("$user.test")
        player.sendMessage("$user.houhou")
        user.save()
    }
}
