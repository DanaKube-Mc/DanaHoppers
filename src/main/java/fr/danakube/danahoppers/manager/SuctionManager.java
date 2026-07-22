package fr.danakube.danahoppers.manager;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTierConfig;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tâche périodique gérant l'aspiration des objets au sol par les CustomHoppers.
 */
public class SuctionManager {

    private final Plugin plugin;
    private final HopperManager hopperManager;
    private final ConfigManager configManager;
    private final HologramManager hologramManager;

    private BukkitTask suctionTask;
    private final Map<UUID, Long> lastSuctionTimes = new ConcurrentHashMap<>();

    public SuctionManager(Plugin plugin, HopperManager hopperManager,
                          ConfigManager configManager, HologramManager hologramManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.hologramManager = hologramManager;
    }

    /**
     * Démarrage de la tâche d'aspiration (s'exécute toutes les 10 ticks = 0.5s).
     */
    public void start() {
        stop();
        suctionTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::processSuctionTick, 10L, 10L);
    }

    /**
     * Arrêt de la tâche d'aspiration.
     */
    public void stop() {
        if (suctionTask != null && !suctionTask.isCancelled()) {
            suctionTask.cancel();
            suctionTask = null;
        }
        lastSuctionTimes.clear();
    }

    /**
     * Exécution à chaque tick de la tâche périodique.
     */
    private void processSuctionTick() {
        long now = System.currentTimeMillis();

        for (CustomHopper hopper : hopperManager.getAllHoppers()) {
            Location loc = hopper.getLocation();
            if (loc == null || !loc.isWorldLoaded()) {
                continue;
            }

            // Vérifier que le chunk du hopper est chargé pour éviter les lag spikes
            if (!loc.getChunk().isLoaded()) {
                continue;
            }

            HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
            if (typeConfig == null) {
                continue;
            }

            HopperTierConfig tierConfig = typeConfig.getTier(hopper.getTier());
            if (tierConfig == null) {
                continue;
            }

            if ("TELEPORTATION".equalsIgnoreCase(tierConfig.suctionType())) {
                continue;
            }

            long intervalMs = (long) (tierConfig.intervalSeconds() * 1000.0);
            Long lastTime = lastSuctionTimes.get(hopper.getHopperUuid());
            if (lastTime != null && (now - lastTime) < intervalMs) {
                continue;
            }

            // Mettre à jour l'horodatage du dernier traitement
            lastSuctionTimes.put(hopper.getHopperUuid(), now);

            // Exécuter l'aspiration pour ce hopper
            processHopperSuction(hopper, tierConfig);
        }
    }

    /**
     * Traitement de l'aspiration pour un CustomHopper spécifique.
     */
    public void processHopperSuction(CustomHopper hopper, HopperTierConfig tierConfig) {
        Location loc = hopper.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // 1. Déterminer les objets au sol selon le type de zone (CUBE ou CHUNK)
        Collection<Item> itemsToProcess = findItemsInRange(world, loc, tierConfig);
        if (itemsToProcess.isEmpty()) {
            return;
        }

        // 2. Déterminer l'inventaire cible (conteneur lié ou entonnoir lui-même)
        Inventory targetInventory = getTargetInventory(hopper);
        if (targetInventory == null) {
            return;
        }

        // 3. Traiter l'aspiration par STACK complète
        boolean transferredAny = false;
        for (Item itemEntity : itemsToProcess) {
            if (!itemEntity.isValid() || itemEntity.isDead() || itemEntity.getPickupDelay() > 0) {
                continue;
            }

            ItemStack stack = itemEntity.getItemStack();
            if (stack == null || stack.getType().isAir()) {
                continue;
            }

            // Vérification du filtrage (WHITELIST / BLACKLIST)
            if (hopper.getFilter() != null && !hopper.getFilter().matches(stack.getType())) {
                continue;
            }

            // Transfert vers l'inventaire cible
            int originalAmount = stack.getAmount();
            HashMap<Integer, ItemStack> remaining = targetInventory.addItem(stack);

            int transferredAmount = originalAmount;
            if (!remaining.isEmpty()) {
                ItemStack leftover = remaining.get(0);
                transferredAmount = originalAmount - leftover.getAmount();
                itemEntity.setItemStack(leftover);
            } else {
                itemEntity.remove();
            }

            if (transferredAmount > 0) {
                hopper.addItemsTransferred(transferredAmount);
                transferredAny = true;
            }

            // Si l'inventaire est plein, on arrête la boucle d'aspiration
            if (!remaining.isEmpty()) {
                break;
            }
        }

        // Mettre à jour l'hologramme si au moins un objet a été aspiré
        if (transferredAny && hologramManager != null && hopper.isHologramEnabled()) {
            hologramManager.updateHologram(hopper);
        }
    }

    private Collection<Item> findItemsInRange(World world, Location loc, HopperTierConfig tierConfig) {
        List<Item> items = new ArrayList<>();

        if ("CHUNK".equalsIgnoreCase(tierConfig.suctionType())) {
            Chunk chunk = loc.getChunk();
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Item item) {
                    items.add(item);
                }
            }
        } else {
            // Zone en CUBE (rayon X, Y, Z)
            int rx = tierConfig.radiusX();
            int ry = tierConfig.radiusY();
            int rz = tierConfig.radiusZ();

            BoundingBox box = new BoundingBox(
                    loc.getBlockX() - rx, loc.getBlockY() - ry, loc.getBlockZ() - rz,
                    loc.getBlockX() + 1 + rx, loc.getBlockY() + 1 + ry, loc.getBlockZ() + 1 + rz
            );

            for (Entity entity : world.getNearbyEntities(box)) {
                if (entity instanceof Item item) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    /**
     * Récupère l'inventaire cible pour le transfert (verifie que la chunk cible est bien chargée).
     */
    private Inventory getTargetInventory(CustomHopper hopper) {
        Location targetLoc = hopper.getLinkedLocation();

        if (targetLoc != null) {
            // Conteneur lié distant : VÉRIFICATION que la chunk cible est chargée pour éviter les lag spikes
            if (targetLoc.isWorldLoaded() && targetLoc.getChunk().isLoaded()) {
                BlockState state = targetLoc.getBlock().getState();
                if (state instanceof InventoryHolder holder) {
                    return holder.getInventory();
                } else if (state instanceof Container container) {
                    return container.getInventory();
                }
            }
            return null; // Chunk non chargée ou conteneur non valide
        } else {
            // Conteneur local (le bloc d'entonnoir lui-même)
            Location loc = hopper.getLocation();
            if (loc != null && loc.isWorldLoaded() && loc.getChunk().isLoaded()) {
                BlockState state = loc.getBlock().getState();
                if (state instanceof InventoryHolder holder) {
                    return holder.getInventory();
                }
            }
            return null;
        }
    }
}
