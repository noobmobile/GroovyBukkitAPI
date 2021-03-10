package com.dont.groovy.utils.meta.database.converters

import com.dont.groovy.utils.Serializers
import org.bukkit.inventory.ItemStack

import javax.persistence.AttributeConverter

class ItemAdapter implements AttributeConverter<ItemStack, String> {

    static final INSTANCE = new ItemAdapter()

    @Override
    String convertToDatabaseColumn(ItemStack attribute) {
        return Serializers.Item.serializeItem(attribute)
    }

    @Override
    ItemStack convertToEntityAttribute(String dbData) {
        return Serializers.Item.deserializeItem(dbData)
    }
}
