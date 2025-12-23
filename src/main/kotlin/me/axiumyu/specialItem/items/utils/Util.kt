package me.axiumyu.specialItem.items.utils

import me.axiumyu.specialItem.SpecialItem.Companion.mm
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

object Util {
    fun ItemStack.changeLastLore(lore: Component) {
        val lore1 = this.lore()?.toMutableList() ?: mutableListOf()
        if (lore1.isEmpty()) throw IllegalStateException("ItemStack has no lore")
        lore1.removeAt(lore1.lastIndex)
        lore1.add(lore)
        this.lore(lore1)
    }

    fun ItemStack.changeLastLore(lore: String) {
        this.changeLastLore(mm.deserialize(lore))
    }
}