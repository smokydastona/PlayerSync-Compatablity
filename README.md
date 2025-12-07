# PlayerSync Multi-Mod Compatibility

A compatibility module that enables [PlayerSync](https://github.com/mlus-asuka/PlayerSync) to synchronize multiple mod data across Minecraft servers.

## Supported Mods

### Traveler's Backpack
Synchronizes [Traveler's Backpack](https://github.com/Tiviacz1337/Travelers-Backpack) data:

- ✅ **Main Inventory** - All 42 slots of backpack storage
- ✅ **Crafting Inventory** - 9 crafting grid slots  
- ✅ **Fluid Tanks** - Both left and right tank contents
- ✅ **Settings** - All backpack settings and configurations
- ✅ **Sleeping Bag State** - Whether the sleeping bag is deployed
- ✅ **Abilities** - Special backpack abilities and upgrades

### Minecraft Comes Alive (MCA) Reborn
Synchronizes [MCA Reborn](https://www.curseforge.com/minecraft/mc-mods/minecraft-comes-alive-reborn) player data:

- ✅ **Marriage & Spouse** - Complete marriage status and partner data
- ✅ **Family & Children** - All children and family member relationships
- ✅ **Village Rank & Reputation** - Standing, rank progression, and monarch status
- ✅ **Relationships & Hearts** - All NPC relationships and heart levels
- ✅ **Player Traits & Mood** - Personality characteristics and emotional states
- ✅ **Baby/Pregnancy State** - Expecting status and baby growth progress
- ✅ **Destiny & Quests** - Destiny progress and quest completion
- ✅ **Gift History** - Gift interactions and cooldowns
- ✅ **Genetics** - Player genetic data for offspring
- ✅ **Interaction History** - Daily interaction tracking with NPCs
- ✅ **Village Data** - Home village UUID and tax status
- ✅ **All MCA Capabilities** - Complete capability data via reflection

### Project MMO (PMMO)
Synchronizes [Project MMO](https://www.curseforge.com/minecraft/mc-mods/project-mmo) progression data:

- ✅ **All Skills & Levels** - Complete skill progression across all skills
- ✅ **Experience Points** - XP data for every skill
- ✅ **Perks** - All unlocked perks and abilities
- ✅ **Active Abilities** - Currently equipped and unlocked abilities
- ✅ **Vein Miner Settings** - Vein miner configuration and data
- ✅ **Player Preferences** - PMMO-specific player settings
- ✅ **Statistics** - All tracked player statistics
- ✅ **Active Bonuses** - Current bonus effects
- ✅ **Requirements** - Progression requirement tracking
- ✅ **Full Capability Sync** - Complete PMMO capability data via reflection

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0 or higher
- PlayerSync 2.0.0 or higher
- **Optional:** Traveler's Backpack 9.0.0 or higher (for backpack sync)
- **Optional:** MCA Reborn 7.5.0 or higher (for MCA sync)
- **Optional:** Project MMO 1.7.0 or higher (for PMMO sync)

> **Note:** This mod works with any combination of the supported mods. You don't need all of them installed.

## Installation

### For Server Owners

1. Download the latest release of this mod
2. Place it in your server's `mods` folder alongside PlayerSync
3. Add any supported mods you want synchronized (Traveler's Backpack, MCA Reborn, etc.)
4. Restart the server
5. The mod will automatically detect and integrate with installed mods

### Building from Source

1. Clone this repository:
   ```powershell
   git clone https://github.com/yourusername/PlayerSync-Chaos-Compatablity.git
   cd PlayerSync-Chaos-Compatablity
   ```

2. Download PlayerSync JAR and place it in `libs/` folder:
   - Get the latest PlayerSync from [releases](https://github.com/mlus-asuka/PlayerSync/releases)
   - Create a `libs` folder in the project root
   - Place `playersync-1.20.1-2.1.4.jar` (or your version) in the `libs` folder

3. Build the project:
   ```powershell
   .\gradlew.bat build
   ```

4. The compiled JAR will be in `build/libs/`

## How It Works

This compatibility module works by:

1. **Hooking into PlayerSync Events** - Listens for player data save/load events from PlayerSync
2. **Detecting Installed Mods** - Automatically detects which supported mods are present
3. **Extracting Mod Data** - Uses reflection and capability systems to extract mod-specific data
4. **Storing in NBT** - Saves the data in a format that PlayerSync can synchronize across servers
5. **Restoring on Login** - When a player joins a different server, their mod data is automatically restored

## Compatibility Notes

### Traveler's Backpack
- **Curios API**: If Traveler's Backpack is equipped via Curios, the data will still sync properly
- **Multiple Backpacks**: Only the currently equipped backpack is synchronized
- **Backpack Items**: Placed backpack blocks are NOT synchronized - only equipped backpacks

### MCA Reborn
- **Marriage Data**: Spouse relationships sync across servers
- **Family Members**: Children and family data persist when switching servers
- **Village Data**: Your standing with villages is maintained
- **Comprehensive Sync**: All player traits, moods, genetics, quests, and destiny progress
- **Both Fabric and Forge**: This compatibility mod is for Forge only, but MCA data format is compatible
- **Auto-Detection**: Scans for 30+ different MCA data keys to ensure nothing is missed

### Project MMO
- **Skill Progression**: All skills, levels, and XP transfer between servers
- **Perks & Abilities**: Unlocked perks and active abilities persist
- **Vein Miner**: Your vein miner configuration follows you
- **Statistics**: All tracked stats are maintained
- **Complete Sync**: Scans for 35+ PMMO data keys including skills, bonuses, preferences, and requirements

## Configuration

This mod works out of the box with no configuration needed. It automatically detects when PlayerSync and any supported mods are present.

**Crash-Resistant Design**: The mod includes comprehensive error handling to prevent crashes even if one of the supported mods has issues. If a particular mod fails, only that mod's sync will be affected - other mods will continue working normally.

## Troubleshooting

### Backpack data not syncing

1. Check that PlayerSync is properly configured and working
2. Verify that both PlayerSync and Traveler's Backpack are installed
3. Check server logs for any error messages from `playersync_travelersbackpack_compat`

### Items disappearing

- Ensure all servers in your network are running the same version of Traveler's Backpack
- Make sure PlayerSync's database is properly configured

### Debug Mode

Enable debug logging by adding this to your `forge.cfg`:

```
forge.logging.console.level=debug
```

Then check logs for messages from `playersync_travelersbackpack_compat`.

## Development

### Project Structure

```
PlayerSync-Chaos-Compatablity/
├── src/main/java/com/playersync/compat/
│   ├── PlayerSyncTravelersBackpackCompat.java   # Main mod class
│   ├── TravelersBackpackCompat.java             # Traveler's Backpack handler
│   ├── MCACompat.java                           # MCA Reborn handler
│   ├── PMmoCompat.java                          # Project MMO handler
│   └── BackpackSyncHandler.java                 # Event handlers
├── src/main/resources/
│   ├── META-INF/mods.toml                       # Mod metadata
│   └── pack.mcmeta                              # Resource pack metadata
├── build.gradle                                  # Build configuration
├── gradle.properties                             # Project properties
└── README.md                                     # This file
```

### Adding Support for New Mods

To add support for additional mods:

1. Create a new compat class (e.g., `YourModCompat.java`) following the pattern in `TravelersBackpackCompat.java`, `MCACompat.java`, or `PMmoCompat.java`
2. Implement `isLoaded()`, `saveData()`, and `loadData()` methods
3. Add integration calls in `BackpackSyncHandler.java`
4. Use comprehensive error handling (Throwable catches) to prevent crashes
5. Test thoroughly on multiple servers

## Credits

- **PlayerSync** - [mlus-asuka](https://github.com/mlus-asuka)
- **Traveler's Backpack** - [Tiviacz1337](https://github.com/Tiviacz1337)
- **MCA Reborn** - [Luke100000](https://github.com/Luke100000/minecraft-comes-alive)
- **Project MMO** - [Caltinor](https://www.curseforge.com/minecraft/mc-mods/project-mmo)

## License

This project is licensed under the GPL-3.0 License - see the [LICENSE](LICENSE) file for details.

## Support

If you encounter issues:

1. Check the [Issues](https://github.com/yourusername/PlayerSync-Chaos-Compatablity/issues) page
2. Join the PlayerSync Discord
3. Create a new issue with:
   - Minecraft version
   - Forge version
   - PlayerSync version
   - Traveler's Backpack version
   - Full error log

## Changelog

### Version 1.0.0
- Initial release
- Support for Minecraft 1.20.1
- Traveler's Backpack: Full inventory, fluids, settings, and abilities sync
- MCA Reborn: Comprehensive sync of all player data including:
  - Marriage, family, and relationships
  - Village rank, reputation, and monarch status
  - Traits, mood, and genetics
  - Destiny, quests, and interactions
  - Baby/pregnancy state and gift history
  - 30+ data keys automatically detected and synced
- Project MMO: Complete skill progression sync including:
  - All skills, levels, and XP
  - Perks and abilities
  - Vein miner settings
  - Statistics and bonuses
  - Player preferences and requirements
  - 35+ data keys automatically detected and synced
- Crash-resistant error handling for maximum stability
- Reflection-based compatibility - no compile-time dependencies
