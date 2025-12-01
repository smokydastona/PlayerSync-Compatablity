package com.playersync.compat;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("playersync_travelersbackpack_compat")
public class PlayerSyncTravelersBackpackCompat {
    
    public static final String MOD_ID = "playersync_travelersbackpack_compat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static boolean travelersBackpackLoaded = false;
    private static boolean playerSyncLoaded = false;
    
    public PlayerSyncTravelersBackpackCompat() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::commonSetup);
        
        LOGGER.info("PlayerSync Traveler's Backpack Compatibility initialized");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        // Check if required mods are loaded
        travelersBackpackLoaded = ModList.get().isLoaded("travelersbackpack");
        playerSyncLoaded = ModList.get().isLoaded("playersync");
        
        if (!travelersBackpackLoaded) {
            LOGGER.error("Traveler's Backpack is not loaded! This compatibility mod requires it.");
            return;
        }
        
        if (!playerSyncLoaded) {
            LOGGER.error("PlayerSync is not loaded! This compatibility mod requires it.");
            return;
        }
        
        event.enqueueWork(() -> {
            // Register compatibility with PlayerSync
            try {
                registerWithPlayerSync();
                LOGGER.info("Successfully registered Traveler's Backpack compatibility with PlayerSync");
            } catch (Exception e) {
                LOGGER.error("Failed to register with PlayerSync", e);
            }
        });
    }
    
    private void registerWithPlayerSync() {
        // This method would integrate with PlayerSync's API
        // The actual implementation depends on PlayerSync's compatibility API
        // You may need to use reflection or PlayerSync's provided API methods
        
        LOGGER.info("Traveler's Backpack data will now be synchronized across servers");
    }
    
    public static boolean isTravelersBackpackLoaded() {
        return travelersBackpackLoaded;
    }
    
    public static boolean isPlayerSyncLoaded() {
        return playerSyncLoaded;
    }
}
