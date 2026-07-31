package fr.danakube.danahoppers.listener;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.manager.HologramManager;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.model.HopperFilter;
import fr.danakube.danahoppers.util.HopperItemUtil;
import fr.danakube.danahoppers.util.PDCUtil;
import fr.danakube.danahoppers.util.SkyblockUtil;
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

import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;

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
        Location blockLoc = block.getLocation();

        // 0. Vérification du monde désactivé
        List<String> disabledWorlds = configManager.getConfig().getStringList("disabled-worlds");
        if (disabledWorlds != null && disabledWorlds.contains(blockLoc.getWorld().getName())) {
            configManager.sendRawMessage(player, "disabled_world");
            event.setCancelled(true);
            return;
        }

        // 1. Intégration SuperiorSkyblock2 : Vérification du placement
        boolean useSkyblock = configManager.getConfig().getBoolean("integrations.superiorskyblock", true);
        Island island = null;

        if (useSkyblock && SkyblockUtil.isSkyblockActive()) {
            island = SkyblockUtil.getIslandAt(blockLoc);
            if (island == null) {
                configManager.sendRawMessage(player, "not_on_island");
                event.setCancelled(true);
                return;
            }
            // Vérifier si membre de l'île
            if (!SkyblockUtil.isIslandMember(player, island)) {
                configManager.sendRawMessage(player, "not_island_member");
                event.setCancelled(true);
                return;
            }
        }

        // 2. Vérification de la limite de placement
        if (!player.hasPermission("danahoppers.admin.bypasslimit") && !player.hasPermission("danahoppers.admin")) {
            int maxAllowed = getLimitAllowed(player, island);
            int currentCount = countHoppersPlaced(player.getUniqueId(), island);

            if (currentCount >= maxAllowed) {
                configManager.sendMessage(player, "hopper_limit_reached", Map.of("limit", String.valueOf(maxAllowed)));
                event.setCancelled(true);
                return;
            }
        }

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
        CustomHopper hopper = new CustomHopper(hopperUuid, player.getUniqueId(), blockLoc, typeId, tier);
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

        configManager.sendMessage(player, "hopper_placed", Map.of("type", typeId));
    }

    /**
     * Calcule la limite de hoppers autorisés pour un joueur ou son île.
     */
    private int getLimitAllowed(Player player, Island island) {
        Player targetPlayer = player;
        
        boolean useIslandLimits = configManager.getConfig().getBoolean("limits.use-island-limits", true);

        // Si skyblock est actif et qu'on utilise les limites par île, la limite se base sur les permissions du leader
        if (useIslandLimits && island != null) {
            SuperiorPlayer superiorOwner = island.getOwner();
            if (superiorOwner != null) {
                Player onlineOwner = superiorOwner.asPlayer();
                if (onlineOwner != null) {
                    targetPlayer = onlineOwner;
                }
            }
        }

        if (targetPlayer.hasPermission("danahoppers.admin.bypasslimit") || targetPlayer.hasPermission("danahoppers.admin")) {
            return Integer.MAX_VALUE;
        }

        int max = 0;
        List<String> limitsList = configManager.getConfig().getStringList("limits.limits-by-permission");
        for (String entry : limitsList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                String perm = parts[0].trim();
                try {
                    int limitVal = Integer.parseInt(parts[1].trim());
                    if (targetPlayer.hasPermission(perm)) {
                        if (limitVal > max) {
                            max = limitVal;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // Limite par défaut
        return max > 0 ? max : 3;
    }

    /**
     * Compte le nombre de hoppers déjà placés par un joueur ou sur une île.
     */
    private int countHoppersPlaced(UUID playerUuid, Island island) {
        int count = 0;
        boolean useIslandLimits = configManager.getConfig().getBoolean("limits.use-island-limits", true);

        for (CustomHopper h : hopperManager.getAllHoppers()) {
            if (useIslandLimits && island != null) {
                // Mode île : voir si le hopper est situé sur la même île
                Island locIsland = SkyblockUtil.getIslandAt(h.getLocation());
                if (locIsland != null && locIsland.getUniqueId().equals(island.getUniqueId())) {
                    count++;
                }
            } else {
                // Mode individuel : comparer l'UUID du propriétaire
                if (playerUuid.equals(h.getOwnerUuid())) {
                    count++;
                }
            }
        }
        return count;
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

        // Intégration SuperiorSkyblock2 : Interdire de casser si non membre
        boolean useSkyblock = configManager.getConfig().getBoolean("integrations.superiorskyblock", true);
        if (useSkyblock && SkyblockUtil.isSkyblockActive()) {
            Island island = SkyblockUtil.getIslandAt(loc);
            if (island != null && !SkyblockUtil.isIslandMember(player, island)) {
                configManager.sendRawMessage(player, "not_island_member");
                event.setCancelled(true);
                return;
            }
        }

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

        configManager.sendMessage(player, "hopper_broken", Map.of("type", hopper.getTypeId()));
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
