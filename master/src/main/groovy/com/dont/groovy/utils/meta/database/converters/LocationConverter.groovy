package com.dont.groovy.utils.meta.database.converters

import com.dont.groovy.utils.Serializers
import org.bukkit.Location

import javax.persistence.AttributeConverter

class LocationConverter implements AttributeConverter<Location, String> {

    static final INSTANCE = new LocationConverter()

    @Override
    String convertToDatabaseColumn(Location attribute) {
        return Serializers.Locations.serializeLocation(attribute);
    }

    @Override
    Location convertToEntityAttribute(String dbData) {
        return Serializers.Locations.deserializeLocation(dbData);
    }
}
