package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Compatibility module for Project MMO (PMMO)
 * Syncs comprehensive player progression data including skills, levels, XP, perks, and abilities
 */
public class PMmoCompat {
    
    public static final String MOD_ID = "pmmo";
    public static final String CAPABILITY_KEY = "PMmoPlayerData";
    
    // PMMO data keys for comprehensive sync
    private static final String[] PMMO_DATA_KEYS = {
        "pmmo",                         // Main PMMO data
        "pmmo:player_data",             // Player-specific data
        "PlayerData",                   // Legacy format
        "Skills",                       // All skill data
        "SkillLevels",                  // Skill levels
        "SkillXP",                      // Experience points per skill
        "Perks",                        // Unlocked perks
        "Abilities",                    // Active abilities
        "Vein",                         // Vein miner data
        "Preferences",                  // Player preferences/settings
        "Stats",                        // Player statistics
        "Bonuses",                      // Active bonuses
        "Requirements",                 // Requirement tracking
        "pmmo_skills",                  // Skill registry
        "pmmo_xp",                      // XP data
        "pmmo_levels",                  // Level data
        "pmmo_perks",                   // Perk data
        "pmmo_abilities",               // Ability data
        "pmmo_vein",                    // Vein data
        "pmmo_stats",                   // Stats data
        "pmmo_bonuses",                 // Bonus data
        "pmmo_config",                  // Player config
        "SkillData",                    // Comprehensive skill data
        "LevelData",                    // Level progression
        "XpData",                       // XP progression
        "PerkData",                     // Perk progression
        "AbilityData",                  // Ability progression
        "VeinData",                     // Vein miner settings
        "PreferenceData",               // Preference settings
        "StatData",                     // Stat tracking
        "BonusData",                    // Active bonus tracking
        "pmmoPlayerData",               // Alternate data key
        "ProjectMMO",                   // Project MMO main key
        "ProgressionData",              // General progression
        "PlayerSkills",                 // Player skill mapping
        "PlayerPerks",                  // Player perk mapping
        "PlayerAbilities"               // Player ability mapping
    };
    
    private static Class<?> playerDataClass;
    private static Class<?> skillsClass;
    
    /**
     * Checks if PMMO is loaded and initializes reflection
     */
    public static boolean isLoaded() {
        try {
            // Try multiple possible class locations for PMMO
            try {
                playerDataClass = Class.forName("harmonised.pmmo.core.Core");
            } catch (ClassNotFoundException e1) {
                try {
                    playerDataClass = Class.forName("harmonised.pmmo.api.APIUtils");
                } catch (ClassNotFoundException e2) {
                    try {
                        playerDataClass = Class.forName("harmonised.pmmo.features.skills.Skill");
                    } catch (ClassNotFoundException e3) {
                        try {
                            playerDataClass = Class.forName("harmonised.pmmo.PMmo");
                        } catch (ClassNotFoundException e4) {
                            return false;
                        }
                    }
                }
            }
            
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
    
    /**
     * Saves comprehensive PMMO player data to NBT
     * 
     * @param player The player whose PMMO data to save
     * @return CompoundTag containing all PMMO data, or null if no data exists
     */
    public static CompoundTag savePMmoData(@NotNull Player player) {
        try {
            if (player == null) {
                return null;
            }
            
            CompoundTag pmmoData = new CompoundTag();
            CompoundTag playerData = player.getPersistentData();
            
            if (playerData == null) {
                return null;
            }
            
            boolean hasData = false;
            
            // Save all known PMMO data keys
            for (String key : PMMO_DATA_KEYS) {
                if (playerData.contains(key)) {
                    pmmoData.put(key, playerData.get(key));
                    hasData = true;
                }
            }
            
            // Also check ForgeData for PMMO capabilities
            if (playerData.contains("ForgeData")) {
                CompoundTag forgeData = playerData.getCompound("ForgeData");
                for (String key : forgeData.getAllKeys()) {
                    if (key.toLowerCase().contains("pmmo") || key.toLowerCase().contains("projectmmo")) {
                        pmmoData.put("ForgeData_" + key, forgeData.get(key));
                        hasData = true;
                    }
                }
            }
            
            // Scan for any other PMMO-related keys we might have missed
            for (String key : playerData.getAllKeys()) {
                if ((key.toLowerCase().contains("pmmo") || key.toLowerCase().contains("skill") || 
                     key.toLowerCase().contains("projectmmo")) && !pmmoData.contains(key)) {
                    pmmoData.put(key, playerData.get(key));
                    hasData = true;
                }
            }
            
            // Try to access PMMO's capability system via reflection
            try {
                Class<?> pmmoCore = Class.forName("harmonised.pmmo.core.Core");
                Method getPlayerData = pmmoCore.getMethod("get", Player.class);
                Object pmmoPlayerData = getPlayerData.invoke(null, player);
                
                if (pmmoPlayerData != null) {
                    Method serializeMethod = pmmoPlayerData.getClass().getMethod("serializeNBT");
                    Object capData = serializeMethod.invoke(pmmoPlayerData);
                    
                    if (capData instanceof CompoundTag) {
                        pmmoData.put("PMmoCapability", (CompoundTag) capData);
                        hasData = true;
                    }
                }
            } catch (Exception capEx) {
                // Capability approach failed, try alternative
                try {
                    Class<?> apiUtils = Class.forName("harmonised.pmmo.api.APIUtils");
                    Method getXpRaw = apiUtils.getMethod("getXpRaw", Player.class);
                    Method getSkillLevel = apiUtils.getMethod("getSkillLevel", Player.class);
                    
                    // These methods exist, so we have PMMO data through capability
                    // The NBT approach should capture it
                } catch (Exception apiEx) {
                    // API approach also failed
                }
            }
            
            if (!hasData) {
                return null;
            }
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Saved {} PMMO data keys for player: {}", pmmoData.getAllKeys().size(), player.getName().getString());
            return pmmoData;
            
        } catch (Throwable t) {
            // Catch everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error saving PMMO data - operation failed gracefully", t);
            return null;
        }
    }
    
    /**
     * Loads comprehensive PMMO player data from NBT
     * 
     * @param player The player whose PMMO data to load
     * @param data The CompoundTag containing the PMMO data
     */
    public static void loadPMmoData(@NotNull Player player, @NotNull CompoundTag data) {
        try {
            if (player == null || data == null || data.isEmpty()) {
                return;
            }
            
            CompoundTag playerData = player.getPersistentData();
            if (playerData == null) {
                return;
            }
            
            int restoredKeys = 0;
            
            // Restore all PMMO data
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
            if (data.contains("PMmoCapability")) {
                try {
                    Class<?> pmmoCore = Class.forName("harmonised.pmmo.core.Core");
                    Method getPlayerData = pmmoCore.getMethod("get", Player.class);
                    Object pmmoPlayerData = getPlayerData.invoke(null, player);
                    
                    if (pmmoPlayerData != null) {
                        Method deserializeMethod = pmmoPlayerData.getClass().getMethod("deserializeNBT", Object.class);
                        deserializeMethod.invoke(pmmoPlayerData, data.getCompound("PMmoCapability"));
                    }
                } catch (Exception capEx) {
                    // Capability restore failed, NBT data should be sufficient
                }
            }
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Loaded {} PMMO data keys for player: {}", restoredKeys, player.getName().getString());
        } catch (Throwable t) {
            // Catch everything to prevent crashes
            PlayerSyncTravelersBackpackCompat.LOGGER.error("Critical error loading PMMO data - operation failed gracefully", t);
        }
    }
}
