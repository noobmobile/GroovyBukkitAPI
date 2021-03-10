package com.dont.groovy.models.bukkit


import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player

import java.util.function.Function
import java.util.function.Predicate

class Context {

    CommandSender sender
    String[] args
    FileConfiguration i18n

    CommandSender sender() {
        return sender
    }

    Player player() throws ContextException {
        if (sender instanceof Player) {
            return (Player) sender
        } else {
            throw new ContextException(i18n.commands.onlyPlayer)
        }
    }

    void returning(String... returning) throws ContextException {
        throw new ContextException(returning)
    }

    int parseInt(int index) throws ContextException {
        try {
            getArg(index) as int
        } catch (ignored) {
            throw new ContextException(String.format(i18n.commands.invalidNumber, getArg(index)))
        }
    }

    double parseDouble(int index) throws ContextException {
        try {
            getArg(index) as Double
        } catch (ignored) {
            throw new ContextException(String.format(i18n.commands.invalidNumber, getArg(index)))
        }
    }

    def <T extends Enum<T>> T parseEnum(int index, Class<T> clazz) throws ContextException {
        try {
            Enum.valueOf(clazz, getArg(index).toUpperCase())
        } catch (ignored) {
            throw new ContextException(String.format(i18n.commands.invalidType, getArg(index)))
        }
    }


    def <T> T parseT(Collection<T> collection, Function<T, Object> predicate, Object filter, String type) throws ContextException {
        try {
            collection
                    .findAll { test(predicate.apply(it), filter) }
                    .first()
        } catch (ignored) {
            throw new ContextException(String.format(i18n.commands.invalidType, filter))
        }
    }

    def <T> T parseT(Collection<T> collection, Predicate<T> predicate, String type) throws ContextException {
        try {
            collection
                    .findAll { predicate }
                    .first()
        } catch (ignored) {
            throw new ContextException(String.format(i18n.commands.invalidType, type))
        }
    }

    Player parsePlayer(int index) throws ContextException {
        Player target = Bukkit.getPlayer(getArg(index))
        if (target == null) {
            throw new ContextException(String.format(i18n.commands.invalidPlayer, getArg(index)))
        }
        return target
    }

    private boolean test(Object o1, Object o2) {
        return o1 instanceof String && o2 instanceof String ? ((String) o1).equalsIgnoreCase((String) o2) : o1.equals(o2)
    }

    boolean hasPermission(String permission) {
        sender.hasPermission(permission)
    }

    void permission(String permission) throws ContextException {
        if (!sender.hasPermission(permission)) {
            throw new ContextException(i18n.commands.noPermission)
        }
    }

    def <T> String validTypes(Collection<T> collection, Function<T, String> extractor) {
        collection
                .collect { extractor }
                .join(", ")
    }

    String validTypes(Class<? extends Enum> clazz) {
        clazz.getEnumConstants()
                .collect { it.name() }
                .collect { it.toLowerCase() }
                .join(", ")
    }

    int argsLenght() {
        return args.length
    }

    String getArg(int index) {
        return args[index]
    }

    boolean hasArg(int index) {
        return args.length > index
    }

    boolean hasArgs(int size) {
        return args.length >= size
    }

}
