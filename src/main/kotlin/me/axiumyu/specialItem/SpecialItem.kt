package me.axiumyu.specialItem

import me.axiumyu.specialItem.commands.SpecialItemCommand
import me.axiumyu.specialItem.items.impl.ExperienceStorge
import me.axiumyu.specialItem.items.impl.IronElevator
import me.axiumyu.specialItem.items.impl.ItemMagnet
import me.axiumyu.specialItem.items.impl.WindStaff
import me.yic.xconomy.api.XConomyAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin

class SpecialItem : JavaPlugin() {
    companion object {
        val plugin by lazy { getPlugin(SpecialItem::class.java) }

        @JvmField
        val mm = MiniMessage.miniMessage()

        val xc by lazy { XConomyAPI() }
    }

    override fun onEnable() {
        // 1. 注册所有自定义的特殊物品
        // 管理器会自动将它们注册为监听器
        listOf(WindStaff, IronElevator, ItemMagnet, ExperienceStorge).forEach {
            ItemManager.register(it)
        }

        // 2. 注册命令处理器
        getCommand("specialitem")?.apply {
            tabCompleter = SpecialItemCommand
            setExecutor(SpecialItemCommand)
        }
    }

    override fun onDisable() {
        ItemManager.unregisterAll()
    }
}