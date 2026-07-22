package fr.danakube.danahoppers.util;

import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.model.HopperFilter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Utilitaire pour la gestion des données du CustomHopper dans le PersistentDataContainer (PDC) des TileState.
 */
public final class PDCUtil {

    private PDCUtil() {
        // Class utilitaire non instanciable
    }

    private static NamespacedKey key(Plugin plugin, String name) {
        return new NamespacedKey(plugin, name);
    }

    /**
     * Vérifie si un TileState possède les métadonnées d'un CustomHopper.
     */
    public static boolean isCustomHopper(TileState tileState, Plugin plugin) {
        if (tileState == null || plugin == null) return false;
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        return pdc.has(key(plugin, "hopper_uuid"), PersistentDataType.STRING);
    }

    /**
     * Sauvegarde les métadonnées d'un CustomHopper dans le PDC du TileState.
     */
    public static void saveToPDC(TileState tileState, CustomHopper hopper, Plugin plugin) {
        if (tileState == null || hopper == null || plugin == null) return;

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();

        pdc.set(key(plugin, "hopper_uuid"), PersistentDataType.STRING, hopper.getHopperUuid().toString());
        pdc.set(key(plugin, "owner_uuid"), PersistentDataType.STRING, hopper.getOwnerUuid().toString());
        pdc.set(key(plugin, "type_id"), PersistentDataType.STRING, hopper.getTypeId());
        pdc.set(key(plugin, "tier"), PersistentDataType.INTEGER, hopper.getTier());
        pdc.set(key(plugin, "items_transferred"), PersistentDataType.LONG, hopper.getItemsTransferred());
        pdc.set(key(plugin, "hologram_enabled"), PersistentDataType.BYTE, (byte) (hopper.isHologramEnabled() ? 1 : 0));
        pdc.set(key(plugin, "teleport_enabled"), PersistentDataType.BYTE, (byte) (hopper.isTeleportEnabled() ? 1 : 0));

        if (hopper.getFilter() != null) {
            pdc.set(key(plugin, "filter_mode"), PersistentDataType.STRING, hopper.getFilter().getMode().name());
            pdc.set(key(plugin, "filter_materials"), PersistentDataType.STRING, hopper.getFilter().serializeMaterials());
        }

        if (hopper.getLinkedLocation() != null) {
            Location loc = hopper.getLinkedLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            pdc.set(key(plugin, "linked_location"), PersistentDataType.STRING, locStr);
        } else {
            pdc.remove(key(plugin, "linked_location"));
        }

        tileState.update();
    }

    /**
     * Charge un CustomHopper depuis le PDC d'un TileState.
     */
    public static Optional<CustomHopper> loadFromPDC(TileState tileState, Plugin plugin) {
        if (!isCustomHopper(tileState, plugin)) {
            return Optional.empty();
        }

        PersistentDataContainer pdc = tileState.getPersistentDataContainer();

        try {
            String uuidStr = pdc.get(key(plugin, "hopper_uuid"), PersistentDataType.STRING);
            String ownerStr = pdc.get(key(plugin, "owner_uuid"), PersistentDataType.STRING);
            String typeId = pdc.getOrDefault(key(plugin, "type_id"), PersistentDataType.STRING, "default");
            Integer tier = pdc.getOrDefault(key(plugin, "tier"), PersistentDataType.INTEGER, 1);
            Long itemsTransferred = pdc.getOrDefault(key(plugin, "items_transferred"), PersistentDataType.LONG, 0L);
            Byte holoByte = pdc.getOrDefault(key(plugin, "hologram_enabled"), PersistentDataType.BYTE, (byte) 1);
            Byte tpByte = pdc.getOrDefault(key(plugin, "teleport_enabled"), PersistentDataType.BYTE, (byte) 0);

            if (uuidStr == null || ownerStr == null) {
                return Optional.empty();
            }

            UUID hopperUuid = UUID.fromString(uuidStr);
            UUID ownerUuid = UUID.fromString(ownerStr);

            // Filtrage
            String filterModeStr = pdc.getOrDefault(key(plugin, "filter_mode"), PersistentDataType.STRING, "WHITELIST");
            String filterMatsStr = pdc.getOrDefault(key(plugin, "filter_materials"), PersistentDataType.STRING, "");
            FilterMode mode = FilterMode.WHITELIST;
            try {
                mode = FilterMode.valueOf(filterModeStr);
            } catch (IllegalArgumentException ignored) {}

            List<Material> materials = HopperFilter.deserializeMaterials(filterMatsStr);
            HopperFilter filter = new HopperFilter(mode, materials);

            // Linked location
            Location linkedLoc = null;
            String locStr = pdc.get(key(plugin, "linked_location"), PersistentDataType.STRING);
            if (locStr != null && !locStr.isBlank()) {
                String[] parts = locStr.split(",");
                if (parts.length == 4) {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world != null) {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        int z = Integer.parseInt(parts[3]);
                        linkedLoc = new Location(world, x, y, z);
                    }
                }
            }

            CustomHopper hopper = new CustomHopper(
                    hopperUuid,
                    ownerUuid,
                    tileState.getLocation(),
                    typeId,
                    tier,
                    linkedLoc,
                    filter,
                    itemsTransferred,
                    holoByte == 1,
                    tpByte == 1
            );

            return Optional.of(hopper);
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur lors de la désérialisation PDC du CustomHopper à " + tileState.getLocation() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Supprime les métadonnées d'un CustomHopper du PDC.
     */
    public static void removeFromPDC(TileState tileState, Plugin plugin) {
        if (tileState == null || plugin == null) return;
        PersistentDataContainer pdc = tileState.getPersistentDataContainer();
        pdc.remove(key(plugin, "hopper_uuid"));
        pdc.remove(key(plugin, "owner_uuid"));
        pdc.remove(key(plugin, "type_id"));
        pdc.remove(key(plugin, "tier"));
        pdc.remove(key(plugin, "items_transferred"));
        pdc.remove(key(plugin, "hologram_enabled"));
        pdc.remove(key(plugin, "teleport_enabled"));
        pdc.remove(key(plugin, "filter_mode"));
        pdc.remove(key(plugin, "filter_materials"));
        pdc.remove(key(plugin, "linked_location"));
        tileState.update();
    }
}
