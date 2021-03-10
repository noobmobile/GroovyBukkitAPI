package com.dont.testplugin.models

import com.dont.groovy.models.annotations.Inject

@Inject(initialize = false)
class Categoria {

    String key
    String name
    int size
    List<CategoriaItem> categoriaItems

    Map<Integer, CategoriaItem> itemsBySlot

    void init() {
        itemsBySlot = categoriaItems.collectEntries { [it.slot, it] }
    }


    @Override
    public String toString() {
        return "Categoria{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", itemsBySlot=" + itemsBySlot +
                '}';
    }
}
