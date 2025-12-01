# PlayerSync Traveler's Backpack Compatibility

A compatibility module that enables [PlayerSync](https://github.com/mlus-asuka/PlayerSync) to synchronize [Traveler's Backpack](https://github.com/Tiviacz1337/Travelers-Backpack) data across multiple Minecraft servers.

## Features

This compatibility module synchronizes the following Traveler's Backpack data:

- ✅ **Main Inventory** - All 42 slots of backpack storage
- ✅ **Crafting Inventory** - 9 crafting grid slots  
- ✅ **Fluid Tanks** - Both left and right tank contents
- ✅ **Settings** - All backpack settings and configurations
- ✅ **Sleeping Bag State** - Whether the sleeping bag is deployed
- ✅ **Abilities** - Special backpack abilities and upgrades

## Requirements

- Minecraft 1.20.1
- Forge 47.2.0 or higher
- PlayerSync 2.0.0 or higher
- Traveler's Backpack 9.0.0 or higher

## Installation

### For Server Owners

1. Download the latest release of this mod
2. Place it in your server's `mods` folder alongside PlayerSync and Traveler's Backpack
3. Restart the server
4. The mod will automatically integrate with PlayerSync

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
2. **Extracting Backpack Data** - Uses Traveler's Backpack's capability system to extract all backpack data
3. **Storing in NBT** - Saves the data in a format that PlayerSync can synchronize across servers
4. **Restoring on Login** - When a player joins a different server, their backpack data is automatically restored

## Compatibility Notes

- **Curios API**: If Traveler's Backpack is equipped via Curios, the data will still sync properly
- **Multiple Backpacks**: Only the currently equipped backpack is synchronized
- **Backpack Items**: Placed backpack blocks are NOT synchronized - only equipped backpacks

## Configuration

This mod works out of the box with no configuration needed. It automatically detects when both PlayerSync and Traveler's Backpack are present.

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
│   ├── TravelersBackpackCompat.java             # Backpack data handler
│   └── BackpackSyncHandler.java                 # Event handlers
├── src/main/resources/
│   ├── META-INF/mods.toml                       # Mod metadata
│   └── pack.mcmeta                              # Resource pack metadata
├── build.gradle                                  # Build configuration
├── gradle.properties                             # Project properties
└── README.md                                     # This file
```

### Adding Features

To add support for additional backpack features:

1. Edit `TravelersBackpackCompat.java`
2. Add save logic in `saveBackpackData()`
3. Add load logic in `loadBackpackData()`
4. Test thoroughly on multiple servers

## Credits

- **PlayerSync** - [mlus-asuka](https://github.com/mlus-asuka)
- **Traveler's Backpack** - [Tiviacz1337](https://github.com/Tiviacz1337)

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
- Full backpack inventory synchronization
- Fluid tank synchronization
- Settings and abilities synchronization
