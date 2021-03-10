package com.dont.groovy

import com.dont.groovy.utils.Logger
import com.dont.groovy.utils.ReflectionUtils
import com.dont.groovy.utils.TimeUtils
import com.dont.groovy.utils.Utils
import com.dont.groovy.utils.meta.MetaInjector
import com.dont.groovy.utils.meta.database.MetaDatabaseInjector
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

import java.nio.charset.StandardCharsets

abstract class AbstractTerminal extends JavaPlugin {

    Map<Class<?>, ?> services = [:]
    Map<String, Object> injects = [:]
    Map<String, YamlConfiguration> configs = [:]
    Logger simpleLogger
    List<Class> clazzes
    List<Closure> enables = [], disables = []
    static def instance

    void onEnable() {
        instance = this
        simpleLogger = new Logger(this)
        clazzes = ReflectionUtils.discoverClasses(instance)
        preEnable()
        simpleLogger.log("enabled")
        def time = TimeUtils.measureTime({ startInject(); enable() })
        enables.each { it() }
        simpleLogger.log("enabled in ${time}ms")
    }

    void onDisable() {
        simpleLogger.log("disabled")
        disables.each { it() }
        disable();
    }

    abstract void preEnable()

    abstract void enable()

    abstract void disable()

    void useDatabase() {
        MetaDatabaseInjector.injectDatabase(instance)
    }

    void customInject(methodName, method) {
        injects.put(methodName, method)
    }

    void startInject() {
        if (getClass() == Terminal) return
        MetaInjector.defaultInjects(instance)
        MetaInjector.injectMeta(clazzes, instance)
    }

    def registerService(Class<?> serviceClass) {
        if (services.containsKey(serviceClass)) return services.get(serviceClass)
        simpleLogger.debug("instantiating service ${serviceClass.getSimpleName()}")
        services.put(serviceClass, serviceClass.getConstructor().newInstance())
        return services.get(serviceClass)
    }

    def registerConfig(String configName) {
        configName = configName.toLowerCase().replace("_" as char, File.separatorChar) + (configName.endsWith(".yml") ? "" : ".yml")
        if (configs.containsKey(configName)) return configs.get(configName)
        def config = generateConfig(configName)
        configs.put(configName, config)
        if (configName.equalsIgnoreCase("i18n.yml")) {
            Utils.setupDefaultI18n(config)
        } else if (configName.equalsIgnoreCase("database.yml")) {
            Utils.setupDefaultDb(config)
        }
        MetaInjector.injectConfigFields(config)
        simpleLogger.debug("creating config ${configName}")
        return configs.get(configName)
    }

    private def generateConfig(configName) {
        def configFile = new File(instance.dataFolder.toString() + File.separatorChar + configName)
        if (!configFile.exists()) {
            try {
                instance.saveResource(configName, false)
            } catch (any) {
                configFile.createNewFile()
                simpleLogger.debug("config $configName not found, generating a blank one")
            }
        }
        def config = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))
        InputStream is = instance.getResource(configName)
        if (is != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(is)
            config.setDefaults(defConfig)
        }
        config.metaClass.file = configFile
        return config
    }

    def getClassLoaderBypass() {
        return classLoader
    }

    static <T extends AbstractTerminal> T getInstance() {
        instance
    }

}
