package com.dont.groovy.utils

import com.dont.groovy.Terminal
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

/**
 * wasn't wanting to redo this class in Groovy
 */
class InventoryHandler {

    static {
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onClick(InventoryClickEvent event) {
                if (event.getSlotType() == InventoryType.SlotType.OUTSIDE
                        || event.getCurrentItem() == null) return;
                if (event.getInventory().getHolder() instanceof InventoryHandler.HandlerHolder) {
                    def holder = (InventoryHandler.HandlerHolder) event.getInventory().getHolder()
                    def inventoryHandler = holder.inventoryHandler;
                    event.setCancelled(true);
                    def slot = event.slot
                    def player = (Player) event.getWhoClicked();
                    if (inventoryHandler.actions.containsKey(slot)) {
                        inventoryHandler.actions.get(slot)(player)
                    } else if (inventoryHandler.itemsSlots.contains(slot)
                            && inventoryHandler.itemConsumer != null) {
                        inventoryHandler.itemConsumer(event.getCurrentItem(), player)
                    } else if (inventoryHandler.scroller && slot == inventoryHandler.scrollerNextPageSlot) {
                        if (inventoryHandler.hasPage(holder.getPage() + 1)) {
                            inventoryHandler.open(player, holder.getPage() + 1)
                        }
                    } else if (inventoryHandler.scroller && slot == inventoryHandler.scrollerPreviousPageSlot) {
                        if (inventoryHandler.hasPage(holder.getPage() - 1)) {
                            inventoryHandler.open(player, holder.getPage() - 1)
                        }
                    } else if (inventoryHandler.handler != null) {
                        inventoryHandler.handler(event)
                    }
                }
            }
        }, Terminal.getInstance());
    }

    private static final List<Integer> CENTER = Arrays.asList(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34)

    String name
    int size
    Map<Integer, Closure> actions
    Map<Integer, ItemStack> items
    Closure handler
    Inventory inventory
    Closure itemConsumer
    List<Integer> itemsSlots

    boolean scroller
    List<ItemStack> scrollerItems
    int scrollerPreviousPageSlot, scrollerNextPageSlot
    ItemStack scrollerPreviousPageItem, scrollerNextPageItem
    Map<Integer, Inventory> pages

    InventoryHandler(String name, int size) {
        this.name = name
        this.size = size
        this.actions = new HashMap<>()
        this.items = new HashMap<>()
        this.scrollerPreviousPageSlot = 18
        this.scrollerNextPageSlot = 26
        this.itemsSlots = CENTER
        this.scrollerPreviousPageItem = getPageItem(true)
        this.scrollerNextPageItem = getPageItem(false)
        this.inventory = Bukkit.createInventory(new HandlerHolder(this), size, name)
    }

    InventoryHandler item(int slot, ItemStack itemStack, Closure consumer) {
        inventory.setItem(slot, itemStack)
        this.items.put(slot, itemStack)
        if (consumer != null) this.actions.put(slot, consumer)
        return this
    }

    InventoryHandler item(int slot, ItemStack itemStack) {
        return item(slot, itemStack, null)
    }

    InventoryHandler item(ItemStack itemStack) {
        return item(itemStack.slot, itemStack, null)
    }

    InventoryHandler item(ItemStack itemStack, Closure consumer) {
        return item(itemStack.slot, itemStack, consumer)
    }

    InventoryHandler scroller() {
        this.scroller = true
        return this
    }

    InventoryHandler scrollerPageSlots(int previous, int next) {
        if (previous) this.scrollerPreviousPageSlot = previous
        if (next) this.scrollerNextPageSlot = next
        return this
    }

    InventoryHandler scrollerPageItems(ItemStack previous, ItemStack next) {
        if (previous) this.scrollerPreviousPageItem = previous
        if (next) this.scrollerNextPageItem = next
        return this
    }

    InventoryHandler slots(List<Integer> slots) {
        this.itemsSlots = slots
        return this
    }

    InventoryHandler slots(Integer... slotsList) {
        return slots(Arrays.asList(slotsList))
    }

    InventoryHandler itemConsumer(Closure consumer) {
        this.itemConsumer = consumer
        return this
    }

    InventoryHandler items(List<ItemStack> items, Closure consumer) {
        if (consumer != null) this.itemConsumer = consumer
        if (scroller) {
            this.scrollerItems = items
            buildScroller()
            return this
        }
        int lastIndex = 0
        for (int i = 0; i < inventory.getSize(); i++) {
            if (!itemsSlots.contains(i)) continue
            if (lastIndex >= items.size()) break
            inventory.setItem(i, items.get(lastIndex))
            lastIndex++
        }
        return this
    }

    InventoryHandler items(List<ItemStack> itemList) {
        return items(itemList, (Closure) null)
    }

    InventoryHandler items(List<ItemStack> itemList, ItemStack orElse, Closure consumer) {
        return items(itemList.isEmpty() ? Collections.singletonList(orElse) : itemList, consumer)
    }

    InventoryHandler items(List<ItemStack> itemList, ItemStack orElse) {
        return items(itemList, orElse, null)
    }

    InventoryHandler handler(Closure handler) {
        this.handler = handler
        return this
    }

    private void buildScroller() {
        pages = new HashMap<>()
        pages.put(1, inventory)
        if (scrollerItems.isEmpty()) {
            return
        }
        List<List<ItemStack>> lists = getPages(scrollerItems, itemsSlots.size())
        int page = 1
        for (List<ItemStack> list : lists) {
            Inventory inventory = Bukkit.createInventory(new HandlerHolder(this, page), size, name)
            int slot = 0
            for (ItemStack it : list) {
                inventory.setItem(itemsSlots.get(slot), it)
                slot++
            }
            items.forEach(inventory.&setItem)
            inventory.setItem(scrollerPreviousPageSlot, editItem(scrollerPreviousPageItem.clone(), page - 1))
            // se for a primeira página, não tem pra onde voltar
            inventory.setItem(scrollerNextPageSlot, editItem(scrollerNextPageItem.clone(), page + 1))

            pages.put(page, inventory)
            page++
        }
        pages.get(1).setItem(scrollerPreviousPageSlot, new ItemStack(Material.AIR))
        // vai na primeira página e remove a flecha de ir pra trás
        pages.get(pages.size()).setItem(scrollerNextPageSlot, new ItemStack(Material.AIR))
        // vai na última página e remove a flecha de ir pra frente
    }

    void open(Player player) {
        if (scroller) {
            open(player, 1)
        } else {
            player.openInventory(inventory)
        }
    }

    void open(Player player, int page) {
        if (scroller) {
            player.openInventory(pages.get(page))
        }
    }

    boolean hasPage(int page) {
        return pages.containsKey(page)
    }

    private ItemStack getPageItem(boolean back) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) (back ? 14 : 5))
        ItemMeta meta = item.getItemMeta()
        meta.setDisplayName("§aPage <page>")
        item.setItemMeta(meta)
        return item
    }

    def <T> List<List<T>> getPages(Collection<T> c, Integer pageSize) {
        // créditos a https://stackoverflow.com/users/2813377/pscuderi
        List<T> list = new ArrayList<T>(c)
        if (pageSize == null || pageSize <= 0 || pageSize > list.size()) pageSize = list.size()
        int numPages = (int) Math.ceil((double) list.size() / (double) pageSize)
        List<List<T>> pages = new ArrayList<List<T>>(numPages)
        for (int pageNum = 0; pageNum < numPages;)
            pages.add(list.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, list.size())))
        return pages
    }

    private ItemStack editItem(ItemStack item, int page) {
        ItemMeta meta = item.getItemMeta()
        meta.setDisplayName(item.getItemMeta().getDisplayName().replace("<page>", page + ""))
        item.setItemMeta(meta)
        return item
    }

    static class HandlerHolder implements InventoryHolder {
        InventoryHandler inventoryHandler
        int page

        HandlerHolder(InventoryHandler inventoryHandler, int page) {
            this.inventoryHandler = inventoryHandler
            this.page = page
        }

        HandlerHolder(InventoryHandler inventoryHandler) {
            this.inventoryHandler = inventoryHandler
            this.page = 1
        }

        int getPage() {
            return page
        }

        @Override
        Inventory getInventory() {
            return null
        }
    }

}
