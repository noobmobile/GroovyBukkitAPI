package com.dont.groovy.utils


import com.dont.groovy.models.annotations.Inject
import org.bukkit.Bukkit

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

class ReflectionUtils {

    private static final String VERSION = Bukkit.getServer().getClass().getName().split("\\.")[3]
    private static final HashMap<String, Class<?>> CLASSES = new HashMap<>()

    static Class<?> getNMSClass(String name) {
        if (!CLASSES.containsKey(name)) {
            try {
                CLASSES.put(name, Class.forName("net.minecraft.server." + VERSION + "." + name))
            } catch (ClassNotFoundException e) {
                e.printStackTrace()
            }
        }
        return CLASSES.get(name)
    }

    static Class<?> getCBClass(String prefix, String name) {
        if (!CLASSES.containsKey(prefix + "." + name)) {
            try {
                CLASSES.put(prefix + "." + name, Class.forName("org.bukkit.craftbukkit." + VERSION + "." + prefix + "." + name))
            } catch (ClassNotFoundException e) {
                e.printStackTrace()
            }
        }
        return CLASSES.get(prefix + "." + name)
    }

    static def hasField(clazz, name) {
        try {
            clazz.getDeclaredField(name)
            return true
        } catch (any) {
            return false
        }
    }

    static void setField(clazz, object, fieldName, value) {
        try {
            Field field = clazz.getDeclaredField(fieldName)
            boolean old = field.isAccessible()
            field.setAccessible(true)
            field.set(object, value)
            field.setAccessible(old)
        } catch (ignored) {
        }
    }

    static void invokeMethod(clazz, object, methodName, Object... args) {
        try {
            Method method = clazz.getDeclaredMethod(methodName)
            boolean old = method.isAccessible()
            method.setAccessible(true)
            method.invoke(object, args)
            method.setAccessible(old)
        } catch (ignored) {
        }
    }

    // 20 libraries e tive que fazer na mão, desgraça
    static def discoverClasses(instance) {
        def packageName = instance.getClass().getPackage().getName().replaceAll("\\.", "/")
        def jarFile = new JarInputStream(new FileInputStream(instance.getFile()))
        JarEntry jarEntry
        def classes = []
        while (true) {
            jarEntry = jarFile.getNextJarEntry()
            if (!jarEntry) break
            if (jarEntry.name.startsWith(packageName) && jarEntry.name.endsWith(".class")) {
                def clazz = Class.forName(jarEntry.name.replace(".class", "").replaceAll("/", "\\."))
                if (clazz.isAnnotationPresent(Inject)) classes.add(clazz)
            }
        }
        classes
    }

}
