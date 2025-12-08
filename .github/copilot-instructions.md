# Copilot Instructions — Playersync Performance Plus

You are a Minecraft Forge mod development expert working on a server-side mod for Minecraft 1.20.1.

## Workflow After Every Code Change

**After ANY code change, you MUST follow this complete workflow:**

1. **Scan all files first** - Run error checking across the entire codebase
2. **Fix all errors systematically** - Address every error found, not just one file
3. **Re-validate after each fix** - Ensure no new errors were introduced
4. **Explain every change** - What was wrong, what you changed, and why
5. **Push to GitHub Actions** - Commit, push, and tag for compilation
6. **Only stop when 100% validated** - Continue until all files are completely correct and compile without errors

## Compilation Workflow

**NEVER build locally.** Always use GitHub Actions:

```bash
git add -A
git commit -m "descriptive message"
git push
git tag v1.0.X
git push origin v1.0.X
```

This triggers GitHub Actions to compile the mod. The workspace must stay clean - no `build/` or `.gradle/` directories.

## Project Architecture

### Project Type
- **Name:** PlayerSync Performance Plus
- **Type:** Server-side performance optimization addon for PlayerSync
- **Minecraft Version:** 1.20.1
- **Mod Loader:** Forge 47.2.0
- **Java Version:** 17
- **Main Mod ID:** `playersync_performance_plus`

### Core Purpose
Universal performance enhancement for PlayerSync that:
- Works WITH or WITHOUT optional mod dependencies
- Provides performance optimizations for ALL PlayerSync data
- Optionally enhances sync for Traveler's Backpack, MCA Reborn, and PMMO
- Uses Lithium-inspired optimizations for massive performance gains

### Project Structure

```
src/main/java/com/playersync/compat/
├── PlayerSyncTravelersBackpackCompat.java  # Main mod class
├── BackpackSyncHandler.java                 # Event handlers for save/load/clone
├── SyncOptimizations.java                   # Batching & compression utilities
├── AsyncSyncManager.java                    # Async processing framework
├── LithiumInspiredOptimizations.java        # Lithium-style optimizations
├── TravelersBackpackCompat.java             # Traveler's Backpack integration
├── MCACompat.java                           # MCA Reborn integration
└── PMmoCompat.java                          # Project MMO integration
```

### Dependencies

