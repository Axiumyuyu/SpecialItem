Language: [中文](README.md) | English

# SpecialItem

A **Minecraft Paper plugin (Kotlin)** designed to build an **upgradeable, player-bound special item system**.

⚠️ **Important Notice (Project Positioning)**

> **SpecialItem is NOT a plug-and-play plugin.**
>
> It is closer to a **“special item development framework / example collection”**, primarily targeting **plugin developers**:
>
> * You can **extend `SpecialItemBase`** to implement your own special items
> * You are free to **modify or remove** the built-in item implementations
> * **All built-in items are examples only** and are **not guaranteed** to fit every server’s gameplay, balance, or economy

If you are looking for a finished plugin that you can just drop into the `plugins` folder and start playing with,
then this project is probably **not for you**.

If you want to **quickly build an upgradeable + player-bound special item system and extend it yourself**,
then this project is made exactly for that purpose.

*(This plugin was written purely for my own server’s convenience. Some parts of the code might be useful to others — nothing more, nothing less.)*

---

## Basic Information

* **Plugin Name**: SpecialItem
* **Version**: 1.0.0
* **Author**: AxiumYu
* **Minecraft API**: Paper / Bukkit (`api-version: 1.21`)
* **Language**: Kotlin
* **Dependencies**:

    * [`XConomy`](https://github.com/YiC200333/XConomyAPI)
      Used for currency handling and upgrade costs
      *(You may replace it with alternatives such as Vault, but abstraction is not implemented yet — planned for the future)*

The most important part of this project is:

> **`SpecialItemBase` — the core of the entire design**

---

## Core Design Concepts

### 1. Every Special Item = `SpecialItemBase`

Each special item:

* Is an **independent Kotlin class**
* Extends `SpecialItemBase`
* Defines its own:

    * Maximum level (`maxLevel`)
    * Upgrade cost (`BigDecimal`, decoupled from the economy implementation)
    * Material, rarity, enchantment range
    * Behavior logic (event handling)

The plugin itself **does not care what your item actually does**.
It only handles:

* Registration
* Identification
* Upgrading
* Player binding

---

### 2. Player Binding & Upgrade System

`SpecialItemBase` already handles:

* Item identification (name, material, lore, max level, etc.)
* Player binding (owner)
* Level storage and upgrading
* Automatic lore updates

Example lore structure:

* Description text
* Current level / max level
* Bound player name

All you need to focus on is:

> **“What does this item do at a given level?”**

---

## Built-in Example Items (Examples Only)

The repository currently includes the following example implementations:

* **WindStaff**
  A wind staff that consumes hunger to dash the player forward
* **ExperienceStorage**
  A convenient tool for storing and retrieving experience points
* **IronElevator**
  An iron-block-based elevator for vertical movement
* **ItemMagnet**
  Attracts nearby dropped items within a certain range

⚠️ Once again:

> These items are **examples only** and are **not guaranteed** to be:
>
> * Balanced
> * Performance-optimized
> * Suitable for your server’s gameplay

It is strongly recommended to:

* Remove them entirely
* Or keep only the parts of the logic you actually need

---

## Commands

The plugin provides a main command:

```
/specialitem
/si
```

### Usage

```
/si get <id>
# Get a special item

/si upgrade <id>
# Upgrade a special item

/si transfer <id> [confirm]
# Transfer a special item to another item (change ownership binding)
```

(Exact behavior is defined in `SpecialItemCommand.kt`.)

---

## Developer Guide (Most Important Section)

### Creating Your Own Special Item

1. Create a new class extending `SpecialItemBase` and implement the required members
2. Register event listeners (`SpecialItemBase` already implements `Listener`)
3. Register the item via `ItemManager` during plugin initialization

---

### Things You Are Free to Modify

* Upgrade cost logic
* Lore formatting
* Player binding rules
* Economy system (currently XConomy-based)
* Trigger conditions / interaction logic

The plugin **does not restrict you** from doing any of these.

---

## Build & Installation (Developer-Oriented)

```bash
./gradlew build
```

The generated JAR can be found at:

```
build/libs/SpecialItem-X.X.X.jar
```

Place it into the Paper server’s `plugins/` directory.

---

## Who Is This For?

✅ **Suitable for**:

* Plugin developers who want to **write their own special items**
* Developers who don’t want to reimplement PDC / upgrade / lore logic from scratch
* Those looking for a **modifiable and extensible item-system skeleton**

❌ **Not suitable for**:

* Server owners looking for a ready-to-use gameplay plugin
* Anyone unwilling to modify code or rebalance mechanics

---

## License

All source code is released under the **MIT License**.
