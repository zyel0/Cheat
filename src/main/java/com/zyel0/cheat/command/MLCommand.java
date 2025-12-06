package com.zyel0.cheat.command;

import com.zyel0.cheat.CheatPlugin;
import com.zyel0.cheat.data.AnomalyFlag;
import com.zyel0.cheat.ml.MLManager;
import com.zyel0.cheat.ml.MovementAnomalyDetector;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Command handler for ML system
 */
public class MLCommand implements CommandExecutor {
    
    private final CheatPlugin plugin;
    private final MLManager mlManager;
    private final Set<UUID> flagsEnabled = new HashSet<>();
    
    public MLCommand(CheatPlugin plugin, MLManager mlManager) {
        this.plugin = plugin;
        this.mlManager = mlManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cheat.ml.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "flags":
                handleFlags(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "clear":
                if (args.length >= 2) {
                    handleClear(sender, args[1]);
                } else {
                    sender.sendMessage("§cUsage: /ml clear <player|all>");
                }
                break;
            case "save":
                handleSave(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7§m          §r §6ML Anticheat §7§m          ");
        sender.sendMessage("§e/ml flags §7- Toggle flag notifications");
        sender.sendMessage("§e/ml status §7- View ML system status");
        sender.sendMessage("§e/ml clear <player|all> §7- Clear flags");
        sender.sendMessage("§e/ml save §7- Save ML model to disk");
        sender.sendMessage("§7§m                                    ");
    }
    
    private void handleFlags(CommandSender sender) {
        if (!(sender instanceof Player)) {
            // For console, show all flags
            showAllFlags(sender);
            return;
        }
        
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        
        if (flagsEnabled.contains(uuid)) {
            flagsEnabled.remove(uuid);
            player.sendMessage("§cFlag notifications disabled.");
        } else {
            flagsEnabled.add(uuid);
            player.sendMessage("§aFlag notifications enabled.");
            
            // Show current flags
            showAllFlags(player);
        }
    }
    
    private void showAllFlags(CommandSender sender) {
        Map<UUID, List<AnomalyFlag>> allFlags = mlManager.getAllFlags();
        
        if (allFlags.isEmpty()) {
            sender.sendMessage("§aNo anomaly flags recorded.");
            return;
        }
        
        sender.sendMessage("§7§m          §r §cAnomaly Flags §7§m          ");
        
        int totalFlags = 0;
        for (Map.Entry<UUID, List<AnomalyFlag>> entry : allFlags.entrySet()) {
            List<AnomalyFlag> flags = entry.getValue();
            if (flags.isEmpty()) continue;
            
            Player player = Bukkit.getPlayer(entry.getKey());
            String playerName = player != null ? player.getName() : "Unknown";
            
            sender.sendMessage("§e" + playerName + " §7(" + flags.size() + " flags):");
            
            // Show recent flags (max 3)
            int shown = 0;
            for (int i = flags.size() - 1; i >= 0 && shown < 3; i--) {
                AnomalyFlag flag = flags.get(i);
                sendClickableFlag(sender, flag);
                shown++;
            }
            
            totalFlags += flags.size();
        }
        
        sender.sendMessage("§7§m                                    ");
        sender.sendMessage("§7Total flags: §e" + totalFlags);
    }
    
    private void sendClickableFlag(CommandSender sender, AnomalyFlag flag) {
        String message = flag.generateFlagMessage();
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            
            TextComponent component = new TextComponent("§7[§cFLAG§7] §f");
            
            TextComponent clickable = new TextComponent(shortenMessage(message, 80));
            clickable.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, message));
            clickable.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§aClick to copy full details").create()
            ));
            
            component.addExtra(clickable);
            player.spigot().sendMessage(component);
        } else {
            sender.sendMessage("§7[§cFLAG§7] §f" + shortenMessage(message, 120));
        }
    }
    
    private String shortenMessage(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength - 3) + "...";
    }
    
    private void handleStatus(CommandSender sender) {
        MovementAnomalyDetector detector = mlManager.getDetector();
        
        sender.sendMessage("§7§m          §r §6ML System Status §7§m          ");
        sender.sendMessage("§eTraining Status: §7" + 
            (detector.isTrained() ? "§aCompleted" : "§eIn Progress"));
        
        if (!detector.isTrained()) {
            long remaining = detector.getTrainingTimeRemaining();
            long days = TimeUnit.MILLISECONDS.toDays(remaining);
            long hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
            
            sender.sendMessage("§eTime Remaining: §7" + days + "d " + hours + "h");
            sender.sendMessage("§eSamples Collected: §7" + detector.getTrainingSampleCount());
            sender.sendMessage("§7The system is learning normal movement patterns.");
        } else {
            sender.sendMessage("§7The system is actively detecting anomalies.");
        }
        
        Map<UUID, List<AnomalyFlag>> allFlags = mlManager.getAllFlags();
        int totalFlags = allFlags.values().stream().mapToInt(List::size).sum();
        sender.sendMessage("§eTotal Flags: §7" + totalFlags);
        sender.sendMessage("§eActive Watchers: §7" + flagsEnabled.size());
        
        sender.sendMessage("§7§m                                              ");
    }
    
    private void handleClear(CommandSender sender, String target) {
        if (target.equalsIgnoreCase("all")) {
            mlManager.clearAllFlags();
            sender.sendMessage("§aCleared all flags.");
        } else {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found!");
                return;
            }
            
            mlManager.clearPlayerFlags(targetPlayer.getUniqueId());
            sender.sendMessage("§aCleared flags for " + targetPlayer.getName());
        }
    }
    
    private void handleSave(CommandSender sender) {
        mlManager.saveDetector();
        sender.sendMessage("§aML model saved successfully.");
    }
    
    public boolean isFlagsEnabled(UUID playerId) {
        return flagsEnabled.contains(playerId);
    }
}
