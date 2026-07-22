package fr.danakube.danahoppers.gui;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTierConfig;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utilitaires pour assembler les inventaires du plugin DanaHoppers à partir de leurs configurations YAML.
 */
public class InventoryBuilder {

    private final ConfigManager configManager;

    public InventoryBuilder(ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
    }

    /**
     * Construit l'inventaire du menu principal (MAIN_MENU) pour un CustomHopper.
     */
    public Inventory buildMainMenu(CustomHopper hopper) {
        FileConfiguration config = configManager.getMainMenuConfig();
        String title = config != null ? config.getString("title", "<blue>Gestion du Hopper</blue>") : "<blue>Gestion du Hopper</blue>";
        int size = config != null ? config.getInt("size", 27) : 27;

        Map<String, String> placeholders = buildPlaceholders(hopper);

        DanaHopperHolder holder = new DanaHopperHolder(hopper, DanaHopperHolder.MenuType.MAIN_MENU);
        Inventory inventory = Bukkit.createInventory(holder, size, ColorUtil.parse(title, placeholders));
        holder.setInventory(inventory);

        // 1. Remplissage des vitres d'arrière-plan
        fillBackground(inventory, config);

        // 2. Boutons d'action
        if (config != null && config.isConfigurationSection("items")) {
            ConfigurationSection itemsSec = config.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String key : itemsSec.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ConfigurationSection itemSec = itemsSec.getConfigurationSection(key);
                        if (itemSec != null && slot >= 0 && slot < size) {
                            ItemStack item = buildItem(itemSec, placeholders);
                            inventory.setItem(slot, item);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return inventory;
    }

    /**
     * Construit l'inventaire du menu du filtre (FILTER_MENU) pour un CustomHopper.
     */
    public Inventory buildFilterMenu(CustomHopper hopper) {
        FileConfiguration config = configManager.getFilterMenuConfig();
        String title = config != null ? config.getString("title", "<blue>Filtre du Hopper</blue>") : "<blue>Filtre du Hopper</blue>";
        int size = config != null ? config.getInt("size", 27) : 27;

        Map<String, String> placeholders = buildPlaceholders(hopper);

        DanaHopperHolder holder = new DanaHopperHolder(hopper, DanaHopperHolder.MenuType.FILTER_MENU);
        Inventory inventory = Bukkit.createInventory(holder, size, ColorUtil.parse(title, placeholders));
        holder.setInventory(inventory);

        // 1. Vitres de fond
        fillBackground(inventory, config);

        // 2. Bouton de Mode (Whitelist / Blacklist) en Slot 0 (ou configuré)
        int modeSlot = 0;
        if (config != null && config.isConfigurationSection("mode_button")) {
            ConfigurationSection modeSec = config.getConfigurationSection("mode_button");
            modeSlot = modeSec.getInt("slot", 0);
            FilterMode currentMode = hopper.getFilter() != null ? hopper.getFilter().getMode() : FilterMode.WHITELIST;
            String subKey = (currentMode == FilterMode.WHITELIST) ? "whitelist" : "blacklist";
            ConfigurationSection btnSec = modeSec.getConfigurationSection(subKey);
            if (btnSec != null) {
                inventory.setItem(modeSlot, buildItem(btnSec, placeholders));
            }
        }

        // 3. Bouton Vider (Clear) en Slot 8 (ou configuré)
        int clearSlot = 8;
        if (config != null && config.isConfigurationSection("clear_button")) {
            ConfigurationSection clearSec = config.getConfigurationSection("clear_button");
            clearSlot = clearSec.getInt("slot", 8);
            inventory.setItem(clearSlot, buildItem(clearSec, placeholders));
        }

        // 4. Slots de filtre (ex: [10, 11, 12, 13, 14, 15, 16])
        List<Integer> filterSlots = getFilterSlots(config);
        for (int slot : filterSlots) {
            if (slot >= 0 && slot < size) {
                inventory.setItem(slot, null); // Libérer les emplacements des vitres
            }
        }

        // Remplir les emplacements du filtre avec les matériaux configurés
        if (hopper.getFilter() != null) {
            List<Material> materials = hopper.getFilter().getMaterials();
            for (int i = 0; i < filterSlots.size() && i < materials.size(); i++) {
                Material mat = materials.get(i);
                int slot = filterSlots.get(i);
                if (mat != null && mat != Material.AIR && slot >= 0 && slot < size) {
                    inventory.setItem(slot, new ItemStack(mat));
                }
            }
        }

        return inventory;
    }

    /**
     * Récupère la liste des slots dédiés aux items de filtre dans la config.
     */
    public static List<Integer> getFilterSlots(FileConfiguration filterConfig) {
        if (filterConfig != null && filterConfig.contains("filter_slots")) {
            return filterConfig.getIntegerList("filter_slots");
        }
        return List.of(10, 11, 12, 13, 14, 15, 16);
    }

    private void fillBackground(Inventory inventory, FileConfiguration config) {
        if (config == null) return;
        String fillerMatName = config.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
        String fillerName = config.getString("filler.name", " ");

        Material mat = Material.matchMaterial(fillerMatName);
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.parse(fillerName));
            glass.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }
    }

