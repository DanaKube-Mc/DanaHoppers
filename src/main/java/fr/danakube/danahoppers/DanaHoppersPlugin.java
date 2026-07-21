package fr.danakube.danahoppers;

import fr.danakube.danahoppers.config.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DanaHoppersPlugin extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        getLogger().info("DanaHoppers v" + getDescription().getVersion() + " (Paper 1.21) initialisé avec succès !");
    }

    @Override
    public void onDisable() {
        getLogger().info("DanaHoppers désactivé.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
