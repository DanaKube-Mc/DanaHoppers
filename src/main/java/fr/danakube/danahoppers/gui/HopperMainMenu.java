package fr.danakube.danahoppers.gui;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.config.HopperTierConfig;
import fr.danakube.danahoppers.config.HopperTypeConfig;
import fr.danakube.danahoppers.listener.PlayerInteractListener;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.util.PDCUtil;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;

/**
 * Gestionnaire d'ouverture et d'interactions pour le menu principal du CustomHopper.
 */
public class HopperMainMenu {

    private final InventoryBuilder inventoryBuilder;
    private final HopperManager hopperManager;
    private final ConfigManager configManager;
    private final Plugin plugin;

    public HopperMainMenu(InventoryBuilder inventoryBuilder, HopperManager hopperManager,
                          ConfigManager configManager, Plugin plugin) {
        this.inventoryBuilder = Objects.requireNonNull(inventoryBuilder, "inventoryBuilder cannot be null");
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    /**
     * Ouvre le menu principal pour le joueur donné.
     */
    public void open(Player player, CustomHopper hopper) {
        Inventory inv = inventoryBuilder.buildMainMenu(hopper);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    /**
     * Gère les clics d'inventaire dans le menu principal.
     */
    public void handleClick(InventoryClickEvent event, Player player, CustomHopper hopper) {
        event.setCancelled(true); // Bloquer tous les retraits d'items du menu principal

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            return;
        }

        switch (rawSlot) {
            case 11 -> { // Bouton Filtre
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                HopperFilterMenu filterMenu = new HopperFilterMenu(inventoryBuilder, hopperManager, configManager, plugin);
                filterMenu.open(player, hopper);
            }
            case 13 -> { // Bouton Amélioration (Upgrade)
                handleUpgrade(player, hopper);
            }
            case 15 -> { // Bouton Liaison (Link)
                handleLinkMode(player, hopper);
            }
            default -> {
                // Aucun comportement particulier sur les autres slots
            }
        }
    }

    private void handleUpgrade(Player player, CustomHopper hopper) {
        HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
        if (typeConfig == null) {
            player.sendMessage(configManager.getMessage("unknown_hopper_type", Map.of("type", hopper.getTypeId())));
            return;
        }

        HopperTierConfig currentTierConfig = typeConfig.getTier(hopper.getTier());
        if (currentTierConfig == null || currentTierConfig.nextTier() <= 0) {
            player.sendMessage(configManager.getMessage("max_tier_reached"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }

        int nextTierNum = currentTierConfig.nextTier();

        // Appliquer l'amélioration
        hopper.setTier(nextTierNum);

        // Mise à jour PDC et BDD
        Block block = hopper.getLocation().getBlock();
        if (block.getState() instanceof TileState tileState) {
            PDCUtil.saveToPDC(tileState, hopper, plugin);
        }
        hopperManager.registerHopper(hopper);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        player.sendMessage(configManager.getMessage("hopper_upgraded", Map.of(
                "tier", String.valueOf(nextTierNum),
                "cost", String.format("%.0f", currentTierConfig.upgradeCostMoney())
        )));

        // Rafraîchir l'inventaire
        open(player, hopper);
    }

    private void handleLinkMode(Player player, CustomHopper hopper) {
        HopperTypeConfig typeConfig = configManager.getHopperType(hopper.getTypeId());
        HopperTierConfig tierConfig = typeConfig != null ? typeConfig.getTier(hopper.getTier()) : null;
        int maxDist = tierConfig != null ? tierConfig.maxLinkingDistance() : 0;

        if (maxDist <= 0) {
            player.sendMessage(configManager.getRawMessage("link_failed_invalid"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
            return;
        }

        // Enregistrer l'état de liaison temporaire du joueur
        PlayerInteractListener.setLinkingPlayer(player.getUniqueId(), hopper);

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        player.sendMessage(configManager.getMessage("link_mode_enabled", Map.of("distance", String.valueOf(maxDist))));
    }
}
