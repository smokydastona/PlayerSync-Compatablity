package com.playersync.compat;

import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.inventory.ITravelersBackpackContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

/**
 * Compatibility module for Traveler's Backpack mod
 * Syncs backpack inventory, fluids, and other data across servers
 */
public class TravelersBackpackCompat {
    
    public static final String MOD_ID = "travelersbackpack";
    public static final String CAPABILITY_KEY = "TravelersBackpackData";
    
    /**
     * Checks if Traveler's Backpack mod is loaded
     */
    public static boolean isLoaded() {
        try {
            Class.forName("com.tiviacz.travelersbackpack.capability.CapabilityUtils");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Saves the player's Traveler's Backpack data to NBT
     * 
     * @param player The player whose backpack data to save
     * @return CompoundTag containing the backpack data, or null if no backpack is equipped
     */
    public static CompoundTag saveBackpackData(@NotNull Player player) {
        LazyOptional<ITravelersBackpackContainer> optional = CapabilityUtils.getBackpackCapability(player);
        
        if (optional.isPresent()) {
            CompoundTag tag = new CompoundTag();
            optional.ifPresent(backpack -> {
                // Save inventory
                CompoundTag inventoryTag = new CompoundTag();
                backpack.getInventory().save(inventoryTag);
                tag.put("Inventory", inventoryTag);
                
                // Save crafting inventory
                CompoundTag craftingTag = new CompoundTag();
                backpack.getCraftingInventory().save(craftingTag);
                tag.put("CraftingInventory", craftingTag);
                
                // Save fluid tanks
                CompoundTag tanksTag = new CompoundTag();
                backpack.getLeftTank().writeToNBT(tanksTag);
                tag.put("LeftTank", tanksTag);
                
                CompoundTag rightTankTag = new CompoundTag();
                backpack.getRightTank().writeToNBT(rightTankTag);
                tag.put("RightTank", rightTankTag);
                
                // Save settings
                tag.putBoolean("IsSleepingBagDeployed", backpack.isSleepingBagDeployed());
                
                // Save abilities (if equipped)
                CompoundTag settingsTag = new CompoundTag();
                backpack.getSettings().save(settingsTag);
                tag.put("Settings", settingsTag);
            });
            return tag;
        }
        
        return null;
    }
    
    /**
     * Loads the player's Traveler's Backpack data from NBT
     * 
     * @param player The player whose backpack data to load
     * @param data The CompoundTag containing the backpack data
     */
    public static void loadBackpackData(@NotNull Player player, @NotNull CompoundTag data) {
        LazyOptional<ITravelersBackpackContainer> optional = CapabilityUtils.getBackpackCapability(player);
        
        optional.ifPresent(backpack -> {
            // Load inventory
            if (data.contains("Inventory")) {
                backpack.getInventory().load(data.getCompound("Inventory"));
            }
            
            // Load crafting inventory
            if (data.contains("CraftingInventory")) {
                backpack.getCraftingInventory().load(data.getCompound("CraftingInventory"));
            }
            
            // Load fluid tanks
            if (data.contains("LeftTank")) {
                backpack.getLeftTank().readFromNBT(data.getCompound("LeftTank"));
            }
            
            if (data.contains("RightTank")) {
                backpack.getRightTank().readFromNBT(data.getCompound("RightTank"));
            }
            
            // Load settings
            if (data.contains("IsSleepingBagDeployed")) {
                backpack.setSleepingBagDeployed(data.getBoolean("IsSleepingBagDeployed"));
            }
            
            if (data.contains("Settings")) {
                backpack.getSettings().load(data.getCompound("Settings"));
            }
        });
    }
    
    /**
     * Marks the backpack as dirty to trigger synchronization
     * 
     * @param player The player whose backpack to mark as dirty
     */
    public static void markBackpackDirty(@NotNull Player player) {
        LazyOptional<ITravelersBackpackContainer> optional = CapabilityUtils.getBackpackCapability(player);
        optional.ifPresent(backpack -> {
            // Force capability synchronization
            if (player.level() != null && !player.level().isClientSide) {
                // Trigger a capability update
                backpack.getInventory().setChanged();
            }
        });
    }
}
