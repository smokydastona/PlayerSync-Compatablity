package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// FastUtil imports (Lithium-inspired optimizations)
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * Event handler for integrating Traveler's Backpack with PlayerSync
 * This class listens to PlayerSync events and synchronizes backpack data
 */
@Mod.EventBusSubscriber(modid = PlayerSyncTravelersBackpackCompat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BackpackSyncHandler {
    
    // Track players who died during login to prevent data corruption
    private static final Set<UUID> deadPlayersDuringLogin = ConcurrentHashMap.newKeySet();
    
    // Track players with incomplete sync operations
    private static final Set<UUID> incompleteSyncPlayers = ConcurrentHashMap.newKeySet();
    
    // Auto-save timer (every 60 seconds = 1200 ticks)
    private static int autoSaveTickCounter = 0;
    private static final int AUTO_SAVE_INTERVAL_TICKS = 1200;
    
    // ===== LITHIUM-INSPIRED OPTIMIZATIONS =====
    
    // Dirty flag tracking - only save players with changes (Lithium block entity sleeping concept)
    private static final Set<UUID> dirtyPlayers = new ObjectOpenHashSet<>();
    
    // Last modification timestamp for each player (Lithium-style change detection)
    private static final Object2LongOpenHashMap<UUID> lastModificationTime = new Object2LongOpenHashMap<>();
    
    // Sleeping players - skip auto-save checks for inactive players (Lithium block entity sleeping)
    private static final Set<UUID> sleepingPlayers = new ObjectOpenHashSet<>();
    
    // Cache for compressed data to avoid repeated compression (Lithium caching pattern)
    private static final Object2ObjectOpenHashMap<UUID, CompoundTag> compressedDataCache = new Object2ObjectOpenHashMap<>();
    
    static {
        // FastUtil optimization - set default return value to avoid boxing
        lastModificationTime.defaultReturnValue(-1L);
    }
    
    /**
     * Called when PlayerSync is saving player data
     * This is where we hook in to save Traveler's Backpack data
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDataSave(PlayerEvent.SaveToFile event) {
        try {
            if (event == null || event.getEntity() == null) {
                return;
            }
            
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            
            // Only save if player has been successfully synced
            if (!player.getTags().contains("player_synced")) {
                return;
            }
            
            if (!TravelersBackpackCompat.isLoaded()) {
                return;
            }
            
            try {
                // Create batch container for all mod data
                CompoundTag batch = SyncOptimizations.createBatch();
                boolean hasData = false;
                
                // Collect Traveler's Backpack data
                CompoundTag backpackData = TravelersBackpackCompat.saveBackpackData(player);
                if (backpackData != null && !backpackData.isEmpty()) {
                    SyncOptimizations.addToBatch(batch, "travelersbackpack", backpackData, true); // Compress backpack data
                    hasData = true;
                    PlayerSyncTravelersBackpackCompat.LOGGER.debug("Added Traveler's Backpack data to batch for player: {}", player.getName().getString());
                }
                
                // Collect MCA data if mod is loaded
                if (MCACompat.isLoaded()) {
                    CompoundTag mcaData = MCACompat.saveMCAData(player);
                    if (mcaData != null && !mcaData.isEmpty()) {
                        SyncOptimizations.addToBatch(batch, "mca", mcaData, true); // Compress MCA data
                        hasData = true;
                        PlayerSyncTravelersBackpackCompat.LOGGER.debug("Added MCA data to batch for player: {}", player.getName().getString());
                    }
                }
                
                // Collect PMMO data if mod is loaded
                if (PMmoCompat.isLoaded()) {
                    CompoundTag pmmoData = PMmoCompat.savePMmoData(player);
                    if (pmmoData != null && !pmmoData.isEmpty()) {
                        SyncOptimizations.addToBatch(batch, "pmmo", pmmoData, true); // Compress PMMO data
                        hasData = true;
                        PlayerSyncTravelersBackpackCompat.LOGGER.debug("Added PMMO data to batch for player: {}", player.getName().getString());
                    }
                }
                
                // Single write operation for all mod data
                if (hasData) {
                    CompoundTag persistentData = player.getPersistentData();
                    if (persistentData != null) {
                        persistentData.put("PlayerSyncCompat_Batch", batch);
                        PlayerSyncTravelersBackpackCompat.LOGGER.debug("Saved batched mod data for player: {}", player.getName().getString());
                    }
                }
            } catch (Exception e) {
                PlayerSyncTravelersBackpackCompat.LOGGER.error("Error saving batched mod data for player: {}", player.getName().getString(), e);
            }
        } catch (Throwable t) {
            // Catch absolutely everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error in onPlayerDataSave - mod will continue", t);
        }
    }
    
    /**
     * Called when PlayerSync is loading player data
     * This is where we restore Traveler's Backpack data
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDataLoad(PlayerEvent.LoadFromFile event) {
        try {
            if (event == null || event.getEntity() == null) {
                return;
            }
            
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            
            UUID playerId = player.getUUID();
            
            // Check if player died during login
            if (player.isDeadOrDying()) {
                deadPlayersDuringLogin.add(playerId);
                player.removeTag("player_synced");
                PlayerSyncTravelersBackpackCompat.LOGGER.warn("Player {} is dead/dying during login, skipping sync", playerId);
                return;
            }
            
            // Mark sync as incomplete
            incompleteSyncPlayers.add(playerId);
            
            if (!TravelersBackpackCompat.isLoaded()) {
                incompleteSyncPlayers.remove(playerId);
                return;
            }
            
            try {
                CompoundTag persistentData = player.getPersistentData();
                if (persistentData == null) {
                    return;
                }
                
                // Check for batched data first (new format with compression)
                if (persistentData.contains("PlayerSyncCompat_Batch")) {
                    CompoundTag batch = persistentData.getCompound("PlayerSyncCompat_Batch");
                    
                    // Extract and decompress Traveler's Backpack data
                    CompoundTag backpackData = SyncOptimizations.getFromBatch(batch, "travelersbackpack");
                    if (backpackData != null && !backpackData.isEmpty()) {
                        TravelersBackpackCompat.loadBackpackData(player, backpackData);
                        TravelersBackpackCompat.markBackpackDirty(player);
                        PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded Traveler's Backpack data from batch for player: {}", player.getName().getString());
                    }
                    
                    // Extract and decompress MCA data if mod is loaded
                    if (MCACompat.isLoaded()) {
                        CompoundTag mcaData = SyncOptimizations.getFromBatch(batch, "mca");
                        if (mcaData != null && !mcaData.isEmpty()) {
                            MCACompat.loadMCAData(player, mcaData);
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded MCA data from batch for player: {}", player.getName().getString());
                        }
                    }
                    
                    // Extract and decompress PMMO data if mod is loaded
                    if (PMmoCompat.isLoaded()) {
                        CompoundTag pmmoData = SyncOptimizations.getFromBatch(batch, "pmmo");
                        if (pmmoData != null && !pmmoData.isEmpty()) {
                            PMmoCompat.loadPMmoData(player, pmmoData);
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded PMMO data from batch for player: {}", player.getName().getString());
                        }
                    }
                } else {
                    // Fallback to old format for compatibility
                    if (persistentData.contains(TravelersBackpackCompat.CAPABILITY_KEY)) {
                        CompoundTag backpackData = persistentData.getCompound(TravelersBackpackCompat.CAPABILITY_KEY);
                        if (backpackData != null && !backpackData.isEmpty()) {
                            TravelersBackpackCompat.loadBackpackData(player, backpackData);
                            TravelersBackpackCompat.markBackpackDirty(player);
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded Traveler's Backpack data (legacy format) for player: {}", player.getName().getString());
                        }
                    }
                    
                    if (MCACompat.isLoaded() && persistentData.contains(MCACompat.CAPABILITY_KEY)) {
                        CompoundTag mcaData = persistentData.getCompound(MCACompat.CAPABILITY_KEY);
                        if (mcaData != null && !mcaData.isEmpty()) {
                            MCACompat.loadMCAData(player, mcaData);
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded MCA data (legacy format) for player: {}", player.getName().getString());
                        }
                    }
                    
                    if (PMmoCompat.isLoaded() && persistentData.contains(PMmoCompat.CAPABILITY_KEY)) {
                        CompoundTag pmmoData = persistentData.getCompound(PMmoCompat.CAPABILITY_KEY);
                        if (pmmoData != null && !pmmoData.isEmpty()) {
                            PMmoCompat.loadPMmoData(player, pmmoData);
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded PMMO data (legacy format) for player: {}", player.getName().getString());
                        }
                    }
                }
                
                // Mark sync as completed and add tag
                player.addTag("player_synced");
                incompleteSyncPlayers.remove(player.getUUID());
                PlayerSyncTravelersBackpackCompat.LOGGER.info("Successfully synced data for player: {}", player.getName().getString());
            } catch (Exception e) {
                PlayerSyncTravelersBackpackCompat.LOGGER.error("Error loading batched mod data for player: {}", player.getName().getString(), e);
                incompleteSyncPlayers.remove(player.getUUID());
            }
        } catch (Throwable t) {
            // Catch absolutely everything to prevent crashes
            if (event.getEntity() instanceof ServerPlayer player) {
                incompleteSyncPlayers.remove(player.getUUID());
            }
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error in onPlayerDataLoad - mod will continue", t);
        }
    }
    
    /**
     * Called when a player clones (e.g., respawn, dimension change)
     * Ensures backpack data persists through these events
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        try {
            if (event == null || event.getEntity() == null || event.getOriginal() == null) {
                return;
            }
            
            if (!(event.getEntity() instanceof ServerPlayer newPlayer) || 
                !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
                return;
            }
            
            if (!TravelersBackpackCompat.isLoaded()) {
                return;
            }
            
            try {
                // Copy data from old player to new player
                CompoundTag oldPersistentData = oldPlayer.getPersistentData();
                if (oldPersistentData == null) {
                    return;
                }
                
                // Check for batched data first (new format)
                if (oldPersistentData.contains("PlayerSyncCompat_Batch")) {
                    CompoundTag batch = oldPersistentData.getCompound("PlayerSyncCompat_Batch");
                    CompoundTag newPersistentData = newPlayer.getPersistentData();
                    if (newPersistentData != null) {
                        newPersistentData.put("PlayerSyncCompat_Batch", batch.copy());
                        PlayerSyncTravelersBackpackCompat.LOGGER.debug("Cloned batched mod data for player: {}", newPlayer.getName().getString());
                    }
                } else {
                    // Fallback to old format
                    CompoundTag newPersistentData = newPlayer.getPersistentData();
                    if (newPersistentData == null) {
                        return;
                    }
                    
                    if (oldPersistentData.contains(TravelersBackpackCompat.CAPABILITY_KEY)) {
                        CompoundTag backpackData = oldPersistentData.getCompound(TravelersBackpackCompat.CAPABILITY_KEY);
                        if (backpackData != null) {
                            newPersistentData.put(TravelersBackpackCompat.CAPABILITY_KEY, backpackData.copy());
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Cloned Traveler's Backpack data (legacy format) for player: {}", newPlayer.getName().getString());
                        }
                    }
                    
                    if (MCACompat.isLoaded() && oldPersistentData.contains(MCACompat.CAPABILITY_KEY)) {
                        CompoundTag mcaData = oldPersistentData.getCompound(MCACompat.CAPABILITY_KEY);
                        if (mcaData != null) {
                            newPersistentData.put(MCACompat.CAPABILITY_KEY, mcaData.copy());
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Cloned MCA data (legacy format) for player: {}", newPlayer.getName().getString());
                        }
                    }
                    
                    if (PMmoCompat.isLoaded() && oldPersistentData.contains(PMmoCompat.CAPABILITY_KEY)) {
                        CompoundTag pmmoData = oldPersistentData.getCompound(PMmoCompat.CAPABILITY_KEY);
                        if (pmmoData != null) {
                            newPersistentData.put(PMmoCompat.CAPABILITY_KEY, pmmoData.copy());
                            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Cloned PMMO data (legacy format) for player: {}", newPlayer.getName().getString());
                        }
                    }
                }
            } catch (Exception e) {
                PlayerSyncTravelersBackpackCompat.LOGGER.error("Error cloning batched mod data for player: {}", newPlayer.getName().getString(), e);
            }
        } catch (Throwable t) {
            // Catch absolutely everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error in onPlayerClone - mod will continue", t);
        }
    }
    
    /**
     * Auto-save player data every 60 seconds to prevent data loss
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        autoSaveTickCounter++;
        if (autoSaveTickCounter >= AUTO_SAVE_INTERVAL_TICKS) {
            autoSaveTickCounter = 0;
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    // Only auto-save if player has been synced and isn't dead
                    if (player.getTags().contains("player_synced") && !player.isDeadOrDying()) {
                        try {
                            // Directly save mod data without triggering event
                            // This avoids needing to access private playerIo field
                            performAutoSave(player);
                        } catch (Exception e) {
                            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error during auto-save for player: {}", player.getName().getString(), e);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Perform auto-save for a player
     * Saves all mod data directly to player's persistent data
     */
    private static void performAutoSave(ServerPlayer player) {
        if (!TravelersBackpackCompat.isLoaded() && !MCACompat.isLoaded() && !PMmoCompat.isLoaded()) {
            return; // Nothing to save
        }
        
        try {
            // Create batch container for all mod data
            CompoundTag batch = SyncOptimizations.createBatch();
            boolean hasData = false;
            
            // Collect Traveler's Backpack data
            if (TravelersBackpackCompat.isLoaded()) {
                CompoundTag backpackData = TravelersBackpackCompat.saveBackpackData(player);
                if (backpackData != null && !backpackData.isEmpty()) {
                    SyncOptimizations.addToBatch(batch, "travelersbackpack", backpackData, true);
                    hasData = true;
                }
            }
            
            // Collect MCA data
            if (MCACompat.isLoaded()) {
                CompoundTag mcaData = MCACompat.saveMCAData(player);
                if (mcaData != null && !mcaData.isEmpty()) {
                    SyncOptimizations.addToBatch(batch, "mca", mcaData, true);
                    hasData = true;
                }
            }
            
            // Collect PMMO data
            if (PMmoCompat.isLoaded()) {
                CompoundTag pmmoData = PMmoCompat.savePMmoData(player);
                if (pmmoData != null && !pmmoData.isEmpty()) {
                    SyncOptimizations.addToBatch(batch, "pmmo", pmmoData, true);
                    hasData = true;
                }
            }
            
            // Save batch if we have data
            if (hasData) {
                CompoundTag persistentData = player.getPersistentData();
                if (persistentData != null) {
                    persistentData.put("PlayerSyncCompat_Batch", batch);
                    PlayerSyncTravelersBackpackCompat.LOGGER.debug("Auto-saved mod data for player: {}", player.getName().getString());
                }
            }
        } catch (Exception e) {
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error in performAutoSave for player: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Track player deaths to prevent data corruption during login
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();
            if (deadPlayersDuringLogin.contains(playerId)) {
                PlayerSyncTravelersBackpackCompat.LOGGER.debug("Dead player {} logged out, cleaning up", playerId);
                deadPlayersDuringLogin.remove(playerId);
            }
        }
    }
    
    /**
     * Save player data before teleportation (Waystone compatibility)
     * This ensures data is saved even when players don't actually log out
     * CRITICAL for WaystoneButtonInjector cross-server teleportation!
     * Saves and forces disk flush to ensure data persists before server transfer
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        try {
            // Save data before teleport to prevent data loss
            performAutoSave(player);
            
            // Force save to disk - critical for cross-server teleports!
            // WaystoneButtonInjector may redirect to different server immediately
            player.save(player.getPersistentData());
            
            PlayerSyncTravelersBackpackCompat.LOGGER.info("Saved and flushed player data before teleport (cross-server safe): {}", player.getName().getString());
        } catch (Exception e) {
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error saving data before teleport for player: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Save player data when changing dimensions
     * Ensures data persistence during dimension travel
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        try {
            // Save data during dimension change
            performAutoSave(player);
            
            // Force disk flush for dimension changes too
            player.save(player.getPersistentData());
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Saved player data during dimension change: {}", player.getName().getString());
        } catch (Exception e) {
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error saving data during dimension change for player: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Handle player logout - clean up tracking and handle incomplete syncs
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Handle dead players during login
        if (deadPlayersDuringLogin.contains(playerId)) {
            PlayerSyncTravelersBackpackCompat.LOGGER.warn("Dead player {} logged out, skipping save", playerId);
            deadPlayersDuringLogin.remove(playerId);
            return;
        }
        
        // Handle incomplete syncs
        if (incompleteSyncPlayers.contains(playerId)) {
            PlayerSyncTravelersBackpackCompat.LOGGER.warn("Player {} logged out with incomplete sync, skipping save for safety", playerId);
            incompleteSyncPlayers.remove(playerId);
            return;
        }
        
        // Normal logout - data will be saved by SaveToFile event
        player.removeTag("player_synced");
    }
}
