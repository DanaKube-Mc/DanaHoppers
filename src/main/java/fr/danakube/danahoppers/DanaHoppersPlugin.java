package fr.danakube.danahoppers;

import org.bukkit.plugin.java.JavaPlugin;

public final class DanaHoppersPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("DanaHoppers v" + getDescription().getVersion() + " (Paper 1.21) initialisé avec succès !");
    }

    @Override
    public void onDisable() {
        getLogger().info("DanaHoppers désactivé.");
    }
}
