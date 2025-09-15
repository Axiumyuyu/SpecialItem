package me.axiumyu.specialItem.items

import me.axiumyu.specialItem.SpecialItem
import me.axiumyu.specialItem.SpecialItem.Companion.mm
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit.getServer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.math.BigDecimal
import kotlin.math.max

/**
 * The abstract base class for all special items.
 * Each SpecialItem is now a Listener, responsible for its own trigger logic.
 */
abstract class SpecialItemBase : Listener {

    // --- Properties to be defined by subclasses ---
    abstract val id: String

    abstract val name : String
    abstract val itemMaterial: Material
    abstract val description: String
    abstract val enchantmentRanges: Map<Enchantment, Pair<Int, Int>>
    abstract val maxLevel: Int

    open val price : BigDecimal = 100.toBigDecimal()

    // --- Properties defined by the base class ---
    val permissionNode: String by lazy { "specialitems.item.$id" }

    companion object {
        @JvmField
        val ID_KEY = NamespacedKey(SpecialItem.instance, "special_item_id")

        @JvmField
        val OWNER_UUID_KEY = NamespacedKey(SpecialItem.instance, "special_item_owner_uuid")

        @JvmField
        val OWNER_NAME_KEY = NamespacedKey(SpecialItem.instance, "special_item_owner_name")

        @JvmField
        val LEVEL_KEY = NamespacedKey(SpecialItem.instance, "special_item_level")

        @JvmStatic
        fun mapValue(
            x: Double,
            srcInterval: Pair<Double, Double>,
            targetIntervals: Collection<Pair<Double, Double>>
        ): List<Double> {
            val (a, b) = srcInterval
            require(a != b) { "Source interval must not be a single point (a != b)" }

            val ratio = (x - a) / (b - a)

            return targetIntervals.map { (c, d) ->
                c + ratio * (d - c)
            }
        }
    }

//    abstract fun onUse(event: PlayerEvent)

    // --- Abstract methods to be implemented by subclasses ---
    abstract fun calculateUpgradeCost(currentLevel: Int, levelsToUpgrade: Int): Double

    open fun upgrade(item : ItemStack){}

    /**
     * Calculates the enchantments that an item should have at a given level.
     * @param newLevel The target level.
     * @return A map of Enchantments to their corresponding levels.
     */
    open fun mapLevels(newLevel : Int): Map<Enchantment, Int> {
        val source = (1.0 to maxLevel.toDouble())
        val targets = enchantmentRanges.values.map { it.first.toDouble() to it.second.toDouble() }
        val values = mapValue(newLevel.toDouble(),source,targets)

//        val originalMap = mapOf("a" to 1, "b" to 2, "c" to 3)
//        val newValues = listOf(10, 20, 30)

//        val newMap = originalMap.mapValues { (_, index) -> newValues[index] }

//        return enchantmentRanges.mapValues { (_, index) ->  values[index].toInt()}
        return enchantmentRanges.keys.zip(values.map { max(it.toInt(),1) }).toMap()
    }

    // --- Concrete methods provided by the base class ---

    open fun createItem(player: Player): ItemStack {
        val item = ItemStack(itemMaterial, 1)
        item.editPersistentDataContainer {
            it.set(ID_KEY, PersistentDataType.STRING, id)
            it.set(OWNER_UUID_KEY, PersistentDataType.STRING, player.uniqueId.toString())
            it.set(OWNER_NAME_KEY, PersistentDataType.STRING, player.name)
            it.set(LEVEL_KEY, PersistentDataType.INTEGER, 1)
        }
        item.editMeta {
            it.setRarity(ItemRarity.COMMON)
            val displayName = mm.deserialize(name)
            it.displayName(displayName)
            it.lore(updateLore(player.name, 1))
        }
        // Get the calculated enchantments for level 1
        val initialEnchants = mapLevels(1)

        // Apply them to the item
        item.addUnsafeEnchantments(initialEnchants)

        return item
    }

