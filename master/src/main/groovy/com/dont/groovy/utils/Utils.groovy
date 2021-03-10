package com.dont.groovy.utils

import org.bukkit.configuration.file.YamlConfiguration

class Utils {

    static def matchOrNull(def matcher, int i) {
        try {
            return matcher.group(i)
        } catch (any) {
            return null
        }
    }

    static void setupDefaultI18n(YamlConfiguration config) {
        Map<String, String> defaults = [:]
        defaults.put("commands.onlyPlayer", "&cThis command is only for players.")
        defaults.put("commands.noPermission", "&cYou don't have permission to do this.")
        defaults.put("commands.invalidNumber", "&cNumber &f\"%s\"&c is invalid.")
        defaults.put("commands.invalidType", "&cType &f\"%s\"&c is invalid.")
        defaults.put("commands.invalidPlayer", "&cPlayer &f\"%s\"&c is invalid or is not online.")
        defaults.each { if (!config.isSet(it.key)) config.set(it.key, it.value) }
        config.save(config.file)
    }

    static void setupDefaultDb(YamlConfiguration config) {
        Map<String, String> defaults = [:]
        defaults.put("database.type", "SQLITE")
        defaults.put("database.host", "localhost:3306")
        defaults.put("database.user", "admin")
        defaults.put("database.password", "")
        defaults.put("database.database", "test")
        defaults.each { if (!config.isSet(it.key)) config.set(it.key, it.value) }
        config.save(config.file)
    }

    static String replace(String string, Map<String, Object> placeholders) {
        String temp = string;
        placeholders.each {
            temp = fastReplace(temp, it.key, it.value.toString())
        }
        return temp;
    }
    /*apparently this is faster than java String.replace*/

    static String fastReplace(String str, String target, String replacement) {
        int targetLength = target.length();
        if (targetLength == 0) {
            return str;
        }
        int idx2 = str.indexOf(target);
        if (idx2 < 0) {
            return str;
        }
        StringBuilder buffer = new StringBuilder(targetLength > replacement.length() ? str.length() : str.length() * 2);
        int idx1 = 0;
        while ({
            buffer.append(str, idx1, idx2);
            buffer.append(replacement);
            idx1 = idx2 + targetLength;
            idx2 = str.indexOf(target, idx1);
            (idx2 > 0)
        }()) continue
        buffer.append(str, idx1, str.length());
        return buffer.toString();
    }

}
