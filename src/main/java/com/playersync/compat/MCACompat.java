package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

/**
 * Compatibility module for Minecraft Comes Alive (MCA) Reborn mod
 * Syncs player marriage, family, and relationship data across servers
 */
public class MCACompat {
    
    public static final String MOD_ID = "mca";
    public static final String CAPABILITY_KEY = "MCAPlayerData";
    
    private static Class<?> playerDataClass;
    private static Method getPlayerDataMethod;
    private static Method serializeMethod;
    private static Method deserializeMethod;
    
    /**
     * Checks if MCA Reborn mod is loaded and initializes reflection
     */
    public static boolean isLoaded() {
        try {
            // Try to find MCA's PlayerData class
            playerDataClass = Class.forName("net.mca.entity.VillagerLike");
            
            // Alternative class names to try
            if (playerDataClass == null) {
                try {
                    playerDataClass = Class.forName("net.mca.data.PlayerData");
                } catch (ClassNotFoundException e1) {
                    try {
                        playerDataClass = Class.forName("net.mca.server.world.data.PlayerSaveData");
                    } catch (ClassNotFoundException e2) {
                        return false;
                    }
                }
            }
            
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Saves the player's MCA data to NBT using reflection
     * 
     * @param player The player whose MCA data to save
     * @return CompoundTag containing the MCA data, or null if no data exists
     */
    public static CompoundTag saveMCAData(@NotNull Player player) {
        try {
            if (player == null) {
                return null;
            }
            
            // Try to access MCA's player data through various possible methods
            CompoundTag mcaData = new CompoundTag();
            
            // Attempt 1: Check for capability attachment on player
            CompoundTag playerData = player.getPersistentData();
            if (playerData != null && playerData.contains("mca")) {
                mcaData.put("mca", playerData.get("mca"));
                return mcaData;
            }
            
            // Attempt 2: Try to find player data through ForgeData
            if (playerData != null && playerData.contains("ForgeData")) {
                CompoundTag forgeData = playerData.getCompound("ForgeData");
                if (forgeData.contains("mca:player_data")) {
                    mcaData.put("mca:player_data", forgeData.get("mca:player_data"));
                    return mcaData;
                }
            }
            
            // Attempt 3: Look for any MCA-related NBT tags
            if (playerData != null) {
                for (String key : playerData.getAllKeys()) {
                    if (key.toLowerCase().contains("mca")) {
                        mcaData.put(key, playerData.get(key));
                    }
                }
                
                if (!mcaData.isEmpty()) {
                    return mcaData;
                }
            }
            
            return null;
        } catch (Throwable t) {
            // Catch everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error saving MCA data - operation failed gracefully", t);
            return null;
        }
    }
    
    /**
     * Loads the player's MCA data from NBT using reflection
     * 
     * @param player The player whose MCA data to load
     * @param data The CompoundTag containing the MCA data
     */
    public static void loadMCAData(@NotNull Player player, @NotNull CompoundTag data) {
        try {
            if (player == null || data == null || data.isEmpty()) {
                return;
            }
            
            CompoundTag playerData = player.getPersistentData();
            if (playerData == null) {
                return;
            }
            
            // Restore all MCA-related data
            for (String key : data.getAllKeys()) {
                playerData.put(key, data.get(key));
            }
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded MCA data for player: {}", player.getName().getString());
        } catch (Throwable t) {
            // Catch everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error loading MCA data - operation failed gracefully", t);
        }
    }
}
