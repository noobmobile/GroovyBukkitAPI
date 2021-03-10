package com.dont.groovy.utils.meta.database.converters


import javax.persistence.AttributeConverter
import java.lang.reflect.Field

class CollectionAdapter<K> implements AttributeConverter<K, String> {

    private Class<K> clazz
    private Field field

    CollectionAdapter(Field field) {
        this.clazz = field.type as Class<K>
        this.field = field
    }

    @Override
    String convertToDatabaseColumn(K attribute) {
        try {
            return JsonAdapter.GSON.toJson(attribute)
        } catch (any) {
            any.printStackTrace()
            println "error in $attribute $field"
            return "{}"
        }
    }

    @Override
    K convertToEntityAttribute(String dbData) {
        return JsonAdapter.GSON.fromJson(dbData, field.genericType)
    }
}
