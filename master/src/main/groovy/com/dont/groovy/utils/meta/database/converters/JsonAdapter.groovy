package com.dont.groovy.utils.meta.database.converters

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

import javax.persistence.AttributeConverter
import java.lang.reflect.Type

class JsonAdapter<K> implements AttributeConverter<K, String> {

    static Gson GSON

    static void buildGson(Map<Type, TypeAdapter> adapters) {
        def builder = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .registerTypeAdapter(Location, new TypeAdapter<Location>() {
                    @Override
                    void write(JsonWriter out, Location value) throws IOException {
                        out.beginObject()
                        out.name("location").value(LocationConverter.INSTANCE.convertToDatabaseColumn(value))
                        out.endObject()
                    }

                    @Override
                    Location read(JsonReader reader) throws IOException {
                        reader.beginObject()
                        reader.nextName()
                        def location = LocationConverter.INSTANCE.convertToEntityAttribute(reader.nextString())
                        reader.endObject()
                        return location
                    }
                })
                .registerTypeHierarchyAdapter(ItemStack, new TypeAdapter<ItemStack>() {
                    @Override
                    void write(JsonWriter out, ItemStack value) throws IOException {
                        out.beginObject()
                        out.name("item").value(ItemAdapter.INSTANCE.convertToDatabaseColumn(value))
                        out.endObject()
                    }

                    @Override
                    ItemStack read(JsonReader reader) throws IOException {
                        reader.beginObject()
                        reader.nextName()
                        def item = ItemAdapter.INSTANCE.convertToEntityAttribute(reader.nextString())
                        reader.endObject()
                        return item
                    }
                })
        adapters.each { builder.registerTypeAdapter(it.key, it.value) }
        GSON = builder.create()
    }

    private Class<K> clazz

    JsonAdapter(Class<K> clazz) {
        this.clazz = clazz
    }

    @Override
    String convertToDatabaseColumn(K attribute) {
        return GSON.toJson(attribute)
    }

    @Override
    K convertToEntityAttribute(String dbData) {
        return GSON.fromJson(dbData, clazz)
    }
}
