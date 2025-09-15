package me.axiumyu.specialItem.items.impl

import me.axiumyu.specialItem.SpecialItem.Companion.mm
import me.axiumyu.specialItem.items.SpecialItemBase
import me.axiumyu.specialItem.items.impl.IronElevator.Companion.coolDownList
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.Enchantment.POWER
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.collections.contains
import kotlin.math.sqrt
import kotlin.random.Random

class ItemMagnet : SpecialItemBase() {
    companion object {
        @JvmField
        val coolDownList = mutableMapOf<String, Long>()
    }
    override val id: String = "item_magnet"
    override val name: String = "物品磁铁"
    override val itemMaterial: Material = Material.BOWL
    override val description: String = """
        手持右键使用,可吸取一定范围内的物品
        海之眷顾等级代表吸取范围
        快速装填等级为冷却时间
    """.trimIndent()
    override val enchantmentRanges: Map<Enchantment, Pair<Int, Int>> = mapOf(
        Enchantment.LUCK_OF_THE_SEA to (1 to 10),
        Enchantment.QUICK_CHARGE to (90 to 1)
    )
    override val maxLevel: Int = 90

    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        val pl = event.player
        val item = event.item ?: return
        if (pl.gameMode == GameMode.SPECTATOR) return
        if (!event.action.isRightClick) return
        if (!canUse(pl, item)) return
        event.isCancelled = true

        val cd = item.getEnchantmentLevel(Enchantment.QUICK_CHARGE)
        val current = System.currentTimeMillis()
        if (coolDownList.contains(pl.name)) {
            val remain = cd - (current - coolDownList[pl.name]!!) / 1000
            if (remain > 0) {
                pl.sendActionBar(mm.deserialize("<red>请等待<green> $remain </green>秒后重试</red>"))
                return
            }
        }
        coolDownList[pl.name] = current
        val radius = item.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA)
        val items = pl.location.getNearbyEntitiesByType(Item::class.java, radius.toDouble())
        items.forEach {
            it.teleport(pl.location)
        }
    }

    override fun calculateUpgradeCost(currentLevel: Int, levelsToUpgrade: Int): Double {
        var cost = 0.0
        var lvl = currentLevel
        repeat(levelsToUpgrade) {
            cost += lvl + Random.nextInt(lvl) + sqrt(lvl.toDouble() * 2) + 8
            lvl++
        }
        return cost
    }
}