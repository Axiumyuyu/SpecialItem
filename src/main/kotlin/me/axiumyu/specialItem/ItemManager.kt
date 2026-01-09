package me.axiumyu.specialItem

import me.axiumyu.specialItem.SpecialItem.Companion.plugin
import me.axiumyu.specialItem.items.SpecialItemBase
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getPluginManager
import org.bukkit.event.HandlerList
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault

/**
 * A singleton object to manage all registered SpecialItem types.
 * It now also handles registering/unregistering items as event listeners.
 */
/**
 * A singleton object to manage all registered SpecialItem types.
 * It now also handles registering/unregistering items as event listeners.
 */
object ItemManager {

    private val registeredItems = mutableMapOf<String, SpecialItemBase>()

    /**
     * Registers a new special item type and its event listeners.
     * @param itemClass The SpecialItem instance to register.
     */
    fun register(itemClass: SpecialItemBase) {
        if (registeredItems.containsKey(itemClass.id.lowercase())) {
            plugin.logger.warning("Attempted to register a duplicate SpecialItem with id: ${itemClass.id}")
            return
        }
        registeredItems[itemClass.id.lowercase()] = itemClass

        // Register the item itself as a listener
        getPluginManager().registerEvents(itemClass, plugin)

        // Dynamically register the permission for this item
        try {
            val permission = Permission(itemClass.permissionNode, "Allows usage of the ${itemClass.id} special item.", PermissionDefault.TRUE)
            getPluginManager().addPermission(permission)
            plugin.logger.info("Registered item and listeners for ${itemClass.id}")
        } catch (_: IllegalArgumentException) {
            plugin.logger.info("Permission for ${itemClass.id} (${itemClass.permissionNode}) already exists.")
        }
    }

    fun getById(id: String): SpecialItemBase? {
        return registeredItems[id.lowercase()]
    }

    fun getAllIds(): List<String> {
        return registeredItems.keys.toList()
    }

    /**
     * Unregisters all items and their event listeners.
     */
    fun unregisterAll() {
        registeredItems.values.forEach { item ->
            // Unregister the listener
            HandlerList.unregisterAll(item)
            // Remove the permission
            getPluginManager().removePermission(item.permissionNode)
        }
        registeredItems.clear()
        plugin.logger.info("All special items, listeners, and permissions have been unregistered.")
    }
}