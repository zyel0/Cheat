package com.zyel0.cheat.ml;

import com.zyel0.cheat.data.MovementData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ML-based anomaly detector using statistical learning
 * Learns normal movement patterns continuously with memory management
 */
public class MovementAnomalyDetector {
    
    private final int trainingPeriodDays;
    private final int minSamplesForTraining;
    private final double anomalyThreshold;
    
    private final Map<String, List<double[]>> trainingData;
    private final Map<String, DescriptiveStatistics[]> featureStats;
    private final long trainingStartTime;
    private boolean isTrained;
    
    // Memory management
    private int maxSamplesPerContext;
    private long maxMemoryMB;
    
    public MovementAnomalyDetector(int trainingPeriodDays, int minSamplesForTraining, double anomalyThreshold, long maxMemoryMB) {
        this.trainingPeriodDays = trainingPeriodDays;
        this.minSamplesForTraining = minSamplesForTraining;
        this.anomalyThreshold = anomalyThreshold;
        this.trainingData = new ConcurrentHashMap<>();
        this.featureStats = new ConcurrentHashMap<>();
        this.trainingStartTime = System.currentTimeMillis();
        this.isTrained = false;
        this.maxMemoryMB = maxMemoryMB;
        this.maxSamplesPerContext = calculateMaxSamplesPerContext(maxMemoryMB);
    }
    
    public MovementAnomalyDetector(int trainingPeriodDays, int minSamplesForTraining, double anomalyThreshold,
                                   long maxMemoryMB, long trainingStartTime, boolean isTrained,
                                   Map<String, DescriptiveStatistics[]> featureStats) {
        this.trainingPeriodDays = trainingPeriodDays;
        this.minSamplesForTraining = minSamplesForTraining;
        this.anomalyThreshold = anomalyThreshold;
        this.trainingData = new ConcurrentHashMap<>();
        this.featureStats = new ConcurrentHashMap<>(featureStats);
        this.trainingStartTime = trainingStartTime;
        this.isTrained = isTrained;
        this.maxMemoryMB = maxMemoryMB;
        this.maxSamplesPerContext = calculateMaxSamplesPerContext(maxMemoryMB);
    }
    
    private static final int ESTIMATED_MOVEMENT_CONTEXTS = 10; // gliding, flying, sprinting, etc.
    private static final int BYTES_PER_SAMPLE = 200; // Conservative estimate with overhead
    
    /**
     * Calculate maximum samples per context based on memory limit
     * Assumes ~16 features * 8 bytes per double = 128 bytes per sample
     * With overhead, estimate 200 bytes per sample
     */
    private int calculateMaxSamplesPerContext(long memoryMB) {
        long memoryBytes = memoryMB * 1024 * 1024;
        return (int) (memoryBytes / (BYTES_PER_SAMPLE * ESTIMATED_MOVEMENT_CONTEXTS));
    }
    
    /**
     * Process movement data - training and detection happen simultaneously after initial training
     */
    public double processMovement(MovementData data) {
        double[] features = data.extractFeatures();
        String context = getMovementContext(data);
        
        // Collect training data continuously for ongoing learning
        List<double[]> contextSamples = trainingData.computeIfAbsent(context, k -> new ArrayList<>());
        
        // Add sample with memory management
        synchronized (contextSamples) {
            contextSamples.add(features);
            
            // Remove oldest samples if exceeding memory limit
            if (contextSamples.size() > maxSamplesPerContext) {
                // Remove oldest 10% of samples efficiently
                int removeCount = maxSamplesPerContext / 10;
                if (removeCount > 0 && contextSamples.size() > removeCount) {
                    contextSamples.subList(0, removeCount).clear();
                }
            }
        }
        
        // Check if we should train/retrain the model
        if (shouldTrain()) {
            trainModel();
        }
        
        // Detect anomalies if trained
        if (isTrained) {
            return calculateAnomalyScore(features, context);
        }
        
        return 0.0; // No anomaly detection before initial training
    }
    
    /**
     * Get movement context (e.g., "gliding", "sprinting", "normal")
     */
    private String getMovementContext(MovementData data) {
        if (data.isGliding()) return "gliding";
        if (data.isFlying()) return "flying";
        if (data.isSprinting()) return "sprinting";
        if (data.isSneaking()) return "sneaking";
        if (data.isInWater()) return "water";
        if (data.isInLava()) return "lava";
        if (data.isOnGround()) return "ground";
        return "air";
    }
    
    /**
     * Check if still in training period
     */
    public boolean isInTrainingPeriod() {
        long elapsed = System.currentTimeMillis() - trainingStartTime;
        long trainingPeriod = trainingPeriodDays * 24L * 60L * 60L * 1000L;
        return elapsed < trainingPeriod;
    }
    
