package com.playersync.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for optimizing data sync operations
 * Provides compression, batching, and performance improvements
 */
public class SyncOptimizations {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerSyncCompat");
    
    private static final int COMPRESSION_THRESHOLD = 512; // Compress if data is larger than 512 bytes
    
    /**
     * Compresses NBT data using GZIP compression
     * Only compresses if the data size exceeds the threshold
     * 
     * @param tag The CompoundTag to compress
     * @return Compressed CompoundTag with metadata, or original if too small
     */
    public static CompoundTag compressData(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return tag;
        }
        
        try {
            // Estimate size
            ByteArrayOutputStream testStream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, testStream);
            byte[] uncompressed = testStream.toByteArray();
            
            // Only compress if above threshold
            if (uncompressed.length < COMPRESSION_THRESHOLD) {
                return tag;
            }
            
            // Compress the data
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(compressed)) {
                gzip.write(uncompressed);
            }
            
            byte[] compressedData = compressed.toByteArray();
            
            // Only use compression if it actually saves space
            if (compressedData.length >= uncompressed.length) {
                return tag;
            }
            
            // Store compressed data with metadata
            CompoundTag result = new CompoundTag();
            result.putBoolean("compressed", true);
            result.putByteArray("data", compressedData);
            result.putInt("originalSize", uncompressed.length);
            
            PlayerSyncTravelersBackpackCompat.LOGGER.debug("Compressed data from {} to {} bytes ({}% reduction)", 
                uncompressed.length, compressedData.length, 
                (100 - (compressedData.length * 100 / uncompressed.length)));
            
            return result;
        } catch (Exception e) {
            LOGGER.warn("Failed to compress data, using uncompressed", e);
            return tag;
        }
    }
    
    /**
     * Decompresses NBT data that was compressed by compressData()
     * 
     * @param tag The potentially compressed CompoundTag
     * @return Decompressed CompoundTag, or original if not compressed
     */
    public static CompoundTag decompressData(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return tag;
        }
        
        // Check if data is compressed
        if (!tag.getBoolean("compressed")) {
            return tag;
        }
        
        try {
            byte[] compressedData = tag.getByteArray("data");
            
            // Decompress
            ByteArrayInputStream input = new ByteArrayInputStream(compressedData);
            try (GZIPInputStream gzip = new GZIPInputStream(input)) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = gzip.read(buffer)) > 0) {
                    output.write(buffer, 0, len);
                }
                
                byte[] decompressed = output.toByteArray();
                ByteArrayInputStream nbtInput = new ByteArrayInputStream(decompressed);
                return NbtIo.readCompressed(nbtInput);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to decompress data", e);
            return tag;
        }
    }
    
    /**
     * Creates a batched data container for multiple mod data
     * Reduces overhead by combining all mod data into a single operation
     * 
     * @return Empty batch container
     */
    public static CompoundTag createBatch() {
        CompoundTag batch = new CompoundTag();
        batch.putLong("timestamp", System.currentTimeMillis());
        batch.putString("version", "1.0");
        return batch;
    }
    
    /**
     * Adds mod data to a batch container with optional compression
     * 
     * @param batch The batch container
     * @param key The mod data key
     * @param data The mod data
     * @param compress Whether to compress this data
     */
    public static void addToBatch(CompoundTag batch, String key, CompoundTag data, boolean compress) {
        if (data == null || data.isEmpty()) {
            return;
        }
        
        CompoundTag toStore = compress ? compressData(data) : data;
        batch.put(key, toStore);
    }
    
    /**
     * Extracts mod data from a batch container with automatic decompression
     * 
     * @param batch The batch container
     * @param key The mod data key
     * @return The mod data, or null if not present
     */
    public static CompoundTag getFromBatch(CompoundTag batch, String key) {
        if (batch == null || !batch.contains(key)) {
            return null;
        }
        
        CompoundTag data = batch.getCompound(key);
        return decompressData(data);
    }
    
    /**
     * Checks if data has changed since last sync
     * Uses simple hash comparison to avoid syncing unchanged data
     * 
     * @param oldData Previous data
     * @param newData Current data
     * @return true if data has changed
     */
    public static boolean hasDataChanged(CompoundTag oldData, CompoundTag newData) {
        if (oldData == null && newData == null) {
            return false;
        }
        if (oldData == null || newData == null) {
            return true;
        }
        
        // Quick size check
        if (oldData.getAllKeys().size() != newData.getAllKeys().size()) {
            return true;
        }
        
        // Compare hashes for performance
        return oldData.hashCode() != newData.hashCode();
    }
}
