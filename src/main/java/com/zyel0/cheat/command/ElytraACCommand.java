package com.zyel0.cheat.command;

import com.zyel0.cheat.CheatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ElytraACCommand implements CommandExecutor {
    
    private final CheatPlugin plugin;
    
    public ElytraACCommand(CheatPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("elytraac.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "violations":
            case "vl":
                handleViolations(sender);
                break;
            case "reset":
                if (args.length >= 2) {
                    handleReset(sender, args[1]);
                } else {
                    sender.sendMessage("§cUsage: /elytraac reset <player>");
                }
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7§m          §r §cElytra Anticheat §7§m          ");
        sender.sendMessage("§e/elytraac reload §7- Reload the configuration");
        sender.sendMessage("§e/elytraac status §7- Check plugin status");
        sender.sendMessage("§e/elytraac violations §7- View all violations");
        sender.sendMessage("§e/elytraac reset <player> §7- Reset player violations");
        sender.sendMessage("§7§m                                    ");
    }
    
    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage("§aConfiguration reloaded successfully!");
    }
    
    private void handleStatus(CommandSender sender) {
        sender.sendMessage("§7§m          §r §cElytra Anticheat Status §7§m          ");
        sender.sendMessage("§ePlugin Version: §7" + plugin.getDescription().getVersion());
        sender.sendMessage("§eLogin Check: §7" + 
            (plugin.getConfig().getBoolean("login-check.enabled") ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("§eAnticheat: §7" + 
            (plugin.getConfig().getBoolean("anticheat.enabled") ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("§7§m                                              ");
        
        if (plugin.getConfig().getBoolean("anticheat.enabled")) {
            sender.sendMessage("§eActive Checks:");
            sender.sendMessage("  §7- Speed Check: " + 
                (plugin.getConfig().getBoolean("anticheat.speed-check.enabled") ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("  §7- Vertical Check: " + 
                (plugin.getConfig().getBoolean("anticheat.vertical-check.enabled") ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("  §7- Duration Check: " + 
                (plugin.getConfig().getBoolean("anticheat.duration-check.enabled") ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage("  §7- Acceleration Check: " + 
                (plugin.getConfig().getBoolean("anticheat.acceleration-check.enabled") ? "§aEnabled" : "§cDisabled"));
        }
    }
    
    private void handleViolations(CommandSender sender) {
        Map<UUID, Map<String, Integer>> allViolations = plugin.getViolationManager().getAllViolations();
        
        if (allViolations.isEmpty()) {
            sender.sendMessage("§aNo active violations.");
            return;
        }
        
        sender.sendMessage("§7§m          §r §cActive Violations §7§m          ");
        
        for (Map.Entry<UUID, Map<String, Integer>> entry : allViolations.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            String playerName = player != null ? player.getName() : entry.getKey().toString();
            
            sender.sendMessage("§e" + playerName + ":");
            for (Map.Entry<String, Integer> violation : entry.getValue().entrySet()) {
                sender.sendMessage("  §7- " + violation.getKey() + ": §c" + violation.getValue());
            }
        }
        
        sender.sendMessage("§7§m                                    ");
    }
    
    private void handleReset(CommandSender sender, String playerName) {
        Player target = plugin.getServer().getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }
        
        plugin.getViolationManager().resetViolations(target);
        sender.sendMessage("§aReset violations for " + target.getName());
    }
}
