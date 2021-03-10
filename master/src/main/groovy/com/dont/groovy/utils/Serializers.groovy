package com.dont.groovy.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

import java.lang.reflect.Constructor
import java.util.concurrent.ThreadLocalRandom

class Serializers {

    static class Locations {
        static String serializeLocation(Location location) {
            def isBlock = location.getX() == location.getBlockX() && location.getY() == location.getBlockY() && location.getZ() == location.getBlockZ()
            return serializeLocation(location, !isBlock)
        }

        static String serializeLocation(Location location, boolean full) {
            if (location == null) return null
            if (full) {
                return location.getX() + ";" + location.getY() + ";" + location.getZ() + ";" + location.getYaw() + ";" + location.getPitch() + ";" + location.getWorld().getName()
            } else {
                return location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ() + ";" + location.getWorld().getName()
            }
        }

        static Location deserializeLocation(String serialized) {
            if (serialized == null || !serialized.contains(";")) return null
            String[] parts = serialized.split(";")
            double x = Double.parseDouble(parts[0])
            double y = Double.parseDouble(parts[1])
            double z = Double.parseDouble(parts[2])
            if (parts.length == 6) { // full
                float yaw = Float.parseFloat(parts[3])
                float pitch = Float.parseFloat(parts[4])
                World w = Bukkit.getServer().getWorld(parts[5])
                return new Location(w, x, y, z, yaw, pitch)
            } else {
                World w = Bukkit.getServer().getWorld(parts[3])
                return new Location(w, x, y, z)
            }
        }

    }

    static class Item {
        static ItemStack deserializeItem(String deserialized) {
            String[] splitted = deserialized.split(CUSTOM_SEPARATOR)
            ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(splitted[0], 32).toByteArray())
            DataInputStream dataInputStream = new DataInputStream(inputStream)

            ItemStack itemStack = null
            try {
                Class<?> nbtTagCompoundClass = ReflectionUtils.getNMSClass("NBTTagCompound")
                Class<?> nmsItemStackClass = ReflectionUtils.getNMSClass("ItemStack")
                Object nbtTagCompound = ReflectionUtils.getNMSClass("NBTCompressedStreamTools").getMethod("a", DataInputStream.class).invoke(null, dataInputStream)
                Object craftItemStack = nmsItemStackClass.getMethod("createStack", nbtTagCompoundClass).invoke(null, nbtTagCompound)
                itemStack = (ItemStack) ReflectionUtils.getCBClass("inventory", "CraftItemStack").getMethod("asBukkitCopy", nmsItemStackClass).invoke(null, craftItemStack)
            } catch (Throwable e) {
                e.printStackTrace()
            }

            if (splitted.length == 2) {
                def custom = splitted[1]
                def customSplitted = custom.split(CUSTOM_INSIDE_SEPARATOR)
                itemStack.metaClass.getCommands = { -> customSplitted[2].split(LIST_SEPARATOR) }
                itemStack.metaClass.getChance = { -> customSplitted[1] as double }
                itemStack.metaClass.getSlot = { -> customSplitted[0] as int }
                itemStack.metaClass.hasCommands = { -> return !!delegate.commands }
                itemStack.metaClass.hasChance = { -> return !!delegate.chance }
                itemStack.metaClass.hasSlot = { -> return !!delegate.slot }
                itemStack.metaClass.test = { ->
                    return delegate.chance >= 100 || (ThreadLocalRandom.current().nextDouble() * 100) <= delegate.chance
                }
                itemStack.metaClass.execute = { Player player ->
                    if (delegate.hasCommands()) {
                        delegate.getCommands().each {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("\$player", player.name))
                        }
                    } else {
                        player.getInventory().addItem(delegate)
                    }
                }
            }

            return itemStack
        }

        static String serializeItem(ItemStack item) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
            DataOutputStream dataOutput = new DataOutputStream(outputStream)

            try {
                Class<?> nbtTagCompoundClass = ReflectionUtils.getNMSClass("NBTTagCompound")
                Constructor<?> nbtTagCompoundConstructor = nbtTagCompoundClass.getConstructor()
                Object nbtTagCompound = nbtTagCompoundConstructor.newInstance()
                Object nmsItemStack = ReflectionUtils.getCBClass("inventory", "CraftItemStack").getMethod("asNMSCopy", ItemStack.class).invoke(null, item)
                ReflectionUtils.getNMSClass("ItemStack").getMethod("save", nbtTagCompoundClass).invoke(nmsItemStack, nbtTagCompound)
                ReflectionUtils.getNMSClass("NBTCompressedStreamTools").getMethod("a", nbtTagCompoundClass, DataOutput.class).invoke(null, nbtTagCompound, (DataOutput) dataOutput)
            } catch (Throwable e) {
                e.printStackTrace()
            }
            String serialized = new BigInteger(1, outputStream.toByteArray()).toString(32)
            def customSerialized = ""
            if (item.hasProperty("chance")) {
                def custom = new StringBuilder()
                custom.append(item.hasProperty("slot") ? item.slot : 0)
                custom.append(CUSTOM_INSIDE_SEPARATOR)
                custom.append(item.hasProperty("chance") ? item.chance : 0)
                custom.append(CUSTOM_INSIDE_SEPARATOR)
                custom.append(item.hasProperty("commands") ? item.commands.join(LIST_SEPARATOR) : "")
                customSerialized = CUSTOM_SEPARATOR + custom.toString()
            }
            return serialized + customSerialized
        }

        static final CUSTOM_SEPARATOR = "</>"
        static final LIST_SEPARATOR = "<,>"
        static final CUSTOM_INSIDE_SEPARATOR = ";"

    }


}
