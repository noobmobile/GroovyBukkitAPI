package com.dont.groovy.utils.meta

import com.dont.groovy.models.annotations.Inject
import com.dont.groovy.models.annotations.YmlCustomConverter
import com.dont.groovy.utils.ItemBuilder
import com.dont.groovy.utils.ReflectionUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.util.concurrent.ThreadLocalRandom

class YmlMetaConverter {
    private static final Map<Class, Closure> CONVERTERS = [:]

    static {
        register(ItemStack.class, { ConfigurationSection section ->
            Material type = section.getString("Material")?.toUpperCase()?.asType(Material)
            int amount = section.getInt("Amount") ?: 1
            int data = section.getInt("Data") ?: 0
            String name = section.getString("Name")?.translate()
            List<String> lore = section.getStringList("Lore")?.translate()
            Map<Enchantment, Integer> enchantments = (section.getStringList("Enchants") ?: section.getStringList("Enchantments"))
                    .collect { it.replace(" ", "").split(":") }
                    .collectEntries { [Enchantment.getByName(it[0].toUpperCase()), it[1] as int] }
            String texture = section.getString("Texture")
            boolean glow = section.getBoolean("Glow")
            List<String> commands = section.getStringList("Commands")
            int slot = section.getInt("Slot")
            double chance = section.getDouble("Chance")
            def item = (texture ? new ItemBuilder(texture) : new ItemBuilder(type, amount, data))
                    .setName(name)
                    .setLore(lore)
                    .setAmount(amount)
                    .addEnchantments(enchantments)
                    .addGlow(glow)
                    .toItemStack()
            item.metaClass.getCommands = { -> commands }
            item.metaClass.getChance = { -> chance }
            item.metaClass.getSlot = { -> slot }
            item.metaClass.hasCommands = { -> return !!delegate.commands }
            item.metaClass.hasChance = { -> return !!delegate.chance }
            item.metaClass.hasSlot = { -> return !!delegate.slot }
            item.metaClass.test = { ->
                return delegate.chance >= 100 || (ThreadLocalRandom.current().nextDouble() * 100) <= delegate.chance
            }
            item.metaClass.execute = { Player player ->
                if (delegate.hasCommands()) {
                    delegate.getCommands().each {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("\$player", player.name))
                    }
                } else {
                    player.getInventory().addItem(delegate)
                }
            }
            return item
        })
        register(Location.class, { String string ->
            return string as Location
        })
    };

    static def of(Class clazz) {
        return CONVERTERS.get(clazz) ?: CONVERTERS.find { it.key.isAssignableFrom(clazz) }?.value
    }

    static void register(clazz, closure) {
        CONVERTERS.put(clazz, closure)
    }

    static def isRegistered(clazz) {
        CONVERTERS.containsKey(clazz) || CONVERTERS.keySet().any { it.isAssignableFrom(clazz) }
    }

    static <T> T asType(left, Class<T> right) {
        def converter = of(right)
        if (!converter) {
            if (right.isAnnotationPresent(Inject) && ConfigurationSection.isAssignableFrom(left.class)) {
                return asTypeDynamic(left, right)
            }
            if (Enum.isAssignableFrom(right)) {
                return Enum.valueOf(right, left.toUpperCase())
            }
            throw new IllegalArgumentException("No converter of ${left.class.simpleName} to ${right.simpleName}. Please annotate with @Inject.")
        }
        return (T) converter(left)
    }

    // dynamically convert ConfigurationSections to Classes (annotated with @Inject)
    static <T> T asTypeDynamic(ConfigurationSection left, Class<T> right) {
        def sectionKey = left.getName()
        def constructor = [:]
        if (ReflectionUtils.hasField(right, "key")) {
            constructor.put("key", sectionKey)
        }
        left.getKeys(false).each { key ->
            Field field = right.declaredFields.find { field -> field.name.equalsIgnoreCase(key) }
            if (!field) throw new IllegalStateException("No field found for $key in $right.simpleName")
            def fieldType = field.type
            def object = left.get(key)
            if (field.isAnnotationPresent(YmlCustomConverter)) {
                def customConverter = field.getAnnotation(YmlCustomConverter)
                def closure = customConverter.value().newInstance(object, object)
                constructor.put(field.name, closure(object))
            } else if (ConfigurationSection.isAssignableFrom(object.class)) {
                if (fieldType.isAnnotationPresent(Inject)) {
                    // Converting a ConfigurationSection to a type dynamically (recursive)
                    constructor.put(field.name, asTypeDynamic(object, fieldType))
                } else if (List.isAssignableFrom(fieldType)) {
                    // Converting a ConfigurationSection to  List<ItemStack> for example
                    def fieldListType = (field.genericType as ParameterizedType).getActualTypeArguments()[0]
                    constructor.put(field.name, asTypeList(object, fieldListType))
                } else { // Converting a ConfigurationSection to a single ItemStack for example
                    constructor.put(field.name, asType(object, fieldType))
                }
            } else if (List.isAssignableFrom(object.class)) {
                def fieldListType = (field.genericType as ParameterizedType).getActualTypeArguments()[0]
                def objectListType = (object as List)[0]?.class
                if (object && fieldListType != objectListType) {
                    // Converting a List<String> to List<Location> for example
                    constructor.put(field.name, asTypeList(object, fieldListType))
                } else {
                    constructor.put(field.name, object.translate())
                }
            } else {
                def translated = object instanceof String ? object.translate() : object
                constructor.put(field.name, isRegistered(fieldType) ? asType(object, fieldType) : translated)
            }
        }
        def constructed = constructor.asType(right)
        ReflectionUtils.invokeMethod(right, constructed, "init")
        return constructed
    }

    static <T> List<T> asTypeList(left, Class<T> right) {
        if (ConfigurationSection.isAssignableFrom(left.class)) {
            return left.getKeys(false).collect { key -> asType(left.get(key), right) }
        }
        return left.collect { asType(it, right) }
    }

}
