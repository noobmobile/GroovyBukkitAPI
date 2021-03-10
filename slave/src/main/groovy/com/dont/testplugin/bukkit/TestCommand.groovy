package com.dont.testplugin.bukkit

import com.dont.groovy.models.annotations.Inject
import com.dont.groovy.models.bukkit.Context
import com.dont.testplugin.Terminal
import com.dont.testplugin.models.User
import com.dont.testplugin.services.ItemService
import com.dont.testplugin.services.TestService
import org.bukkit.configuration.file.FileConfiguration

@Inject
class TestCommand {

    Terminal main
    ItemService itemService
    TestService testService
    FileConfiguration inventories

    def test = { Context context ->
        log User.CACHE
        return null
    }


}
