package fr.danakube.danahoppers.manager;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.model.CustomHopper;
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

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des hologrammes d'entonnoirs via l'API Paper 1.21 TextDisplay.
 */
public class HologramManager {

    private final ConfigManager configManager;
    private final ConcurrentHashMap<UUID, TextDisplay> hologramCache;
    private final NamespacedKey hologramKey;

    public HologramManager(Plugin plugin, ConfigManager configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.hologramCache = new ConcurrentHashMap<>();
        this.hologramKey = new NamespacedKey(plugin, "hopper_hologram_uuid");
    }

    /**
     * Calcule l'emplacement de l'hologramme au-dessus du bloc d'entonnoir.
     */
    private Location getHologramLocation(Location blockLoc) {
        double gap = configManager.getConfig().getDouble("hologram.line-gap", 1.25);
        return blockLoc.clone().add(0.5, gap, 0.5);
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
            // 1. Billboard
            String billboardStr = configManager.getConfig().getString("hologram.billboard", "VERTICAL").toUpperCase();
            Display.Billboard billboard;
            try {
                billboard = Display.Billboard.valueOf(billboardStr);
            } catch (IllegalArgumentException e) {
                billboard = Display.Billboard.VERTICAL;
            }
            entity.setBillboard(billboard);

            // 2. Shadow
            boolean shadow = configManager.getConfig().getBoolean("hologram.shadow", true);
            entity.setShadowed(shadow);

            // 3. SeeThrough
            boolean seeThrough = configManager.getConfig().getBoolean("hologram.see-through", false);
            entity.setSeeThrough(seeThrough);

            // 4. TextOpacity
            int opacity = configManager.getConfig().getInt("hologram.text-opacity", -1);
            if (opacity != -1) {
                entity.setTextOpacity((byte) opacity);
            }

            // 5. BackgroundColor
            String bgStr = configManager.getConfig().getString("hologram.background-color", "120,0,0,0");
            Color bgColor = Color.fromARGB(120, 0, 0, 0);
            try {
                String[] parts = bgStr.split(",");
                if (parts.length == 4) {
                    int a = Integer.parseInt(parts[0].trim());
                    int r = Integer.parseInt(parts[1].trim());
                    int g = Integer.parseInt(parts[2].trim());
                    int b = Integer.parseInt(parts[3].trim());
                    bgColor = Color.fromARGB(a, r, g, b);
                }
            } catch (Exception ignored) {}
            entity.setBackgroundColor(bgColor);

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

        return configManager.getRawMessage("hopper_hologram", java.util.Map.of(
                "type", typeName,
                "tier", String.valueOf(hopper.getTier()),
                "items", String.valueOf(hopper.getItemsTransferred())
        ));
    }
}
