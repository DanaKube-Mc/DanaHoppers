package fr.danakube.danahoppers.listener;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTierConfig;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.gui.HopperMainMenu;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.util.PDCUtil;
import fr.danakube.danahoppers.util.SkyblockUtil;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import com.bgsoftware.superiorskyblock.api.island.Island;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Écouteur des interactions des joueurs avec les CustomHoppers (clic-droit, ouverture du GUI et mode liaison).
 */
public class PlayerInteractListener implements Listener {

    private final Plugin plugin;
    private final HopperManager hopperManager;
    private final ConfigManager configManager;
    private final HopperMainMenu hopperMainMenu;

    private static final Map<UUID, CustomHopper> linkingPlayers = new ConcurrentHashMap<>();

    public PlayerInteractListener(Plugin plugin, HopperManager hopperManager,
                                  ConfigManager configManager, HopperMainMenu hopperMainMenu) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.hopperMainMenu = Objects.requireNonNull(hopperMainMenu, "hopperMainMenu cannot be null");
    }

    public static void setLinkingPlayer(UUID playerUuid, CustomHopper hopper) {
        if (playerUuid != null && hopper != null) {
            linkingPlayers.put(playerUuid, hopper);
        }
    }

    public static void removeLinkingPlayer(UUID playerUuid) {
        if (playerUuid != null) {
            linkingPlayers.remove(playerUuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Ignorer la main secondaire pour éviter le double déclenchement
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();

        // 1. Traitement du Mode Liaison si actif pour ce joueur
        if (linkingPlayers.containsKey(playerUuid)) {
            event.setCancelled(true);
            CustomHopper hopper = linkingPlayers.remove(playerUuid);

            if (hopper == null || hopper.getLocation() == null) {
                return;
            }

            Location hopperLoc = hopper.getLocation();
            Location targetLoc = block.getLocation();

            // Interdire de lier le hopper à lui-même
            if (hopperLoc.getWorld().equals(targetLoc.getWorld()) &&
                hopperLoc.getBlockX() == targetLoc.getBlockX() &&
                hopperLoc.getBlockY() == targetLoc.getBlockY() &&
                hopperLoc.getBlockZ() == targetLoc.getBlockZ()) {
                player.sendMessage(configManager.getRawMessage("link_failed_self"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }

            // Vérifier que le bloc cible est un conteneur valide (coffre, double coffre, entonnoir, etc.)
            BlockState state = block.getState();
            if (!(state instanceof InventoryHolder)) {
                player.sendMessage(configManager.getRawMessage("link_failed_invalid"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }

            // Vérification de la distance maximale
            HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
            HopperTierConfig tierConfig = typeConfig != null ? typeConfig.getTier(hopper.getTier()) : null;
            int maxDistance = tierConfig != null ? tierConfig.maxLinkingDistance() : 0;

            if (!hopperLoc.getWorld().equals(targetLoc.getWorld()) || hopperLoc.distance(targetLoc) > maxDistance) {
                player.sendMessage(configManager.getMessage("link_failed_distance", Map.of("distance", String.valueOf(maxDistance))));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }

            // Liaison réussie
            hopper.setLinkedLocation(targetLoc);

            // Sauvegarder PDC et BDD
            Block hopperBlock = hopperLoc.getBlock();
            if (hopperBlock.getState() instanceof TileState tileState) {
                PDCUtil.saveToPDC(tileState, hopper, plugin);
            }
            hopperManager.registerHopper(hopper);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            player.sendMessage(configManager.getMessage("link_success", Map.of(
                    "x", String.valueOf(targetLoc.getBlockX()),
                    "y", String.valueOf(targetLoc.getBlockY()),
                    "z", String.valueOf(targetLoc.getBlockZ())
            )));
            return;
        }

        // 2. Interaction normale avec un CustomHopper -> Ouverture du GUI principal
        CustomHopper hopper = hopperManager.getHopper(block.getLocation());
        if (hopper != null) {
            // Si le joueur s'accroupit (sneak) et fait un clic droit, on le laisse accéder à l'inventaire physique du hopper
            if (player.isSneaking()) {
                return;
            }

            event.setCancelled(true);

            // Vérification du monde désactivé
            List<String> disabledWorlds = configManager.getConfig().getStringList("disabled-worlds");
            if (disabledWorlds != null && disabledWorlds.contains(block.getWorld().getName())) {
                player.sendMessage(configManager.getRawMessage("disabled_world"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                return;
            }

            // Intégration SuperiorSkyblock2 : Interdire d'interagir si non membre
            boolean useSkyblock = configManager.getConfig().getBoolean("integrations.superiorskyblock", true);
            if (useSkyblock && SkyblockUtil.isSkyblockActive()) {
                Island island = SkyblockUtil.getIslandAt(block.getLocation());
                if (island != null && !SkyblockUtil.isIslandMember(player, island)) {
                    player.sendMessage(configManager.getRawMessage("not_island_member"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    return;
                }
            }

            hopperMainMenu.open(player, hopper);
        }
    }
}
