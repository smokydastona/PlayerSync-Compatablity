package com.playersync.compat;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

/**
 * Lithium-inspired optimizations for PlayerSync Performance Plus
 * 
 * Based on techniques from CaffeineMC/Lithium:
 * - FastUtil collections for better HashMap/HashSet performance
 * - Dirty flag system for change tracking
 * - Sleeping system to skip inactive entities
 * - Notification-based updates instead of polling
 * - Efficient caching with invalidation
 * 
 * @see <a href="https://github.com/CaffeineMC/lithium">Lithium</a>
 */
public class LithiumInspiredOptimizations {
    
    // ===== DATA STRUCTURES (Lithium-style FastUtil collections) =====
    
    /**
     * Dirty flag tracking - players with unsaved changes
     * Uses ObjectOpenHashSet for 20-40% better performance than HashSet
     */
    private static final Set<UUID> dirtyPlayers = new ObjectOpenHashSet<>();
    
    /**
     * Last modification timestamp for each player
     * Uses Object2LongOpenHashMap to avoid boxing/unboxing overhead
     */
    private static final Object2LongOpenHashMap<UUID> lastModificationTime = new Object2LongOpenHashMap<>();
    
    /**
     * Sleeping players - inactive players that don't need auto-save checks
     * Inspired by Lithium's block entity sleeping optimization
     */
    private static final Set<UUID> sleepingPlayers = new ObjectOpenHashSet<>();
    
    /**
     * Cache for compressed data to avoid repeated compression
     * Uses Object2ObjectOpenHashMap for better performance
     */
    private static final Object2ObjectOpenHashMap<UUID, CompoundTag> compressedDataCache = new Object2ObjectOpenHashMap<>();
    
    /**
     * Inactivity threshold in ticks (5 minutes = 6000 ticks)
     * Players inactive for this long are marked as "sleeping"
     */
    private static final long SLEEP_THRESHOLD_TICKS = 6000;
    
    static {
        // FastUtil optimization - set default return value to avoid boxing
        lastModificationTime.defaultReturnValue(-1L);
    }
    
    // ===== DIRTY FLAG SYSTEM =====
    
    /**
     * Mark a player as dirty (has unsaved changes)
     * Wake up sleeping players when they become dirty
     */
    public static void markDirty(UUID playerId) {
        dirtyPlayers.add(playerId);
        wakeUp(playerId);
        updateModificationTime(playerId);
        invalidateCache(playerId);
    }
    
    /**
     * Check if a player has unsaved changes
     */
    public static boolean isDirty(UUID playerId) {
        return dirtyPlayers.contains(playerId);
    }
    
    /**
     * Clear dirty flag after successful save
     */
    public static void clearDirty(UUID playerId) {
        dirtyPlayers.remove(playerId);
    }
    
    /**
     * Get all dirty players (for batch saving)
     */
    public static Set<UUID> getDirtyPlayers() {
        return dirtyPlayers;
    }
    
    // ===== SLEEPING SYSTEM (Lithium block entity sleeping concept) =====
    
    /**
     * Mark a player as sleeping (inactive, skip auto-save checks)
     */
    public static void sleep(UUID playerId) {
        sleepingPlayers.add(playerId);
        PlayerSyncTravelersBackpackCompat.LOGGER.debug("Player {} is now sleeping (inactive)", playerId);
    }
    
    /**
     * Wake up a player (becomes active again)
     */
    public static void wakeUp(UUID playerId) {
        if (sleepingPlayers.remove(playerId)) {
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Player {} woke up", playerId);
        }
    }
    
    /**
     * Check if a player is sleeping
     */
    public static boolean isSleeping(UUID playerId) {
        return sleepingPlayers.contains(playerId);
    }
    
    /**
     * Update sleeping status based on inactivity
     * Call this during auto-save checks
     */
    public static void updateSleepingStatus(UUID playerId, long currentTick) {
        long lastMod = lastModificationTime.getLong(playerId);
        
        if (lastMod == -1L) {
            // Never modified, mark as sleeping
            sleep(playerId);
            return;
        }
        
        long inactiveTicks = currentTick - lastMod;
        
        if (inactiveTicks > SLEEP_THRESHOLD_TICKS && !isDirty(playerId)) {
            // Been inactive for 5+ minutes and no pending changes
            sleep(playerId);
        }
    }
    
