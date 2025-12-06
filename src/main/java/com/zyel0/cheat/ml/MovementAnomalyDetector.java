package com.zyel0.cheat.ml;

import com.zyel0.cheat.data.MovementData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ML-based anomaly detector using statistical learning
 * Learns normal movement patterns during training period
 */
public class MovementAnomalyDetector {
    
    private final int trainingPeriodDays;
    private final int minSamplesForTraining;
    private final double anomalyThreshold;
    
    private final Map<String, List<double[]>> trainingData;
    private final Map<String, DescriptiveStatistics[]> featureStats;
    private final long trainingStartTime;
    private boolean isTrained;
    
    public MovementAnomalyDetector(int trainingPeriodDays, int minSamplesForTraining, double anomalyThreshold) {
        this.trainingPeriodDays = trainingPeriodDays;
        this.minSamplesForTraining = minSamplesForTraining;
        this.anomalyThreshold = anomalyThreshold;
        this.trainingData = new ConcurrentHashMap<>();
        this.featureStats = new ConcurrentHashMap<>();
        this.trainingStartTime = System.currentTimeMillis();
        this.isTrained = false;
    }
    
    public MovementAnomalyDetector(int trainingPeriodDays, int minSamplesForTraining, double anomalyThreshold,
                                   long trainingStartTime, boolean isTrained,
                                   Map<String, DescriptiveStatistics[]> featureStats) {
        this.trainingPeriodDays = trainingPeriodDays;
        this.minSamplesForTraining = minSamplesForTraining;
        this.anomalyThreshold = anomalyThreshold;
        this.trainingData = new ConcurrentHashMap<>();
        this.featureStats = new ConcurrentHashMap<>(featureStats);
        this.trainingStartTime = trainingStartTime;
        this.isTrained = isTrained;
    }
    
    /**
     * Process movement data - either for training or detection
     */
    public double processMovement(MovementData data) {
        double[] features = data.extractFeatures();
        String context = getMovementContext(data);
        
        if (isInTrainingPeriod()) {
            // Collect training data
            trainingData.computeIfAbsent(context, k -> new ArrayList<>()).add(features);
            
            // Check if we should train the model
            if (shouldTrain()) {
                trainModel();
            }
            
            return 0.0; // No anomaly during training
        } else if (isTrained) {
            // Detect anomalies
            return calculateAnomalyScore(features, context);
        }
        
        return 0.0;
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
     * Check if we have enough data to train
     */
    private boolean shouldTrain() {
        if (isTrained) return false;
        
        int totalSamples = 0;
        for (List<double[]> samples : trainingData.values()) {
            totalSamples += samples.size();
        }
        
        return totalSamples >= minSamplesForTraining;
    }
    
    /**
     * Train the model on collected data
     */
    private void trainModel() {
        featureStats.clear();
        
        for (Map.Entry<String, List<double[]>> entry : trainingData.entrySet()) {
            String context = entry.getKey();
            List<double[]> samples = entry.getValue();
            
            if (samples.isEmpty()) continue;
            
            int numFeatures = samples.get(0).length;
            DescriptiveStatistics[] stats = new DescriptiveStatistics[numFeatures];
            
            for (int i = 0; i < numFeatures; i++) {
                stats[i] = new DescriptiveStatistics();
            }
            
            // Calculate statistics for each feature
            for (double[] sample : samples) {
                for (int i = 0; i < numFeatures; i++) {
                    stats[i].addValue(sample[i]);
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
}
