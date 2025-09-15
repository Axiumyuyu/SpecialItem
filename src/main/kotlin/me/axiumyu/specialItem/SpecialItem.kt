package me.axiumyu.specialItem

import me.axiumyu.specialItem.commands.SpecialItemCommand
import me.axiumyu.specialItem.items.impl.IronElevator
import me.axiumyu.specialItem.items.impl.ItemMagnet
import me.axiumyu.specialItem.items.impl.WindStaff
import me.yic.xconomy.api.XConomyAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin

class SpecialItem : JavaPlugin() {
    companion object {
        lateinit var instance: SpecialItem
            private set
        val mm: MiniMessage = MiniMessage.miniMessage()

        lateinit var xc : XConomyAPI
    }

    override fun onEnable() {
        instance = this
        xc = XConomyAPI()
        logger.info("SpecialItems Plugin is enabling...")

        // 1. 注册所有自定义的特殊物品
        // 管理器会自动将它们注册为监听器
        ItemManager.register(WindStaff())
        ItemManager.register(IronElevator())
        ItemManager.register(ItemMagnet())

        // 2. 注册命令处理器
        val command = getCommand("specialitem")
        if (command != null) {
            val commandHandler = SpecialItemCommand()
            command.setExecutor(commandHandler)
            command.tabCompleter = commandHandler
            logger.info("Command 'specialitem' has been registered.")
        } else {
            logger.severe("Could not register command 'specialitem'. Is it in plugin.yml?")
        }

        // 注意：ItemListener 已被移除，不再需要注册
        logger.info("SpecialItems Plugin has been enabled successfully!")
    }

    override fun onDisable() {
        ItemManager.unregisterAll()
        logger.info("SpecialItems Plugin has been disabled.")
    }
}