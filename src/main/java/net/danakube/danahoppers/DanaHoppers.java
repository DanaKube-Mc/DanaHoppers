package net.danakube.danahoppers;

import org.bukkit.plugin.java.JavaPlugin;

public final class DanaHoppers extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("DanaHoppers initialisé avec succès !");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("DanaHoppers désactivé.");
    }
}
