package fr.danakube.danahoppers.listener;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.manager.HologramManager;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.model.HopperFilter;
import fr.danakube.danahoppers.util.HopperItemUtil;
import fr.danakube.danahoppers.util.PDCUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Écouteur des événements liés aux blocs CustomHopper : Pose, Destruction, Explosions, Pistons et Aspiration Vanilla.
 */
public class HopperBlockListener implements Listener {

    private final Plugin plugin;
    private final HopperManager hopperManager;
    private final ConfigManager configManager;
    private final HologramManager hologramManager;

    public HopperBlockListener(Plugin plugin, HopperManager hopperManager,
                               ConfigManager configManager, HologramManager hologramManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.hologramManager = Objects.requireNonNull(hologramManager, "hologramManager cannot be null");
    }

    /**
     * Pose d'un CustomHopper par un joueur.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemHand = event.getItemInHand();
        if (!HopperItemUtil.isCustomHopperItem(itemHand, plugin)) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        String typeId = HopperItemUtil.getTypeId(itemHand, plugin).orElse("collecteur_elargi");
        int tier = HopperItemUtil.getTier(itemHand, plugin);

        // Restaurer le filtre s'il est présent sur l'item
        HopperFilter filter = new HopperFilter();
        if (itemHand.hasItemMeta()) {
            ItemMeta meta = itemHand.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                String modeStr = pdc.get(new NamespacedKey(plugin, "filter_mode"), PersistentDataType.STRING);
                String matsStr = pdc.get(new NamespacedKey(plugin, "filter_materials"), PersistentDataType.STRING);
                if (modeStr != null) {
                    try {
                        filter.setMode(FilterMode.valueOf(modeStr));
                    } catch (IllegalArgumentException ignored) {}
                }
                if (matsStr != null) {
                    List<Material> mats = HopperFilter.deserializeMaterials(matsStr);
                    for (Material m : mats) {
                        filter.addMaterial(m);
                    }
                }
            }
        }

        UUID hopperUuid = UUID.randomUUID();
        CustomHopper hopper = new CustomHopper(hopperUuid, player.getUniqueId(), block.getLocation(), typeId, tier);
        hopper.setFilter(filter);

        // Écriture dans le PDC du TileState
        if (block.getState() instanceof TileState tileState) {
            PDCUtil.saveToPDC(tileState, hopper, plugin);
        }

        // Sauvegarde BDD & Cache
        hopperManager.registerHopper(hopper);

        // Création de l'hologramme
        if (hopper.isHologramEnabled()) {
            hologramManager.createHologram(hopper);
        }

        player.sendMessage(configManager.getMessage("hopper_placed", Map.of("type", typeId)));
    }

    /**
     * Destruction d'un CustomHopper par un joueur.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        CustomHopper hopper = hopperManager.getHopper(loc);
        if (hopper == null) {
            return;
        }

        Player player = event.getPlayer();

        // Annuler les drops par défaut du bloc
        event.setDropItems(false);

        // Retirer l'hologramme
        hologramManager.removeHologram(hopper);

        // Nettoyer le PDC du bloc
        if (block.getState() instanceof TileState tileState) {
            PDCUtil.removeFromPDC(tileState, plugin);
        }

        // Suppression de la BDD & du cache
        hopperManager.unregisterHopper(loc);

        // Générer et dropper l'objet physique personnalise
        ItemStack customDrop = HopperItemUtil.createHopperItem(hopper.getTypeId(), hopper.getTier(), hopper, configManager, plugin);
        World world = block.getWorld();
        world.dropItemNaturally(loc, customDrop);

        player.sendMessage(configManager.getMessage("hopper_broken", Map.of("type", hopper.getTypeId())));
    }

    /**
     * Protection contre les explosions d'entités (TNT, Creeper, etc.).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> hopperManager.isCustomHopper(block.getLocation()));
    }

    /**
     * Protection contre les explosions de blocs.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> hopperManager.isCustomHopper(block.getLocation()));
    }

    /**
     * Protection contre les poussées de pistons.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (hopperManager.isCustomHopper(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Protection contre les rétractions de pistons.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (hopperManager.isCustomHopper(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Annulation de l'aspiration vanilla pour les CustomHoppers.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        Location loc = event.getInventory().getLocation();
        if (loc != null && hopperManager.isCustomHopper(loc)) {
            // Empêcher l'aspiration vanilla par défaut pour laisser le SuctionManager gérer
            event.setCancelled(true);
        }
    }
}
