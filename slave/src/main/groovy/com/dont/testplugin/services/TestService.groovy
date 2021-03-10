package com.dont.testplugin.services

import com.dont.groovy.models.annotations.Inject
import com.dont.testplugin.Terminal

@Inject
class TestService {

    Terminal main
    ItemService itemService

    String test

    void init() {
        test = itemService.getPedra().getItemMeta().getDisplayName()
    }
}
