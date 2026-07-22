package fr.danakube.danahoppers.util;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTierConfig;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utilitaires pour la génération et la détection des objets CustomHopper sous forme d'ItemStack.
 */
public final class HopperItemUtil {

    private HopperItemUtil() {
        // Classe utilitaire non instanciable
    }

    private static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    /**
     * Crée un ItemStack représentant un CustomHopper.
     */
    public static ItemStack createHopperItem(String typeId, int tier, ConfigManager configManager, Plugin plugin) {
        return createHopperItem(typeId, tier, null, configManager, plugin);
    }

    /**
     * Crée un ItemStack à partir d'un CustomHopper existant.
     */
    public static ItemStack createHopperItem(String typeId, int tier, CustomHopper hopper, ConfigManager configManager, Plugin plugin) {
        Objects.requireNonNull(typeId, "typeId cannot be null");
        Objects.requireNonNull(configManager, "configManager cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        HopperTypeConfig typeConfig = configManager.getHopperType(typeId);
        HopperTierConfig tierConfig = typeConfig != null ? typeConfig.getTier(tier) : null;

        String matName = tierConfig != null ? tierConfig.material() : "HOPPER";
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.HOPPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("type_name", typeConfig != null ? typeConfig.name() : typeId);
            placeholders.put("type", typeConfig != null ? typeConfig.name() : typeId);
            placeholders.put("tier", String.valueOf(tier));

            String displayName = tierConfig != null ? tierConfig.displayName() : "<yellow>Custom Hopper (Niv. " + tier + ")</yellow>";
            meta.displayName(ColorUtil.parse(displayName, placeholders));

            if (typeConfig != null && !typeConfig.baseLore().isEmpty()) {
                meta.lore(ColorUtil.parseList(typeConfig.baseLore(), placeholders));
            }

            if (tierConfig != null && tierConfig.customModelData() > 0) {
                meta.setCustomModelData(tierConfig.customModelData());
            }

            // Écriture du PDC dans l'ItemMeta
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(key(plugin, "is_custom_hopper"), PersistentDataType.BYTE, (byte) 1);
            pdc.set(key(plugin, "type_id"), PersistentDataType.STRING, typeId);
            pdc.set(key(plugin, "tier"), PersistentDataType.INTEGER, Math.max(1, tier));

            if (hopper != null && hopper.getFilter() != null) {
                pdc.set(key(plugin, "filter_mode"), PersistentDataType.STRING, hopper.getFilter().getMode().name());
                pdc.set(key(plugin, "filter_materials"), PersistentDataType.STRING, hopper.getFilter().serializeMaterials());
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Vérifie si un ItemStack contient les métadonnées d'un CustomHopper.
     */
    public static boolean isCustomHopperItem(ItemStack item, Plugin plugin) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(key(plugin, "is_custom_hopper"), PersistentDataType.BYTE) ||
               pdc.has(key(plugin, "type_id"), PersistentDataType.STRING);
    }

    /**
     * Extrait le typeId depuis l'ItemStack.
     */
    public static Optional<String> getTypeId(ItemStack item, Plugin plugin) {
        if (!isCustomHopperItem(item, plugin)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return Optional.ofNullable(pdc.get(key(plugin, "type_id"), PersistentDataType.STRING));
    }

    /**
     * Extrait le tier depuis l'ItemStack.
     */
    public static int getTier(ItemStack item, Plugin plugin) {
        if (!isCustomHopperItem(item, plugin)) return 1;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer tier = pdc.get(key(plugin, "tier"), PersistentDataType.INTEGER);
        return tier != null ? tier : 1;
    }
}
