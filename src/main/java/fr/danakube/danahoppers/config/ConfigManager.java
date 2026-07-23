package fr.danakube.danahoppers.config;

import fr.danakube.danahoppers.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gestionnaire central de configuration, de chargement des ressources YML et de localisation MiniMessage.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final File dataFolder;
    private final Logger logger;

    private FileConfiguration mainConfig;
    private FileConfiguration langConfig;
    private FileConfiguration mainMenuConfig;
    private FileConfiguration filterMenuConfig;

    private final Map<String, HopperTypeConfig> hopperTypes = new HashMap<>();

    /**
     * Constructeur pour l'environnement du plugin Paper.
     *
     * @param plugin l'instance du plugin JavaPlugin
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.logger = plugin.getLogger();
    }

    /**
     * Constructeur indépendant (pour les tests unitaires ou environnements isolés).
     *
     * @param dataFolder dossier racine des données de configuration
     */
    public ConfigManager(File dataFolder) {
        this.plugin = null;
        this.dataFolder = dataFolder;
        this.logger = Logger.getLogger("ConfigManager");
    }

    /**
     * Charge l'ensemble des fichiers de configuration, de langue, de GUI et les types de hoppers.
     */
    public void loadAll() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        saveDefaultResources();

        // 1. Charger main config
        File configFile = new File(dataFolder, "config.yml");
        this.mainConfig = YamlConfiguration.loadConfiguration(configFile);

        // 2. Charger langue
        String langCode = mainConfig.getString("language", "fr_FR");
        File langFile = new File(dataFolder, "lang/" + langCode + ".yml");
        if (!langFile.exists()) {
            // fallback vers fr_FR s'il n'existe pas
            langFile = new File(dataFolder, "lang/fr_FR.yml");
        }
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);

        // 3. Charger les GUIs
        File mainMenuFile = new File(dataFolder, "gui/main_menu.yml");
        this.mainMenuConfig = YamlConfiguration.loadConfiguration(mainMenuFile);

        File filterMenuFile = new File(dataFolder, "gui/filter_menu.yml");
        this.filterMenuConfig = YamlConfiguration.loadConfiguration(filterMenuFile);

        // 4. Charger les Hoppers
        loadHopperTypes();

        logger.info("[ConfigManager] Configurations, langues (" + langCode + ") et " + hopperTypes.size() + " types de hoppers chargés.");
    }

    /**
     * Extrait les ressources YML par défaut depuis le JAR vers le dossier de données si absentes.
     */
    private void saveDefaultResources() {
        saveResourceIfNotExists("config.yml");
        saveResourceIfNotExists("lang/fr_FR.yml");
        saveResourceIfNotExists("gui/main_menu.yml");
        saveResourceIfNotExists("gui/filter_menu.yml");
        saveResourceIfNotExists("hoppers/collecteur_elargi.yml");
        saveResourceIfNotExists("hoppers/collecteur_zone.yml");
        saveResourceIfNotExists("hoppers/sautelien.yml");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File targetFile = new File(dataFolder, resourcePath);
        if (targetFile.exists()) {
            return;
        }

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (plugin != null) {
            try {
                plugin.saveResource(resourcePath, false);
                return;
            } catch (Exception ignored) {
                // En cas d'échec fallback extraction manuelles
            }
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                logger.warning("[ConfigManager] Ressource introuvable dans le JAR: " + resourcePath);
            }
        } catch (IOException e) {
            logger.severe("[ConfigManager] Erreur lors de l'extraction de la ressource " + resourcePath + ": " + e.getMessage());
        }
    }

    /**
     * Scanne le dossier `hoppers/` et charge tous les fichiers YML définissant les types de hoppers.
     */
    private void loadHopperTypes() {
        hopperTypes.clear();
        File hoppersDir = new File(dataFolder, "hoppers");
        if (!hoppersDir.exists() || !hoppersDir.isDirectory()) {
            return;
        }

        File[] files = hoppersDir.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String defaultId = file.getName().replace(".yml", "").replace(".yaml", "");
            HopperTypeConfig hopperType = parseHopperType(yaml, defaultId);
            hopperTypes.put(hopperType.id(), hopperType);
        }
    }

    private HopperTypeConfig parseHopperType(FileConfiguration yaml, String defaultId) {
        String id = yaml.getString("id", defaultId);
        String name = yaml.getString("name", id);
        List<String> baseLore = yaml.getStringList("base_lore");

        Map<Integer, HopperTierConfig> tiersMap = new HashMap<>();
        ConfigurationSection tiersSection = yaml.getConfigurationSection("tiers");
        if (tiersSection != null) {
            for (String key : tiersSection.getKeys(false)) {
                try {
                    int tierNum = Integer.parseInt(key);
                    ConfigurationSection tierSec = tiersSection.getConfigurationSection(key);
                    if (tierSec != null) {
                        HopperTierConfig tierConfig = new HopperTierConfig(
                                tierNum,
                                tierSec.getString("display_name", name + " - Niv. " + tierNum),
                                tierSec.getString("material", "HOPPER"),
                                tierSec.getInt("custom_model_data", 0),
                                tierSec.getString("suction_type", "CUBE"),
                                tierSec.getInt("radius_x", 1),
                                tierSec.getInt("radius_y", 1),
                                tierSec.getInt("radius_z", 1),
                                tierSec.getDouble("interval_seconds", 5.0),
                                tierSec.getInt("max_linking_distance", 0),
                                tierSec.getDouble("upgrade_cost_money", 0.0),
                                tierSec.getString("next_hopper_id", ""),
                                tierSec.getInt("next_tier", 0)
                        );
                        tiersMap.put(tierNum, tierConfig);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return new HopperTypeConfig(id, name, baseLore, tiersMap);
    }

    // Getters

    public FileConfiguration getConfig() {
        return mainConfig;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }

    public FileConfiguration getMainMenuConfig() {
        return mainMenuConfig;
    }

    public FileConfiguration getFilterMenuConfig() {
        return filterMenuConfig;
    }

    public Map<String, HopperTypeConfig> getHopperTypes() {
        return Collections.unmodifiableMap(hopperTypes);
    }

    public HopperTypeConfig getHopperType(String id) {
        return hopperTypes.get(id);
    }

    public String getPrefix() {
        return langConfig != null ? langConfig.getString("prefix", "") : "";
    }

    public Component getMessage(String key) {
        return getMessage(key, Collections.emptyMap());
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        String raw = langConfig != null ? langConfig.getString("messages." + key) : null;
        if (raw == null) {
            return ColorUtil.parse(getPrefix() + "<red>Message introuvable: " + key + "</red>");
        }
        if (raw.trim().isEmpty()) {
            return Component.empty();
        }
        return ColorUtil.parseWithPrefix(getPrefix(), raw, placeholders);
    }

    public Component getRawMessage(String key) {
        return getRawMessage(key, Collections.emptyMap());
    }

    public Component getRawMessage(String key, Map<String, String> placeholders) {
        String raw = langConfig != null ? langConfig.getString("messages." + key) : null;
        if (raw == null) {
            return ColorUtil.parse("<red>Message introuvable: " + key + "</red>");
        }
        if (raw.trim().isEmpty()) {
            return Component.empty();
        }
        return ColorUtil.parse(raw, placeholders);
    }
}
