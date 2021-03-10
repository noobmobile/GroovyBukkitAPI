# GroovyBukkitAPI

GroovyBukkitAPI it's, as its name suggets, a (grails-inspired) api for creating bukkit-plugins with groovy.

# Project
## Main module
The main module it's where it is all the dependencies shadowed, abstraction and dependency injection. You don't need to modify it, it's already compiled in jar. You need to have it in your `/plugins` folder because this is where the Groovy classes are, so you will have at least two jars: `GroovyBukkitAPI.jar` (main module) and `GroovyMyPlugin.jar` (plugin module). Of course you can create multiple plugins modules (`GroovyMyPlugin2.jar`, `GroovyMyPlugin3.jar`), but just need one main module.
### Dependencies shadowed
- Groovy
- [nOrm](https://github.com/dieselpoint/norm)
- [HikariCP](https://github.com/brettwooldridge/HikariCP)
## The plugin module
This it's where are the implementations and the code you will write. You need to create the `Main` class and points it in the plugin.yml, the Main class must extends `AbstractTerminal`.
### The Main class

```java
@Inject(initialize = false)
class Terminal extends AbstractTerminal {

    // called, obliviously, before the onEnable()
    // here you can modify some settings of GroovyBukkitAPI
    @Override
    void preEnable() { 
        simpleLogger.prefix = "§6[${getName()}] §f"
        simpleLogger.debugPrefix = "[DEBUG] §6${getName()} §8"
        customInject("getPlayer", { String it -> Bukkit.getPlayer(it) })
    }

    @Override
    void enable() { // called on onEnable() from JavaPlugin
    
        
    }

    @Override
    void disable() { // called on onDisable() from JavaPlugin

    }
}
```
### [Services](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Services)
Before starting, it's better to note that everyting it's considered a `Service`, there's no **practical** distinction between `Command`, `Listener`, `Service` or whatever (just to make our lives easier). Every service, therefore every class, must contains the `@Inject` annotation. This annotation it's the responsible for injecting dependencies and instantiating our classes. So you just need to annotate `@Inject` and will be instantiated, registered and injected everywhere its called.
#### [Pre-injected methods and fields](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Injections)
- **every field ending with `Service` of yours will be automatically instantiated and injected with the corresponding service.**
- `main` - every field with the name of main it will be passed the main instance, so no more `Main.getPlugin(Main.class)`
- `log()` and `debug()` - log messages to console using our custom logger
- `sync()` and `async()` - change from threads using `BukkitSchedule` methods (yeah, no more `Bukkit.getScheduler().runTaskAsynchronously()`)
- you can inject your own fields and methods using `customInject()`
### [Creating commands](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Commands)
Classes containing commands must have the `Command` suffix and that's all, you're done to start your commands.
```java
@Inject
class TestCommand {

    Terminal main
    ItemService itemService // an example service, it'll automatically injected and registered on our main
    
    // our command will be registered as /give
    def give = { Context context ->
        // when we call the Context#player() method, an exception will be throwed if it's not a player that it's executing the command
        // but don't worry, the exception it's treated and a nice message it's sent to the Console saying it's only for players
        def player = context.player()
        if (!context.hasArgs(2)){ // if there's two or more arguments
            return "§cPlease, use /give <player> <item> <amount>" // we can also use context.returning(String... messages)
        }
        def target = context.parsePlayer(0) // parse the player on args[0]
        def material = context.parseEnum(1, Material) // parse an enum on args[1]
        def amount = context.hasArg(2) ? context.parseInt(2) : 1
        target.getInventory(new ItemComposer(material, amount).build())
        return "§aItem given with success to $target.name"
    }
    
}
```
### [Creating listeners](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Listeners)
Classes listening to events must contain the suffix Listener
```java
@Inject
class TestListener {

    // the variable's name doesn't matters
    def onJoin = { PlayerJoinEvent event ->
        event.player.sendMessage("works as normal listener")
    }
    
    // wants to change the listener's priority, or change the ignoreCancelled? no problem
    @EventSetting(priority = EventPriority.HIGH)
    def onBreak = { BlockBreakEvent event ->
        event.player.sendMessage("high priority listener")
    }

}
```
### [Configurations](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Configurations)
As usually, every field with the type of `FileConfiguration` will be instantiated and a yml file will be generated with the variable's name. GroovyBukkitAPI will inject the fields of the yml in the `FileConfiguration` object.
```yaml
messages:
  message1: "&6Hey You"
  messageList:
    - "&4 that's a list"
  number: 42
```
```java
    FileConfiguration settings // a settings.yml will be generated

    def test = { Context context ->
        def sender = context.sender()
        sender.sendMessage(settings.messages.message1)
        sender.sendMessage(settings.messages.messageList)
        sender.sendMessage("My favorite number is ${settings.messages.number}")
    }
```
```yaml
items:
  it1:
    Material: DIAMOND_SWORD
    Name: "&6Powerful Sword"
    Lore:
      - "&6Really Cool Sword"
  it2:
    Texture: LurionK
    Name: "&6LurionK's head"
    Enchantments:
      - "DURABILITY: 1"
  it3:
    Material: STONE
    Data: 1
    Amount: 16
```
```java
    FileConfiguration teste // teste.yml

    def test = { Context context ->
        def player = context.player()
        player.getInventory().addItem(teste.items.it1 as ItemStack) // automatically cast the teste.items.it1 as a ItemStack
        log(teste.items.asList(ItemStack)) // returns a List of ItemStack with it1, it2 and it3
    }
```
### [Inventories](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Inventories)
Tired of creating inventories using `Bukkit.createInventory()` and creating listeners to `InventoryClickEvent`? GroovyBukkitAPI provides you a great solution. 
```java
// or new InventoryHandler(name, size)
def player = ...
menu(name, size)
	.item(10, new ItemBuilder(Material.STONE), {player -> player.sendMessage("clicked on item STONE on slot 10")})
	.item(new ItemBuilder(Material.DIAMOND), {player -> player.sendMessage("click on item DIAMOND on slot coming from config")})
	.items([item1, item2, item3])
	.handler({InventoyClickEvent event ->
		event.getWhoClicked().sendMessage("custom event handling")
	})
	.open(player)
```
### [Databases](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Database)
You can see our [utilities classes](https://github.com/noobmobile/GroovyBukkitAPI/wiki/Utils) or our [full wiki](https://github.com/noobmobile/GroovyBukkitAPI/wiki) for more details.
