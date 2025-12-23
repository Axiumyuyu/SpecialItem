package me.axiumyu.specialItem.commands

import me.axiumyu.specialItem.ItemManager
import me.axiumyu.specialItem.SpecialItem.Companion.mm
import me.axiumyu.specialItem.SpecialItem.Companion.xc
import me.axiumyu.specialItem.items.SpecialItemBase
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class SpecialItemCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(mm.deserialize("<red>此命令只能由玩家执行。</red>"))
            return true
        }

        if (args.isEmpty()) {
            sendHelpMessage(sender, label)
            return true
        }

        when (args[0].lowercase()) {
            "get" -> handleGetCommand(sender, args, label)
            "transfer" -> handleTransferCommand(sender, args, label)
            "upgrade" -> handleUpgradeCommand(sender, args)
            else -> sendHelpMessage(sender, label)
        }

        return true
    }

    private fun handleUpgradeCommand(player: Player, args: Array<out String>) {

        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) {
            player.sendMessage(mm.deserialize("<red>你必须将要升级的特殊物品拿在主手。</red>"))
            return
        }

        val pdc = itemInHand.itemMeta?.persistentDataContainer ?: run {
            player.sendMessage(mm.deserialize("<red>这不是一个有效的特殊物品。</red>"))
            return
        }

        val itemId = pdc.get(SpecialItemBase.ID_KEY, PersistentDataType.STRING) ?: run {
            player.sendMessage(mm.deserialize("<red>这不是一个有效的特殊物品。</red>"))
            return
        }

        val specialItem = ItemManager.getById(itemId) ?: run {
            player.sendMessage(mm.deserialize("<red>物品数据已损坏，无法识别该物品。</red>"))
            return
        }

        if (!specialItem.canUse(player, itemInHand)) {
            // The canUse method sends the specific error message
            player.sendMessage(mm.deserialize("<red>升级失败,请检查物品附魔是否完整或物品是否属于你"))
            return
        }

        val currentLevel = specialItem.getItemLevel(itemInHand)
        val lvl = if (args.size > 1) args[1].toIntOrNull() ?: 1 else 1
        if (lvl <= 0) {
            player.sendMessage(mm.deserialize("<red>升级数量必须大于0。</red>"))
            return
        }
        if (currentLevel >= specialItem.maxLevel) {
            player.sendMessage(mm.deserialize("<yellow>你的 ${specialItem.id} 已达到最高等级！</yellow>"))
            return
        }
        if (currentLevel + lvl > specialItem.maxLevel) {
            player.sendMessage(mm.deserialize("<yellow>你的 ${specialItem.id} 最高只能达到${specialItem.maxLevel}级！</yellow>"))
            return
        }

        val cost = specialItem.calculateUpgradeCost(currentLevel, lvl).toBigDecimal()
        val playerBal = xc.getPlayerData(player.uniqueId).balance
        val costMsg = xc.getdisplay(cost)
        if (playerBal <= cost) {
            player.sendMessage(mm.deserialize("<red>升级需要$costMsg ，你的余额不足。</red>"))
            return
        }

        // --- Upgrade Logic ---
        xc.changePlayerBalance(player.uniqueId, player.name, cost, false)

        val newLevel = currentLevel + lvl

        upgradeItem(itemInHand, specialItem, player.name, newLevel)
        player.sendMessage(mm.deserialize("<green>恭喜！你的 ${specialItem.id} 已成功升级到 $newLevel 级！</green>, 花费了 $costMsg",))
    }

    private fun handleGetCommand(player: Player, args: Array<out String>, label: String) {
        if (args.size < 2) {
            player.sendMessage(mm.deserialize("<red>用法: /$label get <item_id></red>"))
            return
        }

        val itemId = args[1]
        val specialItem = ItemManager.getById(itemId)
        if (specialItem == null) {
            player.sendMessage(mm.deserialize("<red>未知的特殊物品 ID: '$itemId'</red>"))
            return
        }

        if (!player.hasPermission(specialItem.permissionNode)) {
            player.sendMessage(mm.deserialize("<red>你没有权限获取此物品 (${specialItem.permissionNode})。</red>"))
            return
        }
        val bal = xc.getPlayerData(player.uniqueId).balance
        val price = specialItem.price
        val display by lazy{ xc.getdisplay(price)}
        if (bal < specialItem.price) {
            player.sendMessage(mm.deserialize("你需要 $display 来获得这个特殊物品"))
            return
        }
        val item = specialItem.createItem(player)
        val result = player.inventory.addItem(item)
        if (result.isNotEmpty()) {
            player.world.dropItem(player.location, result.values.first())
            player.sendMessage(mm.deserialize("<yellow>你的背包已满，物品已掉落在你的脚下。</yellow>"))
        }

        xc.changePlayerBalance(player.uniqueId,player.name, price,false)
        player.sendMessage(mm.deserialize("<green>你已获得一个 ${specialItem.id}!</green>"))
    }

    private fun handleTransferCommand(player: Player, args: Array<out String>, label: String) {
        if (args.size < 2) {
            player.sendMessage(mm.deserialize("<red>用法: /$label transfer <item_id> [confirm]</red>"))
            return
        }

        val itemId = args[1]
        val specialItem = ItemManager.getById(itemId)
        if (specialItem == null) {
            player.sendMessage(mm.deserialize("<red>未知的特殊物品 ID: '$itemId'</red>"))
            return
        }

        if (!player.hasPermission(specialItem.permissionNode)) {
            player.sendMessage(mm.deserialize("<red>你没有权限转移此物品 (${specialItem.permissionNode})。</red>"))
            return
        }

        val mainHandItem = player.inventory.itemInMainHand
        val offHandItem = player.inventory.itemInOffHand

        if (offHandItem.type == Material.AIR || !specialItem.isValidItem(offHandItem)) {
            player.sendMessage(mm.deserialize("<red>你必须将特殊物品 '${specialItem.id}' 拿在副手。</red>"))
            return
        }

        if (mainHandItem.type == Material.AIR) {
            player.sendMessage(mm.deserialize("<red>你必须将要转移到的目标物品拿在主手。</red>"))
            return
        }

        if (!specialItem.canUse(player, offHandItem)) {
            player.sendMessage(mm.deserialize("<red>你不能使用这个物品。</red>"))
            return
        }
        val lvl = specialItem.getItemLevel(offHandItem)
        val cost = specialItem.calculateUpgradeCost(lvl, 1).div(2).toBigDecimal()
        val costMsg = xc.getdisplay(cost)

        if (xc.getPlayerData(player.uniqueId).balance < cost) {
            player.sendMessage(mm.deserialize("<red>你需要消耗$costMsg 来执行这个操作</red>"))
        }

        if (args.size < 3 || !args[2].equals("confirm", ignoreCase = true)) {
            player.sendMessage(mm.deserialize("<yellow>警告: 这将覆盖你主手物品 (${mainHandItem.type}) 上的所有数据，并且会删除原物品，额外消耗$costMsg</yellow>"))
            player.sendMessage(mm.deserialize("<yellow>要继续，请在命令末尾加上 'confirm' 并重新运行：</yellow>"))
            player.sendMessage(mm.deserialize("<aqua>/$label transfer $itemId confirm</aqua>"))
            return
        }

        val newMainHandItem = specialItem.transferData(offHandItem, mainHandItem.clone())
        xc.changePlayerBalance(player.uniqueId, player.name, cost, false)
        player.inventory.setItemInMainHand(newMainHandItem)
        player.inventory.setItemInOffHand(null)
        player.sendMessage(mm.deserialize("<green>成功消耗$costMsg 将 '${specialItem.id}' 的灵魂转移到了你的新物品上！</green>"))
    }

    private fun sendHelpMessage(sender: CommandSender, label: String) {
        sender.sendMessage(mm.deserialize("<gold>--- SpecialItems 帮助 ---</gold>"))
        sender.sendMessage(mm.deserialize("<aqua>/$label get <item_id></aqua> <gray>- 获取一个新的特殊物品。</gray>"))
        sender.sendMessage(mm.deserialize("<aqua>/$label transfer <item_id> [confirm]</aqua> <gray>- 转移一个特殊物品的灵魂。</gray>"))
        sender.sendMessage(mm.deserialize("<aqua>/$label upgrade</aqua> <gray>- 升级你主手中的特殊物品。</gray>"))
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String> {
        val completions = mutableListOf<String>()
        if (sender !is Player) return completions

        when (args.size) {
            1 -> {
                if ("get".startsWith(args[0], ignoreCase = true)) completions.add("get")
                if ("transfer".startsWith(args[0], ignoreCase = true)) completions.add("transfer")
                if ("upgrade".startsWith(args[0], ignoreCase = true)) completions.add("upgrade")
            }

            2 -> {
                val subCommand = args[0].lowercase()
                if (subCommand == "get" || subCommand == "transfer") {
                    ItemManager.getAllIds().forEach {
                        if (it.startsWith(args[1], true)
                            && sender.hasPermission("specialitems.item.$it")
                        ) {
                            completions.add(it)
                        }
                    }
                }
            }
        }
        return completions
    }

    private fun upgradeItem(item: ItemStack, source: SpecialItemBase, playerName: String, newLevel: Int) {

        item.editPersistentDataContainer {
            it.set(SpecialItemBase.LEVEL_KEY, PersistentDataType.INTEGER, newLevel)

        }
        val newLore = source.updateLore(playerName, newLevel)
        item.editMeta {
            it.lore(newLore)
            when (newLevel.toDouble() / source.maxLevel.toDouble()) {
                0.2 -> it.setRarity(ItemRarity.COMMON)
                0.4 -> it.setRarity(ItemRarity.UNCOMMON)
                0.6 -> it.setRarity(ItemRarity.RARE)
                0.8 -> it.setRarity(ItemRarity.EPIC)
            }
        }
        val newEnchants = source.mapLevels(newLevel)
        item.removeEnchantments()
        item.addUnsafeEnchantments(newEnchants)
        source.onUpgrade(item)
    }
}