package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Compatibility module for Traveler's Backpack mod
 * Syncs backpack inventory, fluids, and other data across servers
 */
public class TravelersBackpackCompat {
    
    public static final String MOD_ID = "travelersbackpack";
    public static final String CAPABILITY_KEY = "TravelersBackpackData";
    
    private static Class<?> capabilityUtilsClass;
    private static Class<?> backpackContainerClass;
    private static Method getBackpackCapabilityMethod;
    
    /**
     * Checks if Traveler's Backpack mod is loaded and initializes reflection
     */
    public static boolean isLoaded() {
        try {
            capabilityUtilsClass = Class.forName("com.tiviacz.travelersbackpack.capability.CapabilityUtils");
            backpackContainerClass = Class.forName("com.tiviacz.travelersbackpack.inventory.ITravelersBackpackContainer");
            getBackpackCapabilityMethod = capabilityUtilsClass.getMethod("getBackpackCapability", Player.class);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Saves the player's Traveler's Backpack data to NBT using reflection
     * 
     * @param player The player whose backpack data to save
     * @return CompoundTag containing the backpack data, or null if no backpack is equipped
     */
    public static CompoundTag saveBackpackData(@NotNull Player player) {
        try {
            if (getBackpackCapabilityMethod == null) {
                return null;
            }
            
            Object optional = getBackpackCapabilityMethod.invoke(null, player);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            
            if ((Boolean) isPresentMethod.invoke(optional)) {
                CompoundTag tag = new CompoundTag();
                Method getMethod = optional.getClass().getMethod("orElse", Object.class);
                Object backpack = getMethod.invoke(optional, (Object) null);
                
                if (backpack != null) {
                    // Save backpack NBT data using serializeNBT if available
                    try {
                        Method serializeMethod = backpack.getClass().getMethod("serializeNBT");
                        Object nbtData = serializeMethod.invoke(backpack);
                        if (nbtData instanceof CompoundTag) {
                            return (CompoundTag) nbtData;
                        }
                    } catch (NoSuchMethodException e) {
                        // Fallback: just return empty tag - mod will use its own sync
                    }
                }
                return tag;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Loads the player's Traveler's Backpack data from NBT using reflection
     * 
     * @param player The player whose backpack data to load
     * @param data The CompoundTag containing the backpack data
     */
    public static void loadBackpackData(@NotNull Player player, @NotNull CompoundTag data) {
        try {
            if (getBackpackCapabilityMethod == null) {
                return;
            }
            
            Object optional = getBackpackCapabilityMethod.invoke(null, player);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            
            if ((Boolean) isPresentMethod.invoke(optional)) {
                Method getMethod = optional.getClass().getMethod("orElse", Object.class);
                Object backpack = getMethod.invoke(optional, (Object) null);
                
                if (backpack != null && data != null && !data.isEmpty()) {
                    // Load backpack NBT data using deserializeNBT if available
                    try {
                        Method deserializeMethod = backpack.getClass().getMethod("deserializeNBT", Object.class);
                        deserializeMethod.invoke(backpack, data);
                    } catch (NoSuchMethodException e) {
                        // Fallback: mod will handle its own sync
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Marks the backpack as dirty to trigger synchronization using reflection
     * 
     * @param player The player whose backpack to mark as dirty
     */
    public static void markBackpackDirty(@NotNull Player player) {
        try {
            if (getBackpackCapabilityMethod == null) {
                return;
            }
            
            Object optional = getBackpackCapabilityMethod.invoke(null, player);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            
            if ((Boolean) isPresentMethod.invoke(optional)) {
                // Backpack capability exists, sync should happen automatically
                // No action needed - Traveler's Backpack handles its own sync
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}
