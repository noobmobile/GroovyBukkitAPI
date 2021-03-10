package com.dont.groovy.utils

import com.dont.groovy.AbstractTerminal
import org.bukkit.Bukkit

/**
 por que um objeto para um logger invés de ser estático?
 bom, cada plugin precisa de um prefixo
 */
class Logger {

    AbstractTerminal instance
    def debugging = true
    def prefix, debugPrefix

    Logger(AbstractTerminal instance) {
        this.instance = instance
        this.prefix = "§1[${instance.getName()}] §b"
        this.debugPrefix = "§1[DEBUG] [${instance.getName()}] §3"
    }

    void log(message) {
        Bukkit.getConsoleSender().sendMessage("$prefix${message?.toString()}")
    }

    void debug(message) {
        if (debugging) {
            Bukkit.getConsoleSender().sendMessage("$debugPrefix${message?.toString()}")
        }
    }

}
