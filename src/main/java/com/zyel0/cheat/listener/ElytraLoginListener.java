package com.zyel0.cheat.listener;

import com.zyel0.cheat.CheatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class ElytraLoginListener implements Listener {
    
    private final CheatPlugin plugin;
    
    public ElytraLoginListener(CheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is wearing an elytra
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
            handleElytraLogin(player);
        }
    }
    
    private void handleElytraLogin(Player player) {
        // Check if notifications are enabled
        boolean notifyAdmins = plugin.getConfig().getBoolean("login-check.notify-admins", true);
        boolean logToConsole = plugin.getConfig().getBoolean("login-check.log-to-console", true);
        
        String message = plugin.getConfig().getString("login-check.message", 
            "&cPlayer {player} logged in with an elytra equipped!")
            .replace("{player}", player.getName())
            .replace("&", "§");
        
        // Log to console
        if (logToConsole) {
            plugin.getLogger().info(String.format("Player %s logged in with an elytra equipped!", player.getName()));
        }
        
        // Notify admins
        if (notifyAdmins) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.hasPermission("elytraac.admin") || online.hasPermission("elytraac.notify")) {
                    online.sendMessage(message);
                }
            }
        }
    }
}