    /**
     * Check if we should train/retrain the model
     * Retrains periodically for continuous learning
     */
    private boolean shouldTrain() {
        int totalSamples = 0;
        for (List<double[]> samples : trainingData.values()) {
            totalSamples += samples.size();
        }
        
        // Initial training: need minimum samples
        if (!isTrained) {
            return totalSamples >= minSamplesForTraining;
        }
        
        // Continuous learning: retrain every 1000 new samples
        // This keeps the model updated with new patterns
        return totalSamples % 1000 == 0 && totalSamples > 0;
    }
    
    /**
     * Train the model on collected data
     * Uses streaming approach for memory efficiency
     */
    private synchronized void trainModel() {
        featureStats.clear();
        
        for (Map.Entry<String, List<double[]>> entry : trainingData.entrySet()) {
            String context = entry.getKey();
            List<double[]> samples = entry.getValue();
            
            if (samples.isEmpty()) continue;
            
            int numFeatures = samples.get(0).length;
            DescriptiveStatistics[] stats = new DescriptiveStatistics[numFeatures];
            
            // Use windowed statistics to limit memory usage
            for (int i = 0; i < numFeatures; i++) {
                stats[i] = new DescriptiveStatistics();
                stats[i].setWindowSize(Math.min(10000, maxSamplesPerContext)); // Limit window
            }
            
            // Calculate statistics for each feature
            synchronized (samples) {
                for (double[] sample : samples) {
                    for (int i = 0; i < numFeatures; i++) {
                        stats[i].addValue(sample[i]);
                    }
                }
            }
            
            featureStats.put(context, stats);
        }
        
        isTrained = true;
    }
    
    /**
     * Calculate anomaly score based on deviation from normal
     */
    private double calculateAnomalyScore(double[] features, String context) {
        DescriptiveStatistics[] stats = featureStats.get(context);
        
        if (stats == null) {
            // Unknown context - check against all contexts
            double maxScore = 0.0;
            for (DescriptiveStatistics[] contextStats : featureStats.values()) {
                double score = calculateDeviationScore(features, contextStats);
                maxScore = Math.max(maxScore, score);
            }
            return maxScore * 0.5; // Reduce score for unknown context
        }
        
        return calculateDeviationScore(features, stats);
    }
    
    /**
     * Calculate deviation score (normalized z-score)
     */
    private double calculateDeviationScore(double[] features, DescriptiveStatistics[] stats) {
        double totalDeviation = 0.0;
        int validFeatures = 0;
        
        for (int i = 0; i < features.length && i < stats.length; i++) {
            double mean = stats[i].getMean();
            double std = stats[i].getStandardDeviation();
            
            if (std > 0.0001) { // Avoid division by zero
                double zScore = Math.abs((features[i] - mean) / std);
                totalDeviation += zScore;
                validFeatures++;
            }
        }
        
        return validFeatures > 0 ? totalDeviation / validFeatures : 0.0;
    }
    
    /**
     * Check if the anomaly score indicates suspicious behavior
     */
    public boolean isAnomaly(double score) {
        return isTrained && score > anomalyThreshold;
    }
    
    public boolean isTrained() {
        return isTrained;
    }
    
    public long getTrainingStartTime() {
        return trainingStartTime;
    }
    
    public Map<String, DescriptiveStatistics[]> getFeatureStats() {
        return new HashMap<>(featureStats);
    }
    
    public int getTrainingSampleCount() {
        int total = 0;
        for (List<double[]> samples : trainingData.values()) {
            total += samples.size();
        }
        return total;
    }
    
    public long getTrainingTimeRemaining() {
        long elapsed = System.currentTimeMillis() - trainingStartTime;
        long trainingPeriod = trainingPeriodDays * 24L * 60L * 60L * 1000L;
        return Math.max(0, trainingPeriod - elapsed);
    }
    
    public int getTrainingPeriodDays() {
        return trainingPeriodDays;
    }
    
    public long getMaxMemoryMB() {
        return maxMemoryMB;
    }
    
    public void setMaxMemoryMB(long maxMemoryMB) {
        this.maxMemoryMB = maxMemoryMB;
        this.maxSamplesPerContext = calculateMaxSamplesPerContext(maxMemoryMB);
    }
    
    public int getMaxSamplesPerContext() {
        return maxSamplesPerContext;
    }
    
    public int getCurrentSampleCount() {
        int total = 0;
        for (List<double[]> samples : trainingData.values()) {
            total += samples.size();
        }
        return total;
    }
    
    public long getEstimatedMemoryUsageMB() {
        int totalSamples = getCurrentSampleCount();
        long bytesPerSample = 200; // Estimate with overhead
        return (totalSamples * bytesPerSample) / (1024 * 1024);
    }
}
