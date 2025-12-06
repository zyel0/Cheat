package com.zyel0.cheat;

import com.zyel0.cheat.listener.ElytraAnticheatListener;
import com.zyel0.cheat.listener.ElytraLoginListener;
import com.zyel0.cheat.manager.ViolationManager;
import com.zyel0.cheat.command.ElytraACCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CheatPlugin extends JavaPlugin {
    
    private static CheatPlugin instance;
    private ViolationManager violationManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        violationManager = new ViolationManager(this);
        
        // Register listeners
        if (getConfig().getBoolean("login-check.enabled", true)) {
            getServer().getPluginManager().registerEvents(new ElytraLoginListener(this), this);
            getLogger().info("Elytra login check enabled");
        }
        
        if (getConfig().getBoolean("anticheat.enabled", true)) {
            getServer().getPluginManager().registerEvents(new ElytraAnticheatListener(this), this);
            getLogger().info("Elytra anticheat checks enabled");
        }
        
        // Register commands
        getCommand("elytraac").setExecutor(new ElytraACCommand(this));
        
        getLogger().info("Cheat plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Clean up
        if (violationManager != null) {
            violationManager.cleanup();
        }
        
        getLogger().info("Cheat plugin has been disabled!");
    }
    
    public static CheatPlugin getInstance() {
        return instance;
    }
    
    public ViolationManager getViolationManager() {
        return violationManager;
    }
}
