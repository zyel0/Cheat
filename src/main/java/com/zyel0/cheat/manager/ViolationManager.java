package com.zyel0.cheat.manager;

import com.zyel0.cheat.CheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationManager {
    
    private final CheatPlugin plugin;
    private final Map<UUID, Map<String, Integer>> violations;
    private final Map<UUID, Long> lastViolationTime;
    
    public ViolationManager(CheatPlugin plugin) {
        this.plugin = plugin;
        this.violations = new ConcurrentHashMap<>();
        this.lastViolationTime = new ConcurrentHashMap<>();
    }
    
    public void addViolation(Player player, String checkType, int amount) {
        UUID uuid = player.getUniqueId();
        violations.putIfAbsent(uuid, new HashMap<>());
        
        Map<String, Integer> playerViolations = violations.get(uuid);
        int currentViolations = playerViolations.getOrDefault(checkType, 0);
        playerViolations.put(checkType, currentViolations + amount);
        
        lastViolationTime.put(uuid, System.currentTimeMillis());
        
        // Notify admins if enabled
        if (plugin.getConfig().getBoolean("notifications.notify-on-violation", true)) {
            notifyAdmins(player, checkType, currentViolations + amount);
        }
        
        // Log to console if debug is enabled
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(String.format("Player %s violated %s check (VL: %d)", 
                player.getName(), checkType, currentViolations + amount));
        }
    }
    
    public int getViolations(Player player, String checkType) {
        UUID uuid = player.getUniqueId();
        if (!violations.containsKey(uuid)) {
            return 0;
        }
        return violations.get(uuid).getOrDefault(checkType, 0);
    }
    
    public void resetViolations(Player player) {
        violations.remove(player.getUniqueId());
        lastViolationTime.remove(player.getUniqueId());
    }
    
    public void resetViolations(Player player, String checkType) {
        UUID uuid = player.getUniqueId();
        if (violations.containsKey(uuid)) {
            violations.get(uuid).remove(checkType);
        }
    }
    
    private void notifyAdmins(Player player, String checkType, int violationLevel) {
        String permission = plugin.getConfig().getString("notifications.notify-permission", "elytraac.notify");
        String format = plugin.getConfig().getString("notifications.format", 
            "&7[&cElytraAC&7] &e{player} &7failed check: &c{check} &7(VL: {violations})");
        
        String message = format
            .replace("{player}", player.getName())
            .replace("{check}", checkType)
            .replace("{violations}", String.valueOf(violationLevel))
            .replace("&", "§");
        
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                online.sendMessage(message);
            }
        }
    }
    
    public void cleanup() {
        violations.clear();
        lastViolationTime.clear();
    }
    
    public Map<UUID, Map<String, Integer>> getAllViolations() {
        return new HashMap<>(violations);
    }
}
