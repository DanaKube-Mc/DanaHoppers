package fr.danakube.danahoppers.manager;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des hologrammes d'entonnoirs via l'API Paper 1.21 TextDisplay.
 */
public class HologramManager {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, TextDisplay> hologramCache;
    private final NamespacedKey hologramKey;

    public HologramManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.hologramCache = new ConcurrentHashMap<>();
        this.hologramKey = new NamespacedKey(plugin, "hopper_hologram_uuid");
    }

    /**
     * Calcule l'emplacement de l'hologramme au-dessus du bloc d'entonnoir.
     */
    private Location getHologramLocation(Location blockLoc) {
        return blockLoc.clone().add(0.5, 1.25, 0.5);
    }

    /**
     * Crée ou remplace l'hologramme pour le CustomHopper spécifié.
     */
    public TextDisplay createHologram(CustomHopper hopper) {
        if (hopper == null || hopper.getLocation() == null || !hopper.isHologramEnabled()) {
            return null;
        }

        Location loc = hopper.getLocation();
        World world = loc.getWorld();
        if (world == null || !loc.isWorldLoaded() || !loc.getChunk().isLoaded()) {
            return null;
        }

        // Supprimer un hologramme existant si déjà en cache
        removeHologram(hopper);

        Location holoLoc = getHologramLocation(loc);
        TextDisplay display = world.spawn(holoLoc, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setShadowed(true);
            entity.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
            entity.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, hopper.getHopperUuid().toString());
            entity.text(buildHologramText(hopper));
        });

        hologramCache.put(hopper.getHopperUuid(), display);
        return display;
    }

    /**
     * Met à jour le texte de l'hologramme existant ou le crée s'il n'existe pas.
     */
    public void updateHologram(CustomHopper hopper) {
        if (hopper == null) return;

        if (!hopper.isHologramEnabled()) {
            removeHologram(hopper);
            return;
        }

        TextDisplay display = hologramCache.get(hopper.getHopperUuid());
        if (display != null && display.isValid() && !display.isDead()) {
            display.text(buildHologramText(hopper));
        } else {
            createHologram(hopper);
        }
    }

    /**
     * Supprime l'hologramme d'un CustomHopper.
     */
    public void removeHologram(CustomHopper hopper) {
        if (hopper == null) return;
        removeHologramByUuid(hopper.getHopperUuid(), hopper.getLocation());
    }

    public void removeHologramByUuid(UUID hopperUuid, Location location) {
        if (hopperUuid == null) return;

        TextDisplay display = hologramCache.remove(hopperUuid);
        if (display != null && display.isValid()) {
            display.remove();
        }

        // Si l'hologramme n'était pas dans le cache, recherche défensive dans le monde
        if (location != null && location.isWorldLoaded() && location.getChunk().isLoaded()) {
            World world = location.getWorld();
            if (world != null) {
                Location holoLoc = getHologramLocation(location);
                for (Entity entity : world.getNearbyEntities(holoLoc, 1.0, 1.0, 1.0)) {
                    if (entity instanceof TextDisplay td) {
                        String storedUuid = td.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING);
                        if (hopperUuid.toString().equals(storedUuid)) {
                            td.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * Nettoie tous les hologrammes créés lors du reload ou du onDisable().
     */
    public void removeAllHolograms() {
        for (TextDisplay display : hologramCache.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        hologramCache.clear();
    }

    /**
     * Génère la représentation textuelle MiniMessage de l'hologramme.
     */
    private Component buildHologramText(CustomHopper hopper) {
        HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
        String typeName = typeConfig != null ? typeConfig.name() : hopper.getTypeId();

        String rawText = "<gradient:#4facfe:#00f2fe><bold>" + typeName + "</bold></gradient> <gray>(Niv. " + hopper.getTier() + ")</gray>\n" +
                "<gray>Items aspirés : <green>" + hopper.getItemsTransferred() + "</green></gray>";

        return ColorUtil.parse(rawText);
    }
}