**Required:**
- PlayerSync 2.1.4+ (the mod we're enhancing)
- Minecraft Forge 47.2.0+
- FastUtil 8.5.12 (for Lithium optimizations)

**Optional (mod works without these):**
- Traveler's Backpack 9.1.15+ (optional mod compatibility)
- MCA Reborn (optional mod compatibility)
- Project MMO (optional mod compatibility)

### Key Features Implemented

1. **Lazy Loading** - Mod detection caching with reflection
2. **Batch Operations** - Single I/O operation for all mod data
3. **GZIP Compression** - 40-70% size reduction for data > 512 bytes
4. **Auto-Save System** - Every 60 seconds, prevents data loss
5. **Lithium Optimizations** - 94% CPU reduction in auto-save cycles:
   - FastUtil collections (20-40% faster)
   - Dirty flag system (only save changes)
   - Sleeping system (skip AFK players)
   - Notification-based sync (event-driven)
   - Smart caching (reuse compression)

### Important Design Patterns

**Lazy Loading Pattern:**
```java
private static Boolean loaded = null;

public static boolean isLoaded() {
    if (loaded == null) {
        loaded = ModList.get().isLoaded("modid");
    }
    return loaded;
}
```

**Dirty Flag Pattern (Lithium-inspired):**
```java
// Mark dirty when data changes
LithiumInspiredOptimizations.markDirty(playerId);

// Only process dirty players
if (LithiumInspiredOptimizations.isDirty(playerId)) {
    saveData(player);
    LithiumInspiredOptimizations.clearDirty(playerId);
}
```

**Sleeping System (Lithium-inspired):**
```java
// Skip sleeping (inactive) players
if (LithiumInspiredOptimizations.shouldSkipAutoSave(playerId, currentTick)) {
    continue;
}
```

**Batching Pattern:**
```java
CompoundTag batch = SyncOptimizations.createBatch();
SyncOptimizations.addToBatch(batch, "modname", data, true);
player.getPersistentData().put("PlayerSyncCompat_Batch", batch);
```

### Event Handlers

**Priority:** `EventPriority.HIGHEST` (runs before other mods)

**Main Events:**
- `PlayerEvent.SaveToFile` - Save player data
- `PlayerEvent.LoadFromFile` - Load player data  
- `PlayerEvent.Clone` - Clone on death/dimension change
- `LivingDeathEvent` - Track death state
- `PlayerEvent.PlayerLoggedInEvent` - Sync on login
- `PlayerEvent.PlayerLoggedOutEvent` - Cleanup
- `TickEvent.ServerTickEvent` - Auto-save timer

### Safety Features

**Death State Tracking:**
```java
private static final Set<UUID> deadPlayersDuringLogin = ConcurrentHashMap.newKeySet();
```

**Sync Completion Tags:**
```java
player.getTags().add("player_synced");
```

**Incomplete Sync Tracking:**
```java
private static final Set<UUID> incompleteSyncPlayers = ConcurrentHashMap.newKeySet();
```

### Build Configuration

**Files:**
- `build.gradle` - Gradle build script with FastUtil dependency
- `gradle.properties` - Version properties
- `mods.toml` - Forge mod metadata

**Important:** Optional dependencies in `mods.toml`:
```toml
[[dependencies.playersync_performance_plus]]
    modId = "travelersbackpack"
    mandatory = false  # NOT required!
```

### GitHub Actions Workflow

The mod uses GitHub Actions for compilation:
1. Automatically triggers on push
2. Builds with Gradle
3. Uploads JAR artifact
4. Tag with `v1.0.X` to create release

**Never build locally** - always use GitHub Actions to keep workspace clean.

### Documentation Files

- `README.md` - User-facing documentation
- `OPTIMIZATIONS.md` - Technical optimization details
- `OPTIMIZATION_SUMMARY.md` - Summary of all optimizations
- `LITHIUM_OPTIMIZATIONS.md` - Lithium-inspired optimizations explained
- `LITHIUM_INTEGRATION_SUMMARY.md` - Integration guide
- `CONTRIBUTING_TO_PLAYERSYNC.md` - Guide for contributing to PlayerSync

### Code Style Rules

1. **Always check for null** before accessing entities
2. **Use try-catch blocks** around all mod compatibility code
3. **Log at appropriate levels:**
   - `.info()` for important events
   - `.debug()` for detailed tracing
   - `.warn()` for recoverable issues
   - `.error()` for failures
4. **Mark dirty on data changes** (Lithium optimization)
5. **Clear optimization data on logout**

### Common Pitfalls to Avoid

❌ **DON'T:**
- Build locally (use GitHub Actions)
- Make optional mods required
- Skip null checks on entities
- Forget to clear dirty flags after save
- Ignore error handling in reflection code

✅ **DO:**
- Run error checking after every change
- Fix ALL errors before committing
- Use FastUtil collections for better performance
- Mark players dirty when data changes
- Clear optimization data on logout
- Test with and without optional mods

### Testing Checklist

Before pushing to GitHub:

- [ ] All compile errors fixed (check with get_errors tool)
- [ ] All files validated (no red squiggles)
- [ ] Changes explained in commit message
- [ ] Works without optional mods installed
- [ ] Works with optional mods installed
- [ ] No local build artifacts (build/, .gradle/)
- [ ] Dirty flags properly managed
- [ ] Sleeping system functioning correctly

### Version Tagging

Use semantic versioning: `v1.0.X`
- Increment patch version (X) for bug fixes and optimizations
- Increment minor version for new features
- Increment major version for breaking changes

### Credits

**Lithium Optimizations Inspired By:**
- [CaffeineMC/Lithium](https://github.com/CaffeineMC/lithium)
- Primary Developer: [2No2Name](https://github.com/2No2Name)
- Original Creator: [JellySquid](https://github.com/jellysquid3)
- License: LGPL-3.0

**PlayerSync Integration:**
- Original Mod: [PlayerSync by mlus-asuka](https://github.com/mlus-asuka/PlayerSync)

## Emergency Procedures

### If Compilation Fails on GitHub Actions:

1. Pull latest changes
2. Run `get_errors` tool on all files
3. Fix errors systematically
4. Re-validate with `get_errors`
5. Commit and push again
6. Monitor GitHub Actions

### If Errors Persist:

1. Check `mods.toml` for correct modId
2. Verify all imports are correct
3. Ensure FastUtil dependency in `build.gradle`
4. Check for typos in event handler annotations
5. Validate all file paths and package names

---

**Remember:** This is a UNIVERSAL performance addon. It must work perfectly with OR without optional mods installed. Every optimization must benefit ALL PlayerSync users, not just those with specific mods.
