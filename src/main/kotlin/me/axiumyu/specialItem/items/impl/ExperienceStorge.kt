package me.axiumyu.specialItem.items.impl

import me.axiumyu.specialItem.SpecialItem
import me.axiumyu.specialItem.SpecialItem.Companion.mm
import me.axiumyu.specialItem.items.SpecialItemBase
import me.axiumyu.specialItem.items.utils.Util.changeLastLore
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType.LONG
import java.math.BigDecimal
import kotlin.random.Random

object ExperienceStorge : SpecialItemBase() {

    @JvmField
    val EXP_STORED = NamespacedKey(SpecialItem.plugin, "exp")

    override val id: String = "experience_storge"
    override val name: String = "经验池"
    override val itemMaterial: Material = Material.BEACON
    override val description: String = """
        保存经验值，可以随时取出
        <green>右键</green>点击存储，<red>左键</red>点击取出
        <aqua>耐久等级x20</aqua>为每次取出/放入的经验数量
        <aqua>耐久等级x400</aqua>为最大储存经验数量
        <gold>建议升级前取出存储的所有经验
    """.trimIndent()
    override val enchantmentRanges: Map<Enchantment, Pair<Int, Int>> = mapOf(
        Enchantment.UNBREAKING to (1 to 100)
    )
    override val price: BigDecimal = 10.toBigDecimal()

    override val maxLevel: Int = 100

    override fun calculateUpgradeCost(currentLevel: Int, levelsToUpgrade: Int): Double {
        if (levelsToUpgrade <= 0) return 0.0
        // 新常量：30000 / (51^2 + ... + 149^2) = 30000 / 1070850
        val baseConstant = 30000.0 / 1070850.0
        var totalCost = 0.0
        for (i in 0 until levelsToUpgrade) {
            val level = currentLevel + i
            // 确保等级在有效范围 [1, 99]（升级操作在99级时升到100级）
            val safeLevel = level.coerceIn(1, 99)
            val baseCost = baseConstant * (safeLevel + 50.0) * (safeLevel + 50.0)
            val randomFactor = 0.93 + 0.14 * Random.nextDouble() // ±7% 随机
            totalCost += baseCost * randomFactor
        }
        return totalCost
    }

    private fun updateLore(item: ItemStack) {
        item.changeLastLore("<aqua>存储的经验值: <yellow>${item.persistentDataContainer[EXP_STORED, LONG]}</yellow>/${item.getEnchantmentLevel(Enchantment.UNBREAKING) * 400}")
    }

    override fun createItem(player: Player): ItemStack {
        val item = super.createItem(player)
        item.editPersistentDataContainer {
            it[EXP_STORED, LONG] = 0
        }
        val lore = item.lore()!!.toMutableList()
        lore.add(mm.deserialize("<black>喵"))
        item.lore(lore)
        updateLore(item)
        return item
    }

    @EventHandler
    fun onUse(ev: PlayerInteractEvent) {
        val item = ev.item ?: return
        val pl = ev.player
        if (pl.gameMode == GameMode.SPECTATOR) return
        if (canUse(pl, item)) ev.isCancelled = true else return

        val storedExp = item.persistentDataContainer[EXP_STORED, LONG] ?: 0
        val operationExp = item.getEnchantmentLevel(Enchantment.UNBREAKING) * 20

        //取出
        if (ev.action.isLeftClick) {
            if (storedExp < operationExp) {
                pl.sendActionBar(mm.deserialize("经验池中没有足够经验！"))
                return
            }
            item.editPersistentDataContainer {
                it[EXP_STORED, LONG] = storedExp - operationExp
            }
            pl.setExperienceLevelAndProgress(pl.calculateTotalExperiencePoints() + operationExp)
            pl.sendActionBar(mm.deserialize("你取出了$operationExp 点经验"))
            updateLore(item)

            //存储
        } else if (ev.action.isRightClick) {
            val playerExp = pl.calculateTotalExperiencePoints()

            if (playerExp < operationExp) {
                pl.sendActionBar(mm.deserialize("你没有足够经验！"))
                return
            }

            val maxVolume = item.getEnchantmentLevel(Enchantment.UNBREAKING) * 400
            if (storedExp + operationExp > maxVolume) {
                pl.sendActionBar(mm.deserialize("经验池已满！"))
                return
            }
            item.editPersistentDataContainer {
                it[EXP_STORED, LONG] = storedExp + operationExp
            }
            pl.setExperienceLevelAndProgress(pl.calculateTotalExperiencePoints() - operationExp)

            pl.sendActionBar(mm.deserialize("你存入了$operationExp 点经验"))
            updateLore(item)
        }
    }
}