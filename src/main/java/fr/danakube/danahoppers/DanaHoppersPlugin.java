package fr.danakube.danahoppers;

import fr.danakube.danahoppers.command.DanaHopperCommand;
import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.gui.HopperFilterMenu;
import fr.danakube.danahoppers.gui.HopperMainMenu;
import fr.danakube.danahoppers.gui.InventoryBuilder;
import fr.danakube.danahoppers.hook.AdvancedChestHook;
import fr.danakube.danahoppers.listener.HopperBlockListener;
import fr.danakube.danahoppers.listener.InventoryListener;
import fr.danakube.danahoppers.listener.PlayerInteractListener;
import fr.danakube.danahoppers.listener.PlayerSneakListener;
import fr.danakube.danahoppers.manager.HologramManager;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.manager.SuctionManager;
import fr.danakube.danahoppers.storage.DatabaseManager;
import fr.danakube.danahoppers.storage.HopperRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class DanaHoppersPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private HopperRepository hopperRepository;
    private HopperManager hopperManager;
    private HologramManager hologramManager;
    private SuctionManager suctionManager;
    private Economy economy;
    private BukkitTask autosaveTask;

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    @Override
    public void onEnable() {
        // 1. Configuration
        this.configManager = new ConfigManager(this);
        this.configManager.loadAll();

        // Initialisation de Vault
        if (configManager.getConfig().getBoolean("integrations.vault", true)) {
            if (!setupEconomy()) {
                getLogger().warning("Intégration Vault activée mais aucun plugin d'économie compatible trouvé ! Le système d'économie sera désactivé.");
            } else {
                getLogger().info("Liaison réussie avec Vault pour le système d'économie !");
            }
        }

        // Détection AdvancedChests
        if (AdvancedChestHook.isAdvancedChestsActive()) {
            getLogger().info("[Hook] Plugin AdvancedChests détecté et activé !");
        } else {
            getLogger().info("[Hook] AdvancedChests non détecté sur ce serveur.");
        }

        // 2. Base de données & Stockage
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initAsync().join();
        this.hopperRepository = new HopperRepository(databaseManager);
        this.hopperManager = new HopperManager(hopperRepository);

        // Chargement asynchrone des hoppers enregistrés
        this.hopperManager.loadAllAsync().exceptionally(ex -> {
            getLogger().severe("Erreur lors du chargement des hoppers: " + ex.getMessage());
            return null;
        });

        // 3. Managers
        this.hologramManager = new HologramManager(this, configManager);
        this.suctionManager = new SuctionManager(this, hopperManager, configManager, hologramManager);
        this.suctionManager.start();

        // 4. GUI & Sub-systèmes
        InventoryBuilder inventoryBuilder = new InventoryBuilder(configManager);
        HopperMainMenu mainMenu = new HopperMainMenu(inventoryBuilder, hopperManager, configManager, hologramManager, this);
        HopperFilterMenu filterMenu = new HopperFilterMenu(inventoryBuilder, hopperManager, configManager, this);

        // 5. Événements (Listeners)
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new HopperBlockListener(this, hopperManager, configManager, hologramManager), this);
        pm.registerEvents(new PlayerInteractListener(this, hopperManager, configManager, mainMenu), this);
        pm.registerEvents(new InventoryListener(mainMenu, filterMenu), this);
        pm.registerEvents(new PlayerSneakListener(hopperManager, configManager), this);

        // 6. Commandes
        DanaHopperCommand mainCmd = new DanaHopperCommand(this, configManager, hopperManager);
        PluginCommand cmd = getCommand("danahopper");
        if (cmd != null) {
            cmd.setExecutor(mainCmd);
            cmd.setTabCompleter(mainCmd);
        }

        // 7. Sauvegarde automatique
        int autosaveMinutes = configManager.getConfig().getInt("options.database-autosave-minutes", 5);
        if (autosaveMinutes > 0) {
            long ticks = autosaveMinutes * 60L * 20L;
            this.autosaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                getLogger().info("[Autosave] Sauvegarde périodique des hoppers en cours...");
                hopperManager.saveAllAsync().thenAccept(v -> getLogger().info("[Autosave] Sauvegarde terminée avec succès."));
            }, ticks, ticks);
        }

        getLogger().info("DanaHoppers v" + getPluginMeta().getVersion() + " (Paper 1.21) initialisé avec succès !");
    }

    @Override
    public void onDisable() {
        if (autosaveTask != null && !autosaveTask.isCancelled()) {
            autosaveTask.cancel();
        }

        if (suctionManager != null) {
            suctionManager.stop();
        }

        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }

        if (hopperManager != null) {
            try {
                hopperManager.saveAllAsync().join();
            } catch (Exception e) {
                getLogger().severe("Erreur lors de la sauvegarde finale des hoppers: " + e.getMessage());
            }
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("DanaHoppers désactivé avec succès.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HopperManager getHopperManager() {
        return hopperManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public SuctionManager getSuctionManager() {
        return suctionManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}