    // ===== MODIFICATION TIME TRACKING =====
    
    /**
     * Update modification time to current server tick
     */
    public static void updateModificationTime(UUID playerId) {
        long currentTick = getCurrentServerTick();
        lastModificationTime.put(playerId, currentTick);
    }
    
    /**
     * Get last modification time for a player
     * Returns -1 if never modified
     */
    public static long getLastModificationTime(UUID playerId) {
        return lastModificationTime.getLong(playerId);
    }
    
    /**
     * Check if player data was modified recently (within last 5 seconds)
     */
    public static boolean wasRecentlyModified(UUID playerId) {
        long lastMod = lastModificationTime.getLong(playerId);
        if (lastMod == -1L) return false;
        
        long currentTick = getCurrentServerTick();
        return (currentTick - lastMod) < 100; // 100 ticks = 5 seconds
    }
    
    // ===== CACHE MANAGEMENT =====
    
    /**
     * Cache compressed data for a player
     */
    public static void cacheCompressedData(UUID playerId, CompoundTag data) {
        compressedDataCache.put(playerId, data);
    }
    
    /**
     * Get cached compressed data
     * Returns null if not cached
     */
    public static CompoundTag getCachedCompressedData(UUID playerId) {
        return compressedDataCache.get(playerId);
    }
    
    /**
     * Invalidate cache when data changes
     */
    public static void invalidateCache(UUID playerId) {
        compressedDataCache.remove(playerId);
    }
    
    /**
     * Clear all cached data for a player
     */
    public static void clearPlayerData(UUID playerId) {
        dirtyPlayers.remove(playerId);
        sleepingPlayers.remove(playerId);
        lastModificationTime.remove(playerId);
        compressedDataCache.remove(playerId);
    }
    
    // ===== NOTIFICATION SYSTEM =====
    
    /**
     * Called when player inventory changes (notification-based instead of polling)
     * This replaces the need to constantly check hasDataChanged()
     */
    public static void onInventoryChange(ServerPlayer player) {
        markDirty(player.getUUID());
    }
    
    /**
     * Called when player NBT data changes
     */
    public static void onDataChange(ServerPlayer player) {
        markDirty(player.getUUID());
    }
    
    /**
     * Called when player equips/unequips items
     */
    public static void onEquipmentChange(ServerPlayer player) {
        markDirty(player.getUUID());
    }
    
    // ===== STATISTICS & MONITORING =====
    
    /**
     * Get statistics about optimization effectiveness
     */
    public static String getStatistics() {
        return String.format(
            "Lithium-inspired Optimizations Stats:\n" +
            "  Dirty Players: %d\n" +
            "  Sleeping Players: %d\n" +
            "  Cached Compressed Data: %d\n" +
            "  Tracked Players: %d",
            dirtyPlayers.size(),
            sleepingPlayers.size(),
            compressedDataCache.size(),
            lastModificationTime.size()
        );
    }
    
    /**
     * Check if auto-save should be skipped for a player
     * Combines all optimization checks
     */
    public static boolean shouldSkipAutoSave(UUID playerId, long currentTick) {
        // Skip if player is sleeping (inactive)
        if (isSleeping(playerId)) {
            return true;
        }
        
        // Skip if no dirty flags (no changes)
        if (!isDirty(playerId)) {
            return true;
        }
        
        // Update sleeping status based on inactivity
        updateSleepingStatus(playerId, currentTick);
        
        return false;
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Get current server tick count
     */
    private static long getCurrentServerTick() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getTickCount();
        }
        return 0;
    }
    
    /**
     * Clear all optimization data (call on server shutdown)
     */
    public static void clearAll() {
        dirtyPlayers.clear();
        sleepingPlayers.clear();
        lastModificationTime.clear();
        compressedDataCache.clear();
        PlayerSyncTravelersBackpackCompat.LOGGER.info("Cleared all Lithium-inspired optimization data");
    }
}
