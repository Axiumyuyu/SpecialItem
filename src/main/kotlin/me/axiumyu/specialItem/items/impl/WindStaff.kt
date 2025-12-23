package me.axiumyu.specialItem.items.impl

import me.axiumyu.specialItem.SpecialItem.Companion.mm
import me.axiumyu.specialItem.items.SpecialItemBase
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.lang.Math.toRadians
import kotlin.math.*
import kotlin.random.Random

class WindStaff : SpecialItemBase() {
    override val id: String = "wind_staff"
    override val name: String = "风之法杖"
    override val itemMaterial: Material = Material.BREEZE_ROD
    override val description: String = """
            右键点击将你吹至空中
            摔落缓冲等级为风力(飞行速度)
            冲击等级为单次使用消耗的饥饿值
        """.trimIndent()
    override val enchantmentRanges: Map<Enchantment, Pair<Int, Int>> = mapOf(
        Enchantment.FEATHER_FALLING to (1 to 70),
        Enchantment.PUNCH to (20 to 1)
    )
    override val maxLevel: Int = 50

    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return
        val item = event.item ?: return
        val pl = event.player
        if (event.player.gameMode == GameMode.SPECTATOR) return
        if (!canUse(pl, item)) return
        val punchLvl = item.getEnchantmentLevel(Enchantment.PUNCH)
        val fallingLvl = item.getEnchantmentLevel(Enchantment.FEATHER_FALLING)
        if (pl.foodLevel < punchLvl) {
            pl.sendActionBar(mm.deserialize("<color:#ffea3a>你没有足够的饱食度！</color>"))
            return
        }

        pl.foodLevel -= punchLvl
        removeWeb(pl)
        blowPlayer(pl, fallingLvl)

        // play sound and particles
        pl.playSound(pl.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F)
        if (fallingLvl >= enchantmentRanges[Enchantment.FEATHER_FALLING]!!.second.div(2)){
            pl.world.spawnParticle(Particle.GUST_EMITTER_LARGE,pl.location,1)
        }else{
            pl.world.spawnParticle(Particle.GUST_EMITTER_SMALL,pl.location,1)
        }

        //reset fall distance
        pl.fallDistance = -20F
    }

    private fun blowPlayer(pl: Player, fallingLvl: Int) {
        val pitch = pl.pitch.toDouble()
        val yaw = pl.yaw.toDouble()
        val vec = pl.velocity
        val exact: Double = 4 * abs((toRadians(abs(pitch)) - PI) / PI)
        vec.x = (2 - exact) * sin(toRadians(yaw)) + 0.15 * vec.x
        vec.y = -2 * sin(toRadians(pitch)) + 0.05 * vec.y
        vec.z = (exact - 2) * cos(toRadians(yaw)) + 0.15 * vec.z
        pl.velocity = vec.multiply(fallingLvl / 30)
    }

    override fun calculateUpgradeCost(currentLevel: Int, levelsToUpgrade: Int): Double {
        var cost = 0.0
        var lvl = currentLevel
        repeat(levelsToUpgrade) {
            cost += 2 * lvl.toDouble() + round(Random.nextDouble() * 2000.0) / 100 - 5.0
            lvl++
        }
        return cost
    }
    override fun mapLevels(newLevel: Int): Map<Enchantment, Int> {
        return mapOf(
            Enchantment.FEATHER_FALLING to newLevel,
            Enchantment.PUNCH to max(1, (20 - newLevel.toDouble() / maxLevel.toDouble() * 20).toInt())
        )
    }

    private fun removeWeb(pl: Player) {
        val location = pl.location.toBlockLocation()
        if (location.block.type == Material.COBWEB && canBreak(pl, location)) {
            location.block.type = Material.AIR
        }
    }

    private fun canBreak(pl: Player, location: Location): Boolean {
        val breakEvent = BlockBreakEvent(location.block, pl)

        // 2. 触发事件
        // 这会让所有插件（WorldGuard, Residence等）处理该事件
        Bukkit.getPluginManager().callEvent(breakEvent)

        // 3. 检查结果
        // 如果 isCancelled 为 true，说明有插件禁止了该操作
        return !breakEvent.isCancelled
    }
}