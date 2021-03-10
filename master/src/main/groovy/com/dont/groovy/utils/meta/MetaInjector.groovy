package com.dont.groovy.utils.meta

import com.dont.groovy.AbstractTerminal
import com.dont.groovy.models.annotations.EventSetting
import com.dont.groovy.models.annotations.Inject
import com.dont.groovy.models.bukkit.Context
import com.dont.groovy.models.bukkit.ContextException
import com.dont.groovy.utils.InventoryHandler
import com.dont.groovy.utils.ReflectionUtils
import com.dont.groovy.utils.Serializers
import com.dont.groovy.utils.Utils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Event
import org.bukkit.event.EventException
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.EventExecutor
import org.bukkit.scheduler.BukkitRunnable
import org.codehaus.groovy.runtime.DefaultGroovyMethods

import java.lang.reflect.Field
import java.util.concurrent.ThreadLocalRandom

class MetaInjector {

    static void defaultInjects(instance) {
        instance.customInject("log", { instance.simpleLogger.log(it) })
        instance.customInject("debug", { instance.simpleLogger.debug(it) })
        instance.customInject("menu", { String name, int size -> return new InventoryHandler(name, size) })
        instance.customInject("sync", {
            new BukkitRunnable() {
                void run() {
                    it()
                }
            }.runTask(instance)
            return instance
        })
        instance.customInject("async", {
            new BukkitRunnable() {
                void run() {
                    it()
                }
            }.runTaskAsynchronously(instance)
            return instance
        })
        String.metaClass.translate = { -> ChatColor.translateAlternateColorCodes('&' as char, delegate as String) }
        List.metaClass.translate = { -> (delegate as List).collect { it instanceof String ? it.translate() : it } }
        String.metaClass.untranslate = { -> delegate.replace("§", "&") }
        List.metaClass.untranslate = { -> (delegate as List).collect { it instanceof String ? it.untranslate() : it } }
        List.metaClass.random = { -> (delegate as List).get(ThreadLocalRandom.current().nextInt(delegate.size())) }
        List.metaClass.randomItem { ->
            def list = delegate as List<ItemStack>
            def total = list.collect { it.chance }.sum() as double
            def random = ThreadLocalRandom.current().nextDouble() * total
            def weight = 0D
            for (ItemStack t in list) {
                weight += t.chance
                if (weight >= random) {
                    return t
                }
            }
            return null;
        }
        String.metaClass.define {
            def oldAsType = String.metaClass.getMetaMethod("asType", Class)
            asType = { Class clazz ->
                if (clazz == Location) { // String as Location
                    return Serializers.Locations.deserializeLocation(delegate)
                } else {
                    return oldAsType.invoke(delegate, clazz)
                }
            }
        }
        ConfigurationSection.metaClass.define {
            asType = { Class clazz ->
                if (ConfigurationSection.isAssignableFrom(clazz)) {
                    DefaultGroovyMethods.asType(delegate, clazz)
                } else {
                    YmlMetaConverter.asType(delegate, clazz)
                }
            }
            asList = { Class clazz ->
                YmlMetaConverter.asTypeList(delegate, clazz)
            }
            asArray = { Class clazz ->
                YmlMetaConverter.asTypeList(delegate, clazz).toArray()
            }
            asMap = { Class clazz, Closure closure ->
                YmlMetaConverter.asTypeList(delegate, clazz).collectEntries { [closure(it), it] }
            }
        }
        injectInventories()
    }

