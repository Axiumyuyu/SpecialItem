package me.axiumyu.specialItem.items.impl

import me.axiumyu.specialItem.SpecialItem.Companion.mm
import me.axiumyu.specialItem.items.SpecialItemBase
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
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
        val pitch = pl.pitch.toDouble()
        val yaw = pl.yaw.toDouble()
        val vec = pl.velocity
        val exact: Double = 4 * abs((toRadians(abs(pitch)) - PI) / PI)
        vec.x = (2 - exact) * sin(toRadians(yaw)) + 0.15 * vec.x
        vec.y = -2 * sin(toRadians(pitch)) + 0.05 * vec.y
        vec.z = (exact - 2) * cos(toRadians(yaw)) + 0.15 * vec.z
        pl.velocity = vec.multiply(fallingLvl / 30)
        pl.playSound(pl.location, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0F, 1.0F)
        if (fallingLvl >= enchantmentRanges[Enchantment.FEATHER_FALLING]!!.second.div(2)){
            pl.world.spawnParticle(Particle.GUST_EMITTER_LARGE,pl.location,1)
        }else{
            pl.world.spawnParticle(Particle.GUST_EMITTER_SMALL,pl.location,1)
        }
        pl.fallDistance = -20F
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
}