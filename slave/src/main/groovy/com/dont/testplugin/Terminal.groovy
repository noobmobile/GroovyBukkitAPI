package com.dont.testplugin


import com.dont.groovy.AbstractTerminal
import com.dont.groovy.models.annotations.Inject

@Inject(initialize = false)
class Terminal extends AbstractTerminal {


    @Override
    void preEnable() {
    }

    @Override
    void enable() {
        useDatabase() // dont wanna databases? just exclude this line
    }

    @Override
    void disable() {

    }

}
