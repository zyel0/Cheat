package com.zyel0.cheat.listener;

import com.zyel0.cheat.CheatPlugin;
import com.zyel0.cheat.manager.ViolationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElytraAnticheatListener implements Listener {
    
    private final CheatPlugin plugin;
    private final ViolationManager violationManager;
    
    // Track player data
    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Long> flightStartTime = new HashMap<>();
    private final Map<UUID, Vector> lastVelocity = new HashMap<>();
    
    public ElytraAnticheatListener(CheatPlugin plugin) {
        this.plugin = plugin;
        this.violationManager = plugin.getViolationManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has bypass permission
        if (player.hasPermission("elytraac.bypass")) {
            return;
        }
        
        // Check if player is gliding with elytra
        if (!player.isGliding()) {
            // Clean up tracking data if not gliding
            lastLocation.remove(player.getUniqueId());
            flightStartTime.remove(player.getUniqueId());
            lastVelocity.remove(player.getUniqueId());
            return;
        }
        
        // Verify player is wearing elytra
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) {
            return;
        }
        
        // Initialize tracking for this player
        if (!flightStartTime.containsKey(player.getUniqueId())) {
            flightStartTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
        
        // Perform checks
        checkSpeed(player, from, to);
        checkVerticalMovement(player, from, to);
        checkFlightDuration(player);
        checkAcceleration(player, from, to);
        
        // Update tracking data
        lastLocation.put(player.getUniqueId(), to);
    }
    
    private void checkSpeed(Player player, Location from, Location to) {
        if (!plugin.getConfig().getBoolean("anticheat.speed-check.enabled", true)) {
            return;
        }
        
        double maxSpeed = plugin.getConfig().getDouble("anticheat.speed-check.max-speed", 3.5);
        double maxHorizontalSpeed = plugin.getConfig().getDouble("anticheat.speed-check.max-horizontal-speed", 2.5);
        
        double distance = from.distance(to);
        double horizontalDistance = Math.sqrt(
            Math.pow(to.getX() - from.getX(), 2) + 
            Math.pow(to.getZ() - from.getZ(), 2)
        );
        
        // Check overall speed
        if (distance > maxSpeed) {
            handleViolation(player, "overall-speed-check", distance);
        }
        
        // Check horizontal speed
        if (horizontalDistance > maxHorizontalSpeed) {
            handleViolation(player, "horizontal-speed-check", horizontalDistance);
        }
    }
    
    private void checkVerticalMovement(Player player, Location from, Location to) {
        if (!plugin.getConfig().getBoolean("anticheat.vertical-check.enabled", true)) {
            return;
        }
        
        double maxVerticalSpeed = plugin.getConfig().getDouble("anticheat.vertical-check.max-vertical-speed", 2.0);
        double verticalDistance = Math.abs(to.getY() - from.getY());
        
        // Check if vertical movement is too fast (ascending)
        if (to.getY() > from.getY() && verticalDistance > maxVerticalSpeed) {
            handleViolation(player, "vertical-check", verticalDistance);
        }
    }
    
    private void checkFlightDuration(Player player) {
        if (!plugin.getConfig().getBoolean("anticheat.duration-check.enabled", true)) {
            return;
        }
        
        Long startTime = flightStartTime.get(player.getUniqueId());
        if (startTime == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long flightDuration = (currentTime - startTime) / 1000; // Convert to seconds
        
        int maxDuration = plugin.getConfig().getInt("anticheat.duration-check.max-flight-duration", 300);
        int warnDuration = plugin.getConfig().getInt("anticheat.duration-check.warn-at-duration", 240);
        
        if (flightDuration > maxDuration) {
            handleViolation(player, "duration-check", flightDuration);
        } else if (flightDuration > warnDuration) {
            // Warn but don't count as violation
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format("Player %s has been flying for %d seconds", 
                    player.getName(), flightDuration));
            }
        }
    }
    
    private void checkAcceleration(Player player, Location from, Location to) {
        if (!plugin.getConfig().getBoolean("anticheat.acceleration-check.enabled", true)) {
            return;
        }
        
        Vector currentVelocity = to.toVector().subtract(from.toVector());
        Vector lastVel = lastVelocity.get(player.getUniqueId());
        
        if (lastVel != null) {
            double acceleration = currentVelocity.subtract(lastVel).length();
            double maxAcceleration = plugin.getConfig().getDouble("anticheat.acceleration-check.max-acceleration", 0.5);
            
            if (acceleration > maxAcceleration) {
                handleViolation(player, "acceleration-check", acceleration);
            }
        }
        
        lastVelocity.put(player.getUniqueId(), currentVelocity.clone());
    }
    
    private void handleViolation(Player player, String checkType, double value) {
        violationManager.addViolation(player, checkType, 1);
        
        int violations = violationManager.getViolations(player, checkType);
        int threshold = plugin.getConfig().getInt("anticheat." + checkType + ".violation-threshold", 5);
        
        if (violations >= threshold) {
            executeActions(player, checkType);
            violationManager.resetViolations(player, checkType);
        }
    }
    
    private void executeActions(Player player, String checkType) {
        String actionsPath = "anticheat." + checkType + ".actions";
        
        if (!plugin.getConfig().contains(actionsPath)) {
            return;
        }
        
        for (String action : plugin.getConfig().getStringList(actionsPath)) {
            String processedAction = action.replace("{player}", player.getName());
            
            if (processedAction.startsWith("kick ")) {
                String reason = processedAction.substring(5);
                Bukkit.getScheduler().runTask(plugin, () -> 
                    player.kickPlayer(reason.replace("&", "§")));
            } else if (processedAction.startsWith("warn ")) {
                String message = processedAction.substring(5);
                player.sendMessage(message.replace("&", "§"));
            } else if (processedAction.startsWith("command ")) {
                String command = processedAction.substring(8);
                Bukkit.getScheduler().runTask(plugin, () -> 
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocation.remove(uuid);
        flightStartTime.remove(uuid);
        lastVelocity.remove(uuid);
        violationManager.resetViolations(event.getPlayer());
    }
    
    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        if (!event.isGliding()) {
            // Player stopped gliding, clean up
            flightStartTime.remove(player.getUniqueId());
            lastVelocity.remove(player.getUniqueId());
        }
    }
}
