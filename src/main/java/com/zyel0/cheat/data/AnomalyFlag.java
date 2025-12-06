package com.zyel0.cheat.data;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents an anomaly flag for suspicious movement
 */
public class AnomalyFlag {
    
    private final String playerName;
    private final long timestamp;
    private final MovementData movementData;
    private final double anomalyScore;
    private final String details;
    
    public AnomalyFlag(String playerName, MovementData movementData, double anomalyScore, String details) {
        this.playerName = playerName;
        this.timestamp = System.currentTimeMillis();
        this.movementData = movementData;
        this.anomalyScore = anomalyScore;
        this.details = details;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public MovementData getMovementData() {
        return movementData;
    }
    
    public double getAnomalyScore() {
        return anomalyScore;
    }
    
    public String getDetails() {
        return details;
    }
    
    /**
     * Generate detailed flag message (max 100 words)
     */
    public String generateFlagMessage() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String time = sdf.format(new Date(timestamp));
        
        StringBuilder sb = new StringBuilder();
        sb.append(playerName).append(" showed abnormal movement at ").append(time).append(". ");
        sb.append("Position: X=").append(String.format("%.2f", movementData.getX()))
          .append(" Y=").append(String.format("%.2f", movementData.getY()))
          .append(" Z=").append(String.format("%.2f", movementData.getZ())).append(". ");
        sb.append("Direction: ΔX=").append(String.format("%.3f", movementData.getDeltaX()))
          .append(" ΔY=").append(String.format("%.3f", movementData.getDeltaY()))
          .append(" ΔZ=").append(String.format("%.3f", movementData.getDeltaZ())).append(". ");
        sb.append("Speed: ").append(String.format("%.3f", movementData.getSpeed()))
          .append(" (H:").append(String.format("%.3f", movementData.getHorizontalSpeed()))
          .append(" V:").append(String.format("%.3f", movementData.getVerticalSpeed())).append("). ");
        sb.append("Accel: ").append(String.format("%.3f", movementData.getAcceleration())).append(". ");
        sb.append("State: ");
        if (movementData.isGliding()) sb.append("Gliding ");
        if (movementData.isFlying()) sb.append("Flying ");
        if (movementData.isSprinting()) sb.append("Sprint ");
        if (movementData.isSneaking()) sb.append("Sneak ");
        if (movementData.isOnGround()) sb.append("Ground ");
        if (movementData.isInWater()) sb.append("Water ");
        sb.append(". Anomaly: ").append(String.format("%.2f", anomalyScore)).append(". ");
        sb.append(details);
        
        // Truncate to approximately 100 words
        String result = sb.toString();
        int wordCount = 0;
        int lastSpaceIndex = 0;
        
        for (int i = 0; i < result.length(); i++) {
            if (Character.isWhitespace(result.charAt(i))) {
                wordCount++;
                lastSpaceIndex = i;
                if (wordCount >= 100) {
                    return result.substring(0, lastSpaceIndex) + "...";
                }
            }
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("AnomalyFlag[player=%s, score=%.2f, time=%d]", 
            playerName, anomalyScore, timestamp);
    }
}
