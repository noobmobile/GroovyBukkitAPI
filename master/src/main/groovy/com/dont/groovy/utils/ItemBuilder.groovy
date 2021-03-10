package com.dont.groovy.utils

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.util.function.Consumer

class ItemBuilder {

    private ItemStack item

    ItemBuilder(Material material, int amount, int data) {
        this.item = new ItemStack(material, amount, (short) data)
    }

    ItemBuilder(Material material, int amount) {
        this(material, amount, 0)
    }

    ItemBuilder(Material material) {
        this(material, 1)
    }

    ItemBuilder(ItemStack item) {
        this.item = item
    }

    ItemBuilder(String url) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3)
        if (url == null || url.isEmpty()) {
            this.item = skull
            return
        }
        if (url.length() <= 16) {
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta()
            skullMeta.setOwner(url)
            skull.setItemMeta(skullMeta)
            this.item = skull
            return
        }
        if (!url.startsWith("http://textures.minecraft.net/texture/"))
            url = "http://textures.minecraft.net/texture/" + url
        try {
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta()
            GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(url.getBytes()), null)
            profile.getProperties().put("textures", new Property("textures", new String(Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes()))))
            Field profileField = skullMeta.getClass().getDeclaredField("profile")
            profileField.setAccessible(true)
            profileField.set(skullMeta, profile)
            skull.setItemMeta(skullMeta)
        } catch (Exception e) {
            e.printStackTrace()
        }
        this.item = skull
    }

    ItemBuilder compose(Consumer<ItemMeta> composer) {
        ItemMeta itemMeta = item.getItemMeta()
        composer.accept(itemMeta)
        item.setItemMeta(itemMeta)
        return this
    }

    ItemBuilder icompose(Consumer<ItemStack> composer) {
        composer.accept(item)
        return this
    }

    ItemBuilder setType(Material type) {
        return icompose({ it -> it.setType(type) })
    }

    ItemBuilder setAmount(int amount) {
        return icompose({ it -> it.setAmount(amount) })
    }

    ItemBuilder setDurability(int data) {
        return icompose({ it -> it.setDurability((short) data) })
    }

    ItemBuilder setName(String name) {
        if (name == null || name.equals("nulo")) return this
        return compose({ meta -> meta.setDisplayName(translate(name)) })
    }

    ItemBuilder setLore(List<String> lore) {
        if (lore == null || lore.isEmpty() || lore.get(0).equals("nulo")) return this
        return compose({ meta -> meta.setLore(lore.collect { translate(it) }) })
    }

    ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore))
    }

    ItemBuilder addLore(List<String> lore) {
        if (lore == null || lore.isEmpty() || lore.get(0).equals("nulo")) return this
        return setLore(item.getItemMeta().hasLore() ? concate(item.getItemMeta().getLore(), lore) : lore)
    }

    ItemBuilder addLore(String... lore) {
        return addLore(Arrays.asList(lore))
    }

    ItemBuilder addGlow(boolean glow) {
        if (!glow) return this
        return compose({ meta ->
            meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        })
    }

    ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        return icompose({ it -> it.addUnsafeEnchantment(enchantment, level) })
    }

    ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        return icompose({ it -> it.addUnsafeEnchantments(enchantments) })
    }

    ItemBuilder addItemFlag(ItemFlag... itemFlags) {
        return compose({ meta -> meta.addItemFlags(itemFlags) })
    }

    ItemBuilder setSkullOwner(String owner) {
        return compose({ meta ->
            SkullMeta skullMeta = (SkullMeta) meta
            skullMeta.setOwner(owner)
        })
    }

    ItemBuilder editPlaceholders(Map<String, Object> placeholders) {
        return icompose({ this.item = it.editPlaceholders(placeholders) })
    }

    ItemBuilder setNbt(String key, String value) {
        try {
            Object nmsCopy = ReflectionUtils.getCBClass("inventory", "CraftItemStack").getMethod("asNMSCopy", ItemStack.class).invoke(null, item)
            Object nbtTagCompound = ReflectionUtils.getNMSClass("NBTTagCompound").getConstructor().newInstance()
            boolean b = nmsCopy.getClass().getMethod("getTag").invoke(nmsCopy) != null
            Object nbtTag = b ? nmsCopy.getClass().getMethod("getTag").invoke(nmsCopy) : nbtTagCompound
            Constructor nbsString = ReflectionUtils.getNMSClass("NBTTagString").getConstructor(String.class)
            nbtTag.getClass().getMethod("set", String.class, ReflectionUtils.getNMSClass("NBTBase"))
                    .invoke(nbtTag, key, nbsString.newInstance(value))
            nmsCopy.getClass().getMethod("setTag", ReflectionUtils.getNMSClass("NBTTagCompound")).invoke(nmsCopy, nbtTag)
            this.item = (ItemStack) ReflectionUtils.getCBClass("inventory", "CraftItemStack").getMethod("asBukkitCopy", ReflectionUtils.getNMSClass("ItemStack"))
                    .invoke(null, nmsCopy)
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            e.printStackTrace()
        }
        return this
    }

    static Optional<String> getNbt(ItemStack item, String key) {
        String value = null
        if (item != null && item.getType() != Material.AIR) {
            try {
                Object nmsCopy = ReflectionUtils.getCBClass("inventory", "CraftItemStack").getMethod("asNMSCopy", ItemStack.class).invoke(null, item)
                if (nmsCopy.getClass().getMethod("getTag").invoke(nmsCopy) != null) {
                    Object tagCompound = nmsCopy.getClass().getMethod("getTag").invoke(nmsCopy)
                    value = (String) tagCompound.getClass().getMethod("getString", String.class).invoke(tagCompound, key)
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace()
            }
        }
        return value == null || value.isEmpty()
                ? Optional.empty() : Optional.of(value)
    }

    ItemStack build() {
        return item
    }

    ItemStack toItemStack() {
        return item
    }

    private <T> List<T> concate(List<T> a, List<T> b) {
        List<T> ts = new ArrayList<>(a)
        ts.addAll(b)
        return ts
    }

    private String translate(String toTranslate) {
        return ChatColor.translateAlternateColorCodes('&' as char, toTranslate)
    }

    Object asType(Class clazz) {
        if (clazz == ItemStack) {
            return build()
        }
    }

}
