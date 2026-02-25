<div align="center">

# ⚔️ Vanilla² (Fabric)

### Armor, Weapons and combat interaction overhaul while staying close to the vanilla feel.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Compatible-orange.svg)](https://fabricmc.net/)

</div>

## 📦 Installation

### 1️⃣ Download & Install
- 🔽 Download the latest version from the [releases page](../../releases)
- 📁 Place the `.jar` file into your `/mods` folder

### 2️⃣ Restart
Restart your server.

## 🎮 Changes

- Reworks sword, axe, and trident combat stats such as attack damage, attack speed, and interaction tuning
- Expands armor scaling so high armor values keep working past vanilla limits
- Updates combat math to support higher armor and magic absorb values
- Adds a custom mace protection attribute that reduces mace smash damage

### 🤺 Dual wield system

- Adds a dual wield data component and matching logic for compatible weapon pairs
- Merges enchantments from offhand into mainhand during attacks with blocked enchant safeguards
- Grants offhand powered bonus damage on strong hits
- Supports charge based critical bonus behavior triggered through offhand use
- Plays sweep and crit combat feedback for the dual wield flow

### 🧰 Item behavior updates

- Adjusts tool and weapon durability values by material
- Applies armor durability updates through armor material handling
- Gives shields block attack component behavior with tuned values
- Changes potion stack sizes
- Adjusts fishing rod durability and adds offensive hook interactions with enchant integration

### ✨ Client side combat quality of life

- Lets sword targeting pass through grass and flowers so nearby entities are easier to hit
- Improves offhand interaction priority for fishing rod and shield related combat cases
- Extends armor HUD rendering to display armor values above 20 with extra rows

## 🔎 Technological Information 
- Minecraft 1.21.11
- Fabric Loader 0.18.4 or newer
- Fabric API 0.141.2 plus 1.21.11
- Java 21
- Gradle via wrapper scripts included in the repo

### 📋 Runtime Requirements
- ⚙️ Fabric Loader + Fabric API

## 🛠️ Development

### 📦 Build Artifacts
- **Runtime Jar** at `/build/libs/<mod>.jar`

### 🔨 Building
- **Gradle Wrappers** included.
- `./gradlew build` to build **Runtime Jar**.
- `./gradlew runClient` to run a **dedicated Minecraft client** instance with the mod pre-installed.

</div>

