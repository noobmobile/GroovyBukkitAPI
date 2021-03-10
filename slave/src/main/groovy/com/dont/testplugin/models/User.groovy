package com.dont.testplugin.models

import com.dont.groovy.models.Cache
import com.dont.groovy.models.annotations.CacheOptions
import com.dont.groovy.models.annotations.Inject
import groovy.transform.ToString
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Inject
@ToString
@Table(name = "groovyusers")
class User {

    @CacheOptions(loadOnJoin = true, saveOnQuit = true)
    static final Cache<String, User> CACHE = new Cache<>(User, { it.name })

    @Id
    String name
    int blocks = 76
    Map<Material, Integer> test = new HashMap<>()
    Map<ItemStack, Integer> houhou = new HashMap<>()
    Location me
    List<Location> wuba = []

}
