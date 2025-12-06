package com.zyel0.cheat.listener;

import com.zyel0.cheat.CheatPlugin;
import com.zyel0.cheat.data.MovementData;
import com.zyel0.cheat.ml.MLManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to ALL player movement and feeds data to ML system
 */
public class MovementListener implements Listener {
    
    private final CheatPlugin plugin;
    private final MLManager mlManager;
    
    // Track player data
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Double> lastSpeed = new HashMap<>();
    private final Map<UUID, Vector> lastVelocity = new HashMap<>();
    private final Map<UUID, Integer> ticksSinceMove = new HashMap<>();
    
    public MovementListener(CheatPlugin plugin, MLManager mlManager) {
        this.plugin = plugin;
        this.mlManager = mlManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check if player has bypass permission
        if (player.hasPermission("cheat.ml.bypass")) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null || from.getWorld() != to.getWorld()) {
            return;
        }
        
        // Skip if player hasn't actually moved
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            ticksSinceMove.put(uuid, ticksSinceMove.getOrDefault(uuid, 0) + 1);
            return;
        }
        
        // Get or initialize tracking data
        Location previousLocation = lastLocation.getOrDefault(uuid, from);
        double previousSpeed = lastSpeed.getOrDefault(uuid, 0.0);
        Vector velocity = lastVelocity.getOrDefault(uuid, new Vector(0, 0, 0));
        int ticks = ticksSinceMove.getOrDefault(uuid, 0);
        
        // Create movement data snapshot
        MovementData data = new MovementData(
            player.getName(),
            previousLocation,
            to,
            velocity,
            previousSpeed,
            player.isGliding(),
            player.isFlying(),
            player.isSneaking(),
            player.isSprinting(),
            player.isOnGround(),
            player.isInWater(),
            isInLava(player),
            ticks
        );
        
        // Process through ML system
        mlManager.processMovement(uuid, player.getName(), data);
        
        // Update tracking data
        lastLocation.put(uuid, to.clone());
        lastSpeed.put(uuid, data.getSpeed());
        lastVelocity.put(uuid, new Vector(
            data.getDeltaX(),
            data.getDeltaY(),
            data.getDeltaZ()
        ));
        ticksSinceMove.put(uuid, 0);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // Clean up tracking data
        lastLocation.remove(uuid);
        lastSpeed.remove(uuid);
        lastVelocity.remove(uuid);
        ticksSinceMove.remove(uuid);
    }
    
    /**
     * Check if player is in lava
     */
    private boolean isInLava(Player player) {
        try {
            // Try modern API first
            return player.isInLava();
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            Location loc = player.getLocation();
            return loc.getBlock().getType().toString().contains("LAVA");
        }
    }
}
