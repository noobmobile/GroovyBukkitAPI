package com.dont.testplugin.models

import com.dont.groovy.models.Cache
import com.dont.groovy.models.annotations.CacheOptions
import com.dont.groovy.models.annotations.Inject
import groovy.transform.ToString
import org.bukkit.Location

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@ToString
@Entity
@Inject
@Table(name = "groovymachines")
class Machine {

    @CacheOptions(loadAllOnEnable = true, saveAllOnDisable = true)
    static final Cache<Location, Machine> CACHE = new Cache<>(Machine, { it.location })

    @Id
    Location location
    String owner
    int timesClicked


}
