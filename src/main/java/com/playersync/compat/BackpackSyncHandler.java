package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for integrating Traveler's Backpack with PlayerSync
 * This class listens to PlayerSync events and synchronizes backpack data
 */
@Mod.EventBusSubscriber(modid = PlayerSyncTravelersBackpackCompat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BackpackSyncHandler {
    
    /**
     * Called when PlayerSync is saving player data
     * This is where we hook in to save Traveler's Backpack data
     */
    @SubscribeEvent
    public static void onPlayerDataSave(PlayerEvent.SaveToFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        if (!TravelersBackpackCompat.isLoaded()) {
            return;
        }
        
        try {
            CompoundTag backpackData = TravelersBackpackCompat.saveBackpackData(player);
            
            if (backpackData != null && !backpackData.isEmpty()) {
                // Store in player's persistent data for PlayerSync to pick up
                CompoundTag persistentData = player.getPersistentData();
                persistentData.put(TravelersBackpackCompat.CAPABILITY_KEY, backpackData);
                
                PlayerSyncTravelersBackpackCompat.LOGGER.debug("Saved Traveler's Backpack data for player: {}", player.getName().getString());
            }
        } catch (Exception e) {
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error saving Traveler's Backpack data for player: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Called when PlayerSync is loading player data
     * This is where we restore Traveler's Backpack data
     */
    @SubscribeEvent
    public static void onPlayerDataLoad(PlayerEvent.LoadFromFile event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        if (!TravelersBackpackCompat.isLoaded()) {
            return;
        }
        
        try {
            CompoundTag persistentData = player.getPersistentData();
            
            if (persistentData.contains(TravelersBackpackCompat.CAPABILITY_KEY)) {
                CompoundTag backpackData = persistentData.getCompound(TravelersBackpackCompat.CAPABILITY_KEY);
                
                if (!backpackData.isEmpty()) {
                    TravelersBackpackCompat.loadBackpackData(player, backpackData);
                    TravelersBackpackCompat.markBackpackDirty(player);
                    
                    PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded Traveler's Backpack data for player: {}", player.getName().getString());
                }
            }
        } catch (Exception e) {
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error loading Traveler's Backpack data for player: {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Called when a player clones (e.g., respawn, dimension change)
     * Ensures backpack data persists through these events
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) || 
            !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }
        
        if (!TravelersBackpackCompat.isLoaded()) {
            return;
        }
        
        try {
            // Copy backpack data from old player to new player
            CompoundTag oldPersistentData = oldPlayer.getPersistentData();
            
            if (oldPersistentData.contains(TravelersBackpackCompat.CAPABILITY_KEY)) {
                CompoundTag backpackData = oldPersistentData.getCompound(TravelersBackpackCompat.CAPABILITY_KEY);
                
                CompoundTag newPersistentData = newPlayer.getPersistentData();
                newPersistentData.put(TravelersBackpackCompat.CAPABILITY_KEY, backpackData.copy());
                
                PlayerSyncTravelersBackpackCompat.LOGGER.debug("Cloned Traveler's Backpack data for player: {}", newPlayer.getName().getString());
            }
        } catch (Exception e) {
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Error cloning Traveler's Backpack data for player: {}", newPlayer.getName().getString(), e);
        }
    }
}
