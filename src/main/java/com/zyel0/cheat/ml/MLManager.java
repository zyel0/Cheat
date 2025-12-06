package com.zyel0.cheat.ml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zyel0.cheat.CheatPlugin;
import com.zyel0.cheat.data.AnomalyFlag;
import com.zyel0.cheat.data.MovementData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages ML model persistence and flag tracking
 */
public class MLManager {
    
    private final CheatPlugin plugin;
    private final MovementAnomalyDetector detector;
    private final Map<UUID, List<AnomalyFlag>> playerFlags;
    private final Map<UUID, AnomalyCountEntry> recentAnomalyCount;
    private final Gson gson;
    private final int flagThreshold;
    private final long anomalyWindowMs;
    
    public MLManager(CheatPlugin plugin) {
        this.plugin = plugin;
        this.playerFlags = new ConcurrentHashMap<>();
        this.recentAnomalyCount = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        
        // Read config values
        this.flagThreshold = plugin.getConfig().getInt("ml.flag-threshold", 2);
        this.anomalyWindowMs = 60000; // 1 minute window
        
        // Load or create detector
        this.detector = loadDetector();
    }
    
    /**
     * Helper class to track anomaly counts with timestamps
     */
    private static class AnomalyCountEntry {
        int count;
        long lastUpdate;
        
        AnomalyCountEntry(int count, long lastUpdate) {
            this.count = count;
            this.lastUpdate = lastUpdate;
        }
    }
    
    /**
     * Process movement and check for anomalies
     */
    public void processMovement(UUID playerId, String playerName, MovementData data) {
        double anomalyScore = detector.processMovement(data);
        
        // Only flag if trained and score is high
        if (detector.isTrained() && detector.isAnomaly(anomalyScore)) {
            handleAnomaly(playerId, playerName, data, anomalyScore);
        }
        
        // Clean up old anomaly counts
        cleanupAnomalyCounts();
    }
    
    /**
     * Handle detected anomaly
     */
    private void handleAnomaly(UUID playerId, String playerName, MovementData data, double score) {
        long now = System.currentTimeMillis();
        
        // Get or create anomaly count entry
        AnomalyCountEntry entry = recentAnomalyCount.get(playerId);
        
        if (entry == null || (now - entry.lastUpdate) > anomalyWindowMs) {
            // Reset if outside time window
            entry = new AnomalyCountEntry(1, now);
            recentAnomalyCount.put(playerId, entry);
        } else {
            // Increment count within window
            entry.count++;
            entry.lastUpdate = now;
        }
        
        // Only create flag if multiple anomalies detected
        if (entry.count >= flagThreshold) {
            String details = generateAnomalyDetails(data, score);
            AnomalyFlag flag = new AnomalyFlag(playerName, data, score, details);
            
            playerFlags.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>()).add(flag);
            
            // Log to console if debug enabled
            if (plugin.getConfig().getBoolean("ml.debug", false)) {
                plugin.getLogger().warning("Flagged " + playerName + ": " + flag.generateFlagMessage());
            }
            
            // Reset count after flagging
            recentAnomalyCount.remove(playerId);
        }
    }
    
    /**
     * Generate detailed anomaly description
     */
    private String generateAnomalyDetails(MovementData data, double score) {
        StringBuilder sb = new StringBuilder();
        
        if (score > 5.0) {
            sb.append("Extreme deviation detected. ");
        } else if (score > 3.5) {
            sb.append("High deviation from normal. ");
        } else {
            sb.append("Moderate deviation detected. ");
        }
        
        // Identify which aspect is most anomalous
        if (data.getSpeed() > 3.0) {
            sb.append("Excessive speed. ");
        }
        if (Math.abs(data.getAcceleration()) > 1.0) {
            sb.append("Unusual acceleration. ");
        }
        if (data.getVerticalSpeed() > 2.0 && !data.isFlying() && !data.isGliding()) {
            sb.append("Suspicious vertical movement. ");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Clean up old anomaly counts outside the time window
     */
    private void cleanupAnomalyCounts() {
        long now = System.currentTimeMillis();
        recentAnomalyCount.entrySet().removeIf(entry -> 
            (now - entry.getValue().lastUpdate) > anomalyWindowMs
        );
    }
    
    /**
     * Get flags for a player
     */
    public List<AnomalyFlag> getPlayerFlags(UUID playerId) {
        return playerFlags.getOrDefault(playerId, Collections.emptyList());
    }
    
    /**
     * Get all flags
     */
    public Map<UUID, List<AnomalyFlag>> getAllFlags() {
        return new HashMap<>(playerFlags);
    }
    
    /**
     * Clear flags for a player
     */
    public void clearPlayerFlags(UUID playerId) {
        playerFlags.remove(playerId);
        recentAnomalyCount.remove(playerId);
    }
    
    /**
     * Clear all flags
     */
    public void clearAllFlags() {
        playerFlags.clear();
        recentAnomalyCount.clear();
    }
    
    /**
     * Get the detector
     */
    public MovementAnomalyDetector getDetector() {
        return detector;
    }
    
    /**
     * Save detector model to disk
     */
    public void saveDetector() {
        File dataFolder = new File(plugin.getDataFolder(), "ml-data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File modelFile = new File(dataFolder, "model.json");
        
        try (Writer writer = new FileWriter(modelFile)) {
            ModelData modelData = new ModelData(
                detector.getTrainingStartTime(),
                detector.isTrained(),
                detector.getFeatureStats()
            );
            gson.toJson(modelData, writer);
            plugin.getLogger().info("ML model saved successfully");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save ML model: " + e.getMessage());
        }
    }
    
    /**
     * Load detector model from disk
     */
    private MovementAnomalyDetector loadDetector() {
        // Get config values
        int trainingPeriodDays = plugin.getConfig().getInt("ml.training-period-days", 7);
        int minSamples = plugin.getConfig().getInt("ml.min-training-samples", 1000);
        double threshold = plugin.getConfig().getDouble("ml.anomaly-threshold", 2.5);
        
        File dataFolder = new File(plugin.getDataFolder(), "ml-data");
        File modelFile = new File(dataFolder, "model.json");
        
        if (!modelFile.exists()) {
            plugin.getLogger().info("No existing ML model found. Starting fresh training period.");
            return new MovementAnomalyDetector(trainingPeriodDays, minSamples, threshold);
        }
        
        try (Reader reader = new FileReader(modelFile)) {
            ModelData modelData = gson.fromJson(reader, ModelData.class);
            plugin.getLogger().info("Loaded existing ML model (trained: " + modelData.isTrained + ")");
            
            return new MovementAnomalyDetector(
                trainingPeriodDays,
                minSamples,
                threshold,
                modelData.trainingStartTime,
                modelData.isTrained,
                modelData.featureStats
            );
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load ML model: " + e.getMessage());
            return new MovementAnomalyDetector(trainingPeriodDays, minSamples, threshold);
        }
    }
    
    /**
     * Data class for model serialization
     */
    private static class ModelData {
        long trainingStartTime;
        boolean isTrained;
        Map<String, DescriptiveStatistics[]> featureStats;
        
        ModelData(long trainingStartTime, boolean isTrained, 
                  Map<String, DescriptiveStatistics[]> featureStats) {
            this.trainingStartTime = trainingStartTime;
            this.isTrained = isTrained;
            this.featureStats = featureStats;
        }
    }
}
