package me.axiumyu.specialItem.items.impl

import me.axiumyu.specialItem.SpecialItem.Companion.mm
import me.axiumyu.specialItem.items.SpecialItemBase
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.Enchantment.AQUA_AFFINITY
import org.bukkit.enchantments.Enchantment.POWER
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

object IronElevator : SpecialItemBase() {

    @JvmField
    val elevatorBlock = IRON_BLOCK

    @JvmField
    val teleportSound: Sound = Sound.ENTITY_IRON_GOLEM_ATTACK

    @JvmField
    val coolDownList: MutableMap<String, Long> = mutableMapOf()

    override val id: String = "elevator"
    override val name: String = "铁块电梯控制器"
    override val itemMaterial: Material = LEVER
    override val description: String = """
        将其拿在手中时<b>向上看</b>并右键可以上升至上一层
        <b>向下看</b>并右键则下降至下一层
        <gold>水下速掘等级x2</gold> 为单次可上升/下降的距离
        <gold>力量等级</gold> 为每次使用后冷却时间
    """.trimIndent()
    override val enchantmentRanges: Map<Enchantment, Pair<Int, Int>> = mapOf(
        AQUA_AFFINITY to (2 to 256),
        POWER to (90 to 1)
    )
    override val maxLevel: Int = 100

    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        val pl = event.player
        val item = event.item ?: return
        if (canUse(pl, item)) event.isCancelled = true else return
        if ((pl.gameMode == GameMode.SPECTATOR)
            || !event.action.isRightClick
            || (pl.location.block.getRelative(BlockFace.DOWN).type != elevatorBlock)
        ) return

        event.isCancelled = true

        val cd = item.getEnchantmentLevel(POWER)
        val current = System.currentTimeMillis()
        if (coolDownList.contains(pl.name)) {
            val remain = cd - (current - coolDownList[pl.name]!!) / 1000
            if (remain > 0) {
                pl.sendActionBar(mm.deserialize("<red>请等待<green> $remain </green>秒后重试</red>"))
                return
            }
        }
        coolDownList[pl.name] = current

        val pitch = pl.pitch
        val maxHeightChange = item.getEnchantmentLevel(AQUA_AFFINITY) * 2
        if (pitch < 0.0f) {
            val worldMaxY = pl.world.maxHeight
            val startY = pl.location.toBlockLocation().addY().y.toInt()
            pl.location.addY().run {
                while (y - startY <= maxHeightChange && y <= worldMaxY) {
                    val result = isValidUp(this)
                    if (result == 0) {
                        pl.teleportAsync(addY())
                        pl.playSound(pl.location, teleportSound, 1f, 1f)
                        return
                    } else {
                        addY(result)
                    }
                }
            }
        } else if (pitch > 0.0f) {
            val worldMinY = pl.world.minHeight
            val startY = pl.location.toBlockLocation().subY().y.toInt()
            pl.location.subY().run {
                while (startY - y <= maxHeightChange && y >= worldMinY) {
                    val result = isValidDown(this)
                    if (result == 0) {
                        pl.teleportAsync(subY())
                        pl.playSound(pl.location, teleportSound, 1f, 1f)
                        return
                    } else {
                        subY(result)
                    }
                }
            }
        }
    }

    override fun calculateUpgradeCost(currentLevel: Int, levelsToUpgrade: Int): Double {
        var cost = 0.0
        var lvl = currentLevel
        repeat(levelsToUpgrade) {
            cost += lvl + Random.nextInt(lvl) + sqrt(lvl.toDouble() * 2) + 3
            lvl++
        }
        return cost
    }

    override fun createItem(player: Player): ItemStack {
        val item = super.createItem(player)
        setCd(item)
        item.editMeta {
            val equippable = it.equippable
            equippable.slot = EquipmentSlot.OFF_HAND
            it.setEquippable(equippable)
        }
        return item
    }

    override fun onUpgrade(item: ItemStack) {
        setCd(item)
    }

    private fun setCd(item: ItemStack) {
        item.editMeta {
            val cd = it.useCooldown
            cd.cooldownSeconds = item.getEnchantmentLevel(POWER).toFloat()
            it.setUseCooldown(cd)
        }
    }

    /**
     * 将Location的Y减少或增加一个值
     * @param d 需要减少或增加的高度，默认为1
     */
    private fun Location.subY(d: Int = 1): Location {
        return this.subtract(0.0, d.toDouble(), 0.0)
    }

    private fun Location.addY(d: Int = 1): Location {
        return this.add(0.0, d.toDouble(), 0.0)
    }

    /**
     * @return 需要减少的高度，0则认为找到正确位置
     */
    private fun isValidDown(location: Location): Int {
        return if (location.block.getRelative(BlockFace.DOWN, 2).type == elevatorBlock) {
            if (checkBlock(location.block.getRelative(BlockFace.DOWN))) {
                if (checkBlock(location.block)) 0 else 3
            } else 2
        } else 1
    }

    /**
     * @return 需要增加的高度，0则认为找到正确位置
     */
    private fun isValidUp(location: Location): Int {
        return if (location.block.type == elevatorBlock) {
            if (checkBlock(location.block.getRelative(BlockFace.UP))) {
                if (checkBlock(location.block.getRelative(BlockFace.UP, 2))) 0 else 3
            } else 2
        } else 1
    }

    /**
     * @return 是否为安全方块
     */
    private fun checkBlock(block: Block): Boolean {
        return (!block.isCollidable || block.isPassable || !block.isSuffocating)
    }

}