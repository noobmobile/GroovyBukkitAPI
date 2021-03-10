package com.dont.groovy.utils.meta.database

import com.dieselpoint.norm.Database
import com.dont.groovy.models.Cache
import com.dont.groovy.models.annotations.CacheOptions
import com.dont.groovy.models.annotations.DatabaseCustomConverter
import com.dont.groovy.utils.Utils
import com.dont.groovy.utils.meta.database.converters.JsonAdapter
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.apache.commons.lang.StringUtils
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable

import javax.persistence.Entity
import javax.persistence.Id
import javax.sql.DataSource
import java.lang.reflect.Field
import java.sql.SQLException

class MetaDatabaseInjector {

    static void injectDatabase(instance) {
        def config = instance.registerConfig("database")
        def dbType = config.database.type
        def domainClazzes = instance.clazzes.findAll({ it.isAnnotationPresent(Entity) })
        createGson(domainClazzes)
        def database = createDatabase(config, dbType, instance)
        injectField(database, instance)
        createTables(database, domainClazzes, instance)
        injectMethods(database, domainClazzes, instance)
        injectCaches(database, domainClazzes, instance)
        instance.disables.add({
            database.close()
        })
    }

    static def createGson(List<Class> clazzes) {
        JsonAdapter.buildGson(clazzes
                .collect({ it.getAnnotationsByType(DatabaseCustomConverter) })
                .flatten()
                .collectEntries({ DatabaseCustomConverter it -> [it.target(), it.converter().newInstance()] })
        )
    }

    static void injectCaches(database, domainClazzes, instance) {
        domainClazzes.each { Class clazz ->
            def cacheField = clazz.getDeclaredFields().find { it.isAnnotationPresent(CacheOptions) }
            if (!cacheField) return
            cacheField.setAccessible(true)
            def cache = cacheField.get(null) as Cache
            def cacheOptions = cacheField.getAnnotation(CacheOptions)
            def pluralName = "${clazz.simpleName.toLowerCase()}s"
            if (cacheOptions.loadAllOnEnable()) {
                instance.enables.add({
                    instance.async({
                        int objects = cache.loadAll()
                        instance.debug("loaded $objects $pluralName")
                    })
                })
            }
            if (cacheOptions.saveAllOnDisable()) {
                instance.disables.add({
                    int objects = cache.saveCached()
                    instance.debug("saved $objects $pluralName")
                })
            }
            if (cacheOptions.autoSave()) {
                long delay = 20L * 60 * cacheOptions.autoSaveDelay()
                new BukkitRunnable() {
                    void run() {
                        int objects = cache.saveCached()
                        instance.debug("auto-saved $objects $pluralName")
                    }
                }.runTaskTimerAsynchronously(instance, delay, delay)
            }
            if (cacheOptions.loadOnJoin()) {
                def keyField = clazz.declaredFields.find { it.isAnnotationPresent(Id) }
                keyField.setAccessible(true)
                def findMethod = clazz.metaClass.pickMethod("find", Object.class)
                Bukkit.getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    void onJoin(PlayerJoinEvent e) {
                        def key = e.player.name
                        if (cache.isCached(key)) return
                        instance.async {
                            def inDatabase = findMethod.invoke(null, key)
                            if (inDatabase) {
                                cache.cache(inDatabase)
                                instance.debug("getting ${clazz.simpleName} $key from database")
                            } else {
                                def newUser = clazz.newInstance()
                                keyField.set(newUser, key)
                                cache.cache(newUser)
                                instance.debug("creating new ${clazz.simpleName} $key")
                            }
                        }
                    }
                }, instance as Plugin)
            }
            if (cacheOptions.saveOnQuit()) {
                boolean removeFromCache = cacheOptions.uncacheOnQuit()
                Bukkit.getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    void onQuit(PlayerQuitEvent e) {
                        def key = e.player.name
                        if (!cache.isCached(key)) return
                        instance.async {
                            def inCache = cache.getCached(key)
                            if (removeFromCache) {
                                cache.uncache(key)
                            }
                            inCache.save()
                            instance.debug("saving ${clazz.simpleName} $key in to database")
                        }
                    }
                }, instance as Plugin)
            }
        }
    }

    static void createTables(database, clazzes, instance) {
        clazzes.each { clazz ->
            try {
                database.createTable(clazz)
                instance.simpleLogger.debug("creating table for ${clazz.simpleName}")
            } catch (ignored) {
                instance.simpleLogger.debug("table for ${clazz.simpleName} already created")
            }
        }
    }

    static void injectField(database, instance) {
        instance.metaClass.database = { -> database }
    }

    static def createDatabase(config, dbType, instance) {
        def database = new Database()
        if (dbType == "SQLITE") {
            Class.forName("org.sqlite.JDBC");
            database = new Database() {
                @Override
                protected DataSource getDataSource() throws SQLException {
                    HikariConfig hikariConfig = new HikariConfig();
                    hikariConfig.setDriverClassName("org.sqlite.JDBC");
                    hikariConfig.setJdbcUrl("jdbc:sqlite:${instance.getDataFolder().getPath()}/${instance.name}.db");
                    hikariConfig.setConnectionTestQuery("SELECT 1");
                    return new HikariDataSource(hikariConfig);
                }
            }
            database.setSqlMaker(new CustomSqliteMaker())
        } else if (dbType == "MYSQL") {
            database.setJdbcUrl("jdbc:mysql://${config.database.host}/${config.database.database}?autoReconnect=true")
            database.setSqlMaker(new CustomMysqlMaker())
        }
        database.setUser(config.database.user)
        database.setPassword(config.database.password)
        return database
    }

    //
    private final static PATTERN = ~/(?:top|order|orderBy)(\d*)?(Asc|Desc)?(?:By)?([a-zA-z]+)/

    static void injectMethods(Database database, clazzes, instance) {
        clazzes.each { Class clazz ->
            clazz.metaClass.static.$static_methodMissing = { name, args ->
                def matcher = name =~ PATTERN
                if (!matcher) {
                    throw new MissingMethodException(name, clazz, args, true)
                }
                def amount = Utils.matchOrNull(matcher, 1)
                def order = Utils.matchOrNull(matcher, 2) ?: "Desc"
                def field = matcher.group(3).toLowerCase()
                println "$name $amount $order $field"
                return database.orderBy("$field $order ${amount ? "LIMIT $amount" : ""}").results(clazz)
            }
            clazz.declaredFields.each { Field field ->
                clazz.metaClass.static."findBy${StringUtils.capitalize(field.name)}" = { arg ->
                    return database.where("${field.name}=?", arg).first(clazz)
                }
                clazz.metaClass.static."findAllBy${StringUtils.capitalize(field.name)}" = { arg ->
                    return database.where("${field.name}=?", arg).results(clazz)
                }
                if (field.isAnnotationPresent(Id)) {
                    clazz.metaClass.static.find = { arg ->
                        return database.where("${field.name}=?", arg).first(clazz)
                    }
                }
            }
            clazz.metaClass.static.findAll = { -> return database.results(clazz) }
            clazz.metaClass.save = { -> database.upsert(delegate) }
            clazz.metaClass.cache = { flush -> delegate.CACHE.cache(delegate, flush) }
            clazz.metaClass.cache = { -> delegate.cache(false) }
            clazz.metaClass.delete = { -> database.delete(delegate) }
        }
    }


}
