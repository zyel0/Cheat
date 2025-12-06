package com.zyel0.cheat.data;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Represents a snapshot of player movement data
 */
public class MovementData {
    
    private final String playerName;
    private final long timestamp;
    
    // Position data
    private final double x, y, z;
    private final float yaw, pitch;
    
    // Movement vectors
    private final double deltaX, deltaY, deltaZ;
    private final double speed;
    private final double horizontalSpeed;
    private final double verticalSpeed;
    
    // Acceleration
    private final double acceleration;
    private final double horizontalAcceleration;
    
    // Player state
    private final boolean isGliding;
    private final boolean isFlying;
    private final boolean isSneaking;
    private final boolean isSprinting;
    private final boolean isOnGround;
    private final boolean isInWater;
    private final boolean isInLava;
    
    // Additional context
    private final String worldName;
    private final int ticksSinceLastMove;
    
    public MovementData(String playerName, Location from, Location to, 
                        Vector velocity, double lastSpeed, boolean isGliding,
                        boolean isFlying, boolean isSneaking, boolean isSprinting,
                        boolean isOnGround, boolean isInWater, boolean isInLava,
                        int ticksSinceLastMove) {
        this.playerName = playerName;
        this.timestamp = System.currentTimeMillis();
        
        this.x = to.getX();
        this.y = to.getY();
        this.z = to.getZ();
        this.yaw = to.getYaw();
        this.pitch = to.getPitch();
        
        this.deltaX = to.getX() - from.getX();
        this.deltaY = to.getY() - from.getY();
        this.deltaZ = to.getZ() - from.getZ();
        
        this.speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        this.horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        this.verticalSpeed = Math.abs(deltaY);
        
        this.acceleration = speed - lastSpeed;
        this.horizontalAcceleration = horizontalSpeed - Math.sqrt(
            Math.pow(velocity.getX(), 2) + Math.pow(velocity.getZ(), 2)
        );
        
        this.isGliding = isGliding;
        this.isFlying = isFlying;
        this.isSneaking = isSneaking;
        this.isSprinting = isSprinting;
        this.isOnGround = isOnGround;
        this.isInWater = isInWater;
        this.isInLava = isInLava;
        
        this.worldName = to.getWorld().getName();
        this.ticksSinceLastMove = ticksSinceLastMove;
    }
    
    // Getters
    public String getPlayerName() { return playerName; }
    public long getTimestamp() { return timestamp; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getDeltaZ() { return deltaZ; }
    public double getSpeed() { return speed; }
    public double getHorizontalSpeed() { return horizontalSpeed; }
    public double getVerticalSpeed() { return verticalSpeed; }
    public double getAcceleration() { return acceleration; }
    public double getHorizontalAcceleration() { return horizontalAcceleration; }
    public boolean isGliding() { return isGliding; }
    public boolean isFlying() { return isFlying; }
    public boolean isSneaking() { return isSneaking; }
    public boolean isSprinting() { return isSprinting; }
    public boolean isOnGround() { return isOnGround; }
    public boolean isInWater() { return isInWater; }
    public boolean isInLava() { return isInLava; }
    public String getWorldName() { return worldName; }
    public int getTicksSinceLastMove() { return ticksSinceLastMove; }
    
    /**
     * Extract features for ML model
     */
    public double[] extractFeatures() {
        return new double[] {
            speed,
            horizontalSpeed,
            verticalSpeed,
            acceleration,
            horizontalAcceleration,
            Math.abs(deltaY),
            yaw,
            pitch,
            isGliding ? 1.0 : 0.0,
            isFlying ? 1.0 : 0.0,
            isSneaking ? 1.0 : 0.0,
            isSprinting ? 1.0 : 0.0,
            isOnGround ? 1.0 : 0.0,
            isInWater ? 1.0 : 0.0,
            isInLava ? 1.0 : 0.0,
            ticksSinceLastMove
        };
    }
    
    @Override
    public String toString() {
        return String.format("MovementData[player=%s, pos=(%.2f,%.2f,%.2f), speed=%.3f, accel=%.3f, gliding=%s]",
            playerName, x, y, z, speed, acceleration, isGliding);
    }
}
