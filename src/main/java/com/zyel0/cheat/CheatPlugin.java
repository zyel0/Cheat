package com.zyel0.cheat;

import com.zyel0.cheat.listener.ElytraAnticheatListener;
import com.zyel0.cheat.listener.ElytraLoginListener;
import com.zyel0.cheat.listener.MovementListener;
import com.zyel0.cheat.manager.ViolationManager;
import com.zyel0.cheat.command.ElytraACCommand;
import com.zyel0.cheat.command.MLCommand;
import com.zyel0.cheat.ml.MLManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CheatPlugin extends JavaPlugin {
    
    private static CheatPlugin instance;
    private ViolationManager violationManager;
    private MLManager mlManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        violationManager = new ViolationManager(this);
        mlManager = new MLManager(this);
        
        // Register ML movement listener (always enabled for data collection)
        getServer().getPluginManager().registerEvents(new MovementListener(this, mlManager), this);
        getLogger().info("ML-based movement tracking enabled");
        
        // Register legacy listeners if enabled
        if (getConfig().getBoolean("login-check.enabled", false)) {
            getServer().getPluginManager().registerEvents(new ElytraLoginListener(this), this);
            getLogger().info("Elytra login check enabled");
        }
        
        if (getConfig().getBoolean("anticheat.enabled", false)) {
            getServer().getPluginManager().registerEvents(new ElytraAnticheatListener(this), this);
            getLogger().info("Legacy anticheat checks enabled");
        }
        
        // Register commands
        getCommand("elytraac").setExecutor(new ElytraACCommand(this));
        getCommand("ml").setExecutor(new MLCommand(this, mlManager));
        
        // Auto-save ML model at configured interval
        int autoSaveMinutes = getConfig().getInt("ml.auto-save-interval", 5);
        long autoSaveIntervalTicks = autoSaveMinutes * 60L * 20L; // Convert minutes to ticks (20 ticks/second)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            mlManager.saveDetector();
        }, autoSaveIntervalTicks, autoSaveIntervalTicks);
        
        getLogger().info("ML Anticheat plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save ML model before shutdown
        if (mlManager != null) {
            mlManager.saveDetector();
        }
        
        // Clean up
        if (violationManager != null) {
            violationManager.cleanup();
        }
        
        getLogger().info("ML Anticheat plugin has been disabled!");
    }
    
    public static CheatPlugin getInstance() {
        return instance;
    }
    
    public ViolationManager getViolationManager() {
        return violationManager;
    }
    
    public MLManager getMLManager() {
        return mlManager;
    }
}