    fun canUse(player: Player, itemStack: ItemStack?): Boolean {
        itemStack ?: return false
        if (!isThisTypeOfItem(itemStack)) {
            return false
        }

        val pdc = itemStack.itemMeta!!.persistentDataContainer
        val ownerUUID = pdc.get(OWNER_UUID_KEY, PersistentDataType.STRING)

        if (player.uniqueId.toString() != ownerUUID) {
            player.sendMessage(mm.deserialize("<red>这个物品绑定给了另一位玩家，你无法使用它的特殊能力。</red>"))
            return false
        }

        if (!player.hasPermission(permissionNode)) {
            player.sendMessage(mm.deserialize("<red>你没有权限使用这个特殊物品 ($permissionNode)。</red>"))
            return false
        }

        return true
    }

    fun findItemInInventory(player: Player): ItemStack? {
        for (item in player.inventory.contents) {
            if (isThisTypeOfItem(item)) {
                return item
            }
        }
        return null
    }

    fun isThisTypeOfItem(itemStack: ItemStack?): Boolean {
        if (itemStack == null || itemStack.type == Material.AIR) return false
        val pdc = itemStack.itemMeta?.persistentDataContainer ?: return false
        if (!this.hasFullEnchant(itemStack)) return false
        return pdc.get(ID_KEY, PersistentDataType.STRING)?.equals(id, ignoreCase = true) == true
    }

    fun transferData(sourceItem: ItemStack, targetItem: ItemStack): ItemStack {
        val sourceMeta = sourceItem.itemMeta ?: return targetItem
        val targetMeta = getServer().itemFactory.getItemMeta(targetItem.type)!!

        targetMeta.persistentDataContainer.keys.forEach { key ->
            targetMeta.persistentDataContainer.remove(key)
        }
        targetMeta.enchants.keys.forEach { enchant ->
            targetMeta.removeEnchant(enchant)
        }

        val sourcePDC = sourceMeta.persistentDataContainer
        val targetPDC = targetMeta.persistentDataContainer
        targetPDC.set(ID_KEY, PersistentDataType.STRING, sourcePDC.get(ID_KEY, PersistentDataType.STRING)!!)
        targetPDC.set(
            OWNER_UUID_KEY,
            PersistentDataType.STRING,
            sourcePDC.get(OWNER_UUID_KEY, PersistentDataType.STRING)!!
        )
        targetPDC.set(
            OWNER_NAME_KEY,
            PersistentDataType.STRING,
            sourcePDC.get(OWNER_NAME_KEY, PersistentDataType.STRING)!!
        )
        targetPDC.set(LEVEL_KEY, PersistentDataType.INTEGER, sourcePDC.get(LEVEL_KEY, PersistentDataType.INTEGER) ?: 1)

        sourceMeta.enchants.forEach { (enchant, level) ->
            targetMeta.addEnchant(enchant, level, true)
        }

        targetMeta.displayName(sourceMeta.displayName())
        targetMeta.lore(sourceMeta.lore())

        targetMeta.itemFlags.forEach { flag -> targetMeta.removeItemFlags(flag) }
        sourceMeta.itemFlags.forEach { flag -> targetMeta.addItemFlags(flag) }

        targetItem.itemMeta = targetMeta
        return targetItem
    }

    fun getItemLevel(itemStack: ItemStack): Int {
        if (!isThisTypeOfItem(itemStack)) return 0
        return itemStack.itemMeta?.persistentDataContainer?.get(LEVEL_KEY, PersistentDataType.INTEGER) ?: 1
    }

    fun hasFullEnchant(item: ItemStack): Boolean = item.enchantments.keys.containsAll(this.enchantmentRanges.keys)

    fun updateLore(ownerName: String, level: Int): List<Component> {

        val newLore = listOf(
            mm.deserialize("<aqua>$description</aqua>"),
            Component.empty(),
            mm.deserialize(
                "<gray>等级: <yellow><level></yellow>/<max_level>",
                Placeholder.unparsed("level", level.toString()),
                Placeholder.unparsed("max_level", maxLevel.toString())
            ),
            mm.deserialize("<gray>绑定玩家: <yellow>$ownerName</yellow>")
        )
//        getServer().sendMessage(mm.deserialize("new lore set.level: $level"))
        return newLore
    }
}