    private ItemStack buildItem(ConfigurationSection sec, Map<String, String> placeholders) {
        String matStr = sec.getString("material", "STONE");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = sec.getString("name");
            if (name != null) {
                meta.displayName(ColorUtil.parse(name, placeholders));
            }
            List<String> lore = sec.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.lore(ColorUtil.parseList(lore, placeholders));
            }
            int cmd = sec.getInt("custom_model_data", 0);
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Map<String, String> buildPlaceholders(CustomHopper hopper) {
        Map<String, String> map = new HashMap<>();
        HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
        String typeName = typeConfig != null ? typeConfig.name() : hopper.getTypeId();
        HopperTierConfig tierConfig = typeConfig != null ? typeConfig.getTier(hopper.getTier()) : null;

        String radius = "1x1x1";
        if (tierConfig != null) {
            if ("CHUNK".equalsIgnoreCase(tierConfig.suctionType())) {
                radius = "16x384x16 (Chunk)";
            } else {
                int rx = tierConfig.radiusX() * 2 + 1;
                int ry = tierConfig.radiusY() * 2 + 1;
                int rz = tierConfig.radiusZ() * 2 + 1;
                radius = rx + "x" + ry + "x" + rz;
            }
        }

        String ownerName = "Inconnu";
        OfflinePlayer owner = Bukkit.getOfflinePlayer(hopper.getOwnerUuid());
        if (owner.getName() != null) {
            ownerName = owner.getName();
        }

        String currentTier = String.valueOf(hopper.getTier());
        String nextTier = "Max";
        String cost = "0";
        if (tierConfig != null && tierConfig.nextTier() > 0) {
            nextTier = String.valueOf(tierConfig.nextTier());
            cost = String.format("%.0f", tierConfig.upgradeCostMoney());
        }

        String maxDistance = tierConfig != null ? String.valueOf(tierConfig.maxLinkingDistance()) : "0";
        String interval = tierConfig != null ? String.valueOf(tierConfig.intervalSeconds()) : "5.0";

        map.put("type_name", typeName);
        map.put("type", typeName);
        map.put("tier", currentTier);
        map.put("current_tier", currentTier);
        map.put("next_tier", nextTier);
        map.put("radius", radius);
        map.put("interval", interval);
        map.put("suction_interval", interval);
        map.put("owner", ownerName);
        map.put("cost", cost);
        map.put("max_distance", maxDistance);
        map.put("items_transferred", String.valueOf(hopper.getItemsTransferred()));
        map.put("hologram_status", hopper.isHologramEnabled() ? "<green>Activé</green>" : "<red>Désactivé</red>");
        map.put("teleport_status", hopper.isTeleportEnabled() ? "Activé" : "Désactivé");

        return map;
    }
}
