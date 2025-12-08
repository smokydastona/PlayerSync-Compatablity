package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Compatibility module for Minecraft Comes Alive (MCA) Reborn mod
 * Syncs comprehensive player data including marriage, family, traits, mood, village rank, and relationships
 */
public class MCACompat {
    
    public static final String MOD_ID = "mca";
    public static final String CAPABILITY_KEY = "MCAPlayerData";
    
    // MCA data keys for comprehensive sync
    private static final String[] MCA_DATA_KEYS = {
        "mca",                          // Main MCA data
        "mca:player_data",              // Player-specific data
        "PlayerData",                   // Legacy format
        "SpouseUUID",                   // Marriage partner
        "SpouseName",                   // Spouse display name
        "Children",                     // List of children
        "ChildrenUUIDs",                // Children identifiers
        "Hearts",                       // Relationship hearts with NPCs
        "Mood",                         // Current mood/emotional state
        "Traits",                       // Personality traits
        "VillageRank",                  // Village standing/rank
        "VillageReputation",            // Reputation points
        "IsMonarch",                    // Kingdom/monarch status
        "Destiny",                      // Destiny quest progress
        "DestinyProgress",              // Destiny completion
        "Baby",                         // Baby/pregnancy state
        "BabyAge",                      // Baby growth progress
        "Gifts",                        // Gift history
        "GiftCooldown",                 // Gift interaction cooldown
        "Interactions",                 // Daily interaction tracking
        "LastInteraction",              // Last interaction timestamp
        "MarriageState",                // Marriage status/state
        "FamilyTree",                   // Extended family data
        "Genetics",                     // Player genetics (for offspring)
        "Procreation",                  // Procreation settings
        "PlayerSaveData",               // MCA save data structure
        "VillagerEditor",               // Editor mode data
        "InfectedVillagers",            // Infection status (for special events)
        "mcaPlayerData",                // Alternate data key
        "RelationshipData",             // Comprehensive relationship info
        "VillageUUID",                  // Home village identifier
        "VillageName",                  // Village name
        "TaxPaid",                      // Tax payment status
        "QuestProgress"                 // Quest completion tracking
    };
    
    private static Class<?> playerDataClass;
    private static Class<?> playerSaveDataClass;
    private static Method getPlayerDataMethod;
    private static Boolean loaded = null; // Lazy loading cache
    
    /**
     * MCA integration DISABLED - MCA stores villager family trees globally, not per-player
     * Syncing this data causes corruption of family trees across the server
     * 
     * @return Always returns false to disable MCA sync
     */
    public static boolean isLoaded() {
        // ALWAYS RETURN FALSE - DO NOT SYNC MCA DATA
        // MCA family trees are server-wide data, not player-specific
        // Attempting to sync this corrupts villager relationships
        loaded = false;
        return false;
    }
    
    /**
     * Saves comprehensive MCA player data to NBT
     * 
     * @param player The player whose MCA data to save
     * @return CompoundTag containing all MCA data, or null if no data exists
     */
    public static CompoundTag saveMCAData(@NotNull Player player) {
        try {
            if (player == null) {
                return null;
            }
            
            CompoundTag mcaData = new CompoundTag();
            CompoundTag playerData = player.getPersistentData();
            
            if (playerData == null) {
                return null;
            }
            
            boolean hasData = false;
            
            // Save all known MCA data keys
            for (String key : MCA_DATA_KEYS) {
                if (playerData.contains(key)) {
                    mcaData.put(key, playerData.get(key));
                    hasData = true;
                }
            }
            
            // Also check ForgeData for MCA capabilities
            if (playerData.contains("ForgeData")) {
                CompoundTag forgeData = playerData.getCompound("ForgeData");
                for (String key : forgeData.getAllKeys()) {
                    if (key.toLowerCase().contains("mca")) {
                        mcaData.put("ForgeData_" + key, forgeData.get(key));
                        hasData = true;
                    }
                }
            }
            
            // Scan for any other MCA-related keys we might have missed
            for (String key : playerData.getAllKeys()) {
                if (key.toLowerCase().contains("mca") && !mcaData.contains(key)) {
                    mcaData.put(key, playerData.get(key));
                    hasData = true;
                }
            }
            
            // Try to access MCA's capability system via reflection
            try {
                Class<?> mcaCapClass = Class.forName("net.mca.MCA");
                Field capField = mcaCapClass.getDeclaredField("PLAYER_CAPABILITY");
                capField.setAccessible(true);
                Object capability = capField.get(null);
                
                if (capability != null) {
                    Method getCapMethod = player.getClass().getMethod("getCapability", capability.getClass());
                    Object playerCap = getCapMethod.invoke(player, capability);
                    
                    if (playerCap != null) {
                        Method serializeMethod = playerCap.getClass().getMethod("serializeNBT");
                        Object capData = serializeMethod.invoke(playerCap);
                        
                        if (capData instanceof CompoundTag) {
                            mcaData.put("MCACapability", (CompoundTag) capData);
                            hasData = true;
                        }
                    }
                }
            } catch (Exception capEx) {
                // Capability approach failed, continue with NBT approach
            }
            
            if (!hasData) {
                return null;
            }
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Saved {} MCA data keys for player: {}", mcaData.getAllKeys().size(), player.getName().getString());
            return mcaData;
            
        } catch (Throwable t) {
            // Catch everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error saving MCA data - operation failed gracefully", t);
            return null;
        }
    }
    
    /**
     * Loads comprehensive MCA player data from NBT
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
            
            int restoredKeys = 0;
            
            // Restore all MCA data
            for (String key : data.getAllKeys()) {
                if (key.startsWith("ForgeData_")) {
                    // Restore ForgeData capabilities
                    String actualKey = key.substring("ForgeData_".length());
                    if (!playerData.contains("ForgeData")) {
                        playerData.put("ForgeData", new CompoundTag());
                    }
                    CompoundTag forgeData = playerData.getCompound("ForgeData");
                    forgeData.put(actualKey, data.get(key));
                } else {
                    // Restore direct player data
                    playerData.put(key, data.get(key));
                }
                restoredKeys++;
            }
            
            // Try to restore capability data if available
            if (data.contains("MCACapability")) {
                try {
                    Class<?> mcaCapClass = Class.forName("net.mca.MCA");
                    Field capField = mcaCapClass.getDeclaredField("PLAYER_CAPABILITY");
                    capField.setAccessible(true);
                    Object capability = capField.get(null);
                    
                    if (capability != null) {
                        Method getCapMethod = player.getClass().getMethod("getCapability", capability.getClass());
                        Object playerCap = getCapMethod.invoke(player, capability);
                        
                        if (playerCap != null) {
                            Method deserializeMethod = playerCap.getClass().getMethod("deserializeNBT", Object.class);
                            deserializeMethod.invoke(playerCap, data.getCompound("MCACapability"));
                        }
                    }
                } catch (Exception capEx) {
                    // Capability restore failed, NBT data should be sufficient
                }
            }
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded {} MCA data keys for player: {}", restoredKeys, player.getName().getString());
        } catch (Throwable t) {
            // Catch everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error loading MCA data - operation failed gracefully", t);
        }
    }
}