    static void injectInventories() {
        ItemStack.metaClass.edit = { Closure editor ->
            def clone = delegate.clone();
            def meta = clone.getItemMeta();
            editor(meta);
            clone.setItemMeta(meta);
            return clone
        }
        ItemStack.metaClass.editPlaceholders = { Map<String, Object> placeholders ->
            return delegate.edit({ ItemMeta meta ->
                if (meta.hasDisplayName()) {
                    meta.setDisplayName(Utils.replace(meta.getDisplayName(), placeholders));
                }
                if (meta.hasLore()) {
                    meta.setLore(meta.getLore().collect { Utils.replace(it, placeholders) })
                }
                if (meta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) meta;
                    if (skullMeta.hasOwner()) {
                        skullMeta.setOwner(Utils.replace(skullMeta.getOwner(), placeholders));
                    }
                }
            });
        }
        ConfigurationSection.metaClass.asInventory = { Map<String, Object> args ->
            def name = delegate.getString("Name").translate()
            int size = delegate.getInt("Size")
            def placeholders = args.findAll { !(it.value instanceof Closure) }
            def closures = args.findAll { (it.value instanceof Closure) }
            def handler = new InventoryHandler(name, size)
            delegate.getKeys(false).each { key ->
                def object = delegate.get(key)
                if (key.equals("Name") || key.equals("Size")) return
                if (key.equals("Scroller")) {
                    def scrollerSection = object as ConfigurationSection
                    handler.scroller()
                    if (scrollerSection.isSet("Slots")) {
                        def slots = scrollerSection.getString("Slots").split(",").collect { Integer.parseInt(it) }
                        handler.slots(slots)
                    }
                    if (scrollerSection.isSet("PreviousPage")) {
                        def item = scrollerSection.getConfigurationSection("PreviousPage") as ItemStack
                        handler.setScrollerPreviousPageItem(item)
                        handler.setScrollerPreviousPageSlot(item.slot)
                    }
                    if (scrollerSection.isSet("NextPage")) {
                        def item = scrollerSection.getConfigurationSection("NextPage") as ItemStack
                        handler.setScrollerNextPageItem(item)
                        handler.setScrollerNextPageSlot(item.slot)
                    }
                } else if (object instanceof ConfigurationSection) {
                    def item = object as ItemStack
                    if (item.hasSlot()) {
                        def editedItem = item.editPlaceholders(placeholders)
                        def itemClosure = closures.get(key)
                        handler.item(item.slot, editedItem, itemClosure)
                    } else {
                        handler.metaClass."$key" = item
                    }
                }
            }
            return handler
        }
    }

    static void injectMeta(List<Class> classes, instance) {
        classes.sort({ it.getAnnotation(Inject).priority() }).each { clazz ->
            try {
                def canInitialize = clazz.getAnnotation(Inject).initialize()
                // se o service for null, o field terá de ser estático
                def service = canInitialize ? instance.registerService(clazz) : null
                // na main já conhecemos a instância
                if (AbstractTerminal.isAssignableFrom(clazz)) service = getInstance()
                ReflectionUtils.setField(clazz, service, "main", getInstance()) // instancia fields com nome de main
                injectDependencies(clazz, service, instance) // instancia os fields dos services
                if (service) {
                    instance.injects.each {
                        service.metaClass."$it.key" = it.value
                    }
                }
                if (clazz.name.endsWith("Command")) {
                    injectCommands(clazz, service, instance) // kinda obvious bro
                }
                if (clazz.name.endsWith("Listener")) {
                    injectListener(clazz, service, instance)
                }
                // infelizmente o construtor só poderia ser chamado depois das injeções de dependências
                // como não é possível, deve haver um método init (caso) queira ter um construtor
                ReflectionUtils.invokeMethod(clazz, service, "init")
            } catch (any) {
                println "error on ${clazz.name}:"
                any.printStackTrace()
            }
        }
    }

    static void injectListener(clazz, service, instance) {
        Map<Field, Closure> closures = [:]
        clazz.getDeclaredFields().each { field ->
            boolean old = field.isAccessible()
            field.setAccessible(true)
            final closure = field.get(service)
            field.setAccessible(old)
            if (!(closure instanceof Closure)) return
            instance.simpleLogger.debug("instanciando listener ${field.name}")
            closures.put(field, closure)
        }
        def blankListener = new Listener() {}
        closures.each { entry ->
            final closure = entry.value
            def eventClass = entry.value.parameterTypes[0]
            def eventExecutor = new EventExecutor() {
                @Override
                void execute(Listener listener, Event event) throws EventException {
                    closure(event)
                }
            }
            def setting = entry.key.getAnnotation(EventSetting)
            instance.getServer().getPluginManager().registerEvent(eventClass, blankListener,
                    setting?.priority() ?: EventPriority.NORMAL,
                    eventExecutor, instance, setting?.ignoreCancelled() ?: false)
        }
    }

    static void injectCommands(clazz, service, instance) {
        def i18n = instance.registerConfig("i18n.yml")
        clazz.getDeclaredFields().each { field ->
            boolean old = field.isAccessible()
            field.setAccessible(true)
            final closure = field.get(service)
            field.setAccessible(old)
            if (!(closure instanceof Closure)) return
            instance.simpleLogger.debug("instanciando comando /${field.name}")
            instance.getCommand(field.name).setExecutor(new CommandExecutor() {
                @Override
                boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                    def context = new Context(sender: sender, args: args, i18n: i18n)
                    try {
                        String result = closure(context) // como é uma closure, pode ser chamada como metodo
                        if (result) throw new ContextException(result)
                    } catch (ContextException e) {
                        if (e.getMessage() == null) {
                            sender.sendMessage(e.getReturned())
                        } else {
                            sender.sendMessage(e.getMessage())
                        }
                    }
                    false
                }
            })
        }
    }

    static void injectDependencies(clazz, service, instance) {
        clazz.getDeclaredFields().each { field ->
            if (field.name.endsWith("Service")) {
                ReflectionUtils.setField(clazz, service, field.name, instance.registerService(field.type))
            }
            if (FileConfiguration.isAssignableFrom(field.type)) {
                def config = instance.registerConfig(field.name)
                ReflectionUtils.setField(clazz, service, field.name, config)
            }
        }
    }

    static void injectConfigFields(YamlConfiguration config) {
        def fields = [:]
        config.getKeys(false).each { key ->
            def object = config.get(key)
            inject(config, key, object, fields)
        }
        config.metaClass.fields = { fields }
    }

    // função recursiva
    private static void inject(currentMeta, currentKey, currentObject, fields) {
        if (ConfigurationSection.isAssignableFrom(currentObject.class)) {
            def object = currentObject // old: new Object()
            def newSection = currentObject as ConfigurationSection
            newSection.getKeys(false).each { newKey ->
                def newObject = newSection.get(newKey)
                inject(object, newKey, newObject, fields)
            }
            currentMeta.metaClass."$currentKey" = object
        } else {
            def object = currentObject instanceof String || currentObject instanceof List ? currentObject.translate() : currentObject
            currentMeta.metaClass."$currentKey" = object
            fields.put(currentKey, object)
        }
    }

}
