package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.*;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Handles asynchronous data sync operations to prevent server lag
 * Uses a thread pool to process save/load operations in the background
 */
public class AsyncSyncManager {
    
    private static final int THREAD_POOL_SIZE = 2;
    private static final int QUEUE_CAPACITY = 100;
    private static final long TIMEOUT_SECONDS = 5;
    
    private static final ExecutorService executor = new ThreadPoolExecutor(
        THREAD_POOL_SIZE, 
        THREAD_POOL_SIZE,
        60L, 
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(QUEUE_CAPACITY),
        new ThreadPoolExecutor.CallerRunsPolicy() // Fallback to sync if queue full
    );
    
    // Cache for async results
    private static final Map<UUID, CompletableFuture<CompoundTag>> pendingSaves = new ConcurrentHashMap<>();
    private static final Map<UUID, CompletableFuture<Void>> pendingLoads = new ConcurrentHashMap<>();
    
    /**
     * Asynchronously saves player mod data
     * Returns immediately, actual save happens on background thread
     * 
     * @param player The player to save data for
     * @return Future that completes when save is done
     */
    public static CompletableFuture<CompoundTag> saveDataAsync(ServerPlayer player) {
        UUID playerId = player.getUUID();
        
        // Cancel any pending save for this player
        CompletableFuture<CompoundTag> existing = pendingSaves.get(playerId);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }
        
        CompletableFuture<CompoundTag> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Create batch on background thread
                CompoundTag batch = SyncOptimizations.createBatch();
                boolean hasData = false;
                
                // Collect all mod data
                if (TravelersBackpackCompat.isLoaded()) {
                    CompoundTag backpackData = TravelersBackpackCompat.saveBackpackData(player);
                    if (backpackData != null && !backpackData.isEmpty()) {
                        SyncOptimizations.addToBatch(batch, "travelersbackpack", backpackData, true);
                        hasData = true;
                    }
                }
                
                if (PMmoCompat.isLoaded()) {
                    CompoundTag pmmoData = PMmoCompat.savePMmoData(player);
                    if (pmmoData != null && !pmmoData.isEmpty()) {
                        SyncOptimizations.addToBatch(batch, "pmmo", pmmoData, true);
                        hasData = true;
                    }
                }
                
                return hasData ? batch : null;
            } catch (Exception e) {
                PlayerSyncTravelersBackpackCompat.LOGGER.error("Async save failed for player: {}", player.getName().getString(), e);
                return null;
            }
        }, executor);
        
        pendingSaves.put(playerId, future);
        
        // Clean up completed futures
        future.whenComplete((result, error) -> {
            pendingSaves.remove(playerId);
        });
        
        return future;
    }
    
    /**
     * Asynchronously loads player mod data
     * Returns immediately, actual load happens on background thread
     * 
     * @param player The player to load data for
     * @param batch The batched data to load
     * @return Future that completes when load is done
     */
    public static CompletableFuture<Void> loadDataAsync(ServerPlayer player, CompoundTag batch) {
        UUID playerId = player.getUUID();
        
        // Cancel any pending load for this player
        CompletableFuture<Void> existing = pendingLoads.get(playerId);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // Extract and load all mod data on background thread
                if (TravelersBackpackCompat.isLoaded()) {
                    CompoundTag backpackData = SyncOptimizations.getFromBatch(batch, "travelersbackpack");
                    if (backpackData != null && !backpackData.isEmpty()) {
                        TravelersBackpackCompat.loadBackpackData(player, backpackData);
                        TravelersBackpackCompat.markBackpackDirty(player);
                    }
                }
                
                if (PMmoCompat.isLoaded()) {
                    CompoundTag pmmoData = SyncOptimizations.getFromBatch(batch, "pmmo");
                    if (pmmoData != null && !pmmoData.isEmpty()) {
                        PMmoCompat.loadPMmoData(player, pmmoData);
                    }
                }
            } catch (Exception e) {
                PlayerSyncTravelersBackpackCompat.LOGGER.error("Async load failed for player: {}", player.getName().getString(), e);
            }
        }, executor);
        
        pendingLoads.put(playerId, future);
        
        // Clean up completed futures
        future.whenComplete((result, error) -> {
            pendingLoads.remove(playerId);
        });
        
        return future;
    }
    
    /**
     * Waits for all pending operations to complete
     * Should be called on server shutdown
     */
    public static void shutdown() {
        PlayerSyncTravelersBackpackCompat.LOGGER.info("Shutting down async sync manager...");
        
        // Wait for pending operations
        CompletableFuture.allOf(
            Stream.concat(
                pendingSaves.values().stream(),
                pendingLoads.values().stream()
            ).toArray(CompletableFuture[]::new)
        ).orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
         .exceptionally(ex -> null)
         .join();
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        PlayerSyncTravelersBackpackCompat.LOGGER.info("Async sync manager shut down");
    }
    
    /**
     * Gets the number of pending operations
     */
    public static int getPendingOperationCount() {
        return pendingSaves.size() + pendingLoads.size();
    }
}
