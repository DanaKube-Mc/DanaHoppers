package fr.danakube.danahoppers.gui;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.util.PDCUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Gestionnaire d'ouverture, d'interactions et de fermeture du menu de filtre du CustomHopper.
 * Restitue immédiatement les objets physiques aux joueurs lors de la fermeture pour éviter toute perte/duplication.
 */
public class HopperFilterMenu {

    private final InventoryBuilder inventoryBuilder;
    private final HopperManager hopperManager;
    private final ConfigManager configManager;
    private final Plugin plugin;

    public HopperFilterMenu(InventoryBuilder inventoryBuilder, HopperManager hopperManager,
                            ConfigManager configManager, Plugin plugin) {
        this.inventoryBuilder = Objects.requireNonNull(inventoryBuilder, "inventoryBuilder cannot be null");
        this.hopperManager = Objects.requireNonNull(hopperManager, "hopperManager cannot be null");
        this.configManager = Objects.requireNonNull(configManager, "configManager cannot be null");
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    /**
     * Ouvre le menu de filtre pour le joueur.
     */
    public void open(Player player, CustomHopper hopper) {
        Inventory inv = inventoryBuilder.buildFilterMenu(hopper);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
    }

    /**
     * Gère les clics dans le menu de filtre.
     */
    public void handleClick(InventoryClickEvent event, Player player, CustomHopper hopper) {
        int rawSlot = event.getRawSlot();
        Inventory inv = event.getInventory();

        // Si le clic s'effectue dans l'inventaire du joueur (bas de l'écran), autoriser
        if (rawSlot >= inv.getSize()) {
            return;
        }

        List<Integer> filterSlots = InventoryBuilder.getFilterSlots(configManager.getFilterMenuConfig());

        int modeSlot = configManager.getFilterMenuConfig() != null ?
                configManager.getFilterMenuConfig().getInt("mode_button.slot", 0) : 0;
        int clearSlot = configManager.getFilterMenuConfig() != null ?
                configManager.getFilterMenuConfig().getInt("clear_button.slot", 8) : 8;

        if (rawSlot == modeSlot) {
            event.setCancelled(true);
            // Basculer le mode de filtre
            FilterMode currentMode = hopper.getFilter().getMode();
            FilterMode newMode = (currentMode == FilterMode.WHITELIST) ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
            hopper.getFilter().setMode(newMode);

            // Sauvegarder
            saveHopperData(hopper);

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
            player.sendMessage(configManager.getMessage("filter_mode_changed", Map.of("mode", newMode.name())));

            // Ré-ouvrir le menu mis à jour
            open(player, hopper);

        } else if (rawSlot == clearSlot) {
            event.setCancelled(true);
            // Vider le filtre et restituer les items actuellement placés dans les filter slots au joueur
            restituteAndClearFilterSlots(inv, filterSlots, player);

            hopper.getFilter().clearMaterials();
            saveHopperData(hopper);

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f);
            player.sendMessage(configManager.getMessage("filter_cleared"));

            // Ré-ouvrir le menu mis à jour
            open(player, hopper);

        } else if (filterSlots.contains(rawSlot)) {
            // Emplacement de filtre : autoriser l'interaction pour que le joueur place/retire des objets physiques
            // Le clic est autorisé pour modification directe du slot.
        } else {
            // Autres emplacements de fond (vitres de décoration) : interdire le clic
            event.setCancelled(true);
        }
    }

    /**
     * Intercepte la fermeture de l'inventaire du filtre (InventoryCloseEvent) :
     * 1. Lit les types d'objets placés dans les slots de filtrage.
     * 2. Met à jour la liste des matériaux dans le HopperFilter.
     * 3. Restitue immédiatement l'intégralité des objets physiques au joueur (dans l'inventaire ou au sol si plein).
     */
    public void handleClose(InventoryCloseEvent event, Player player, CustomHopper hopper) {
        Inventory inv = event.getInventory();
        List<Integer> filterSlots = InventoryBuilder.getFilterSlots(configManager.getFilterMenuConfig());

        List<Material> extractedMaterials = new ArrayList<>();

        for (int slot : filterSlots) {
            if (slot >= 0 && slot < inv.getSize()) {
                ItemStack stack = inv.getItem(slot);
                if (stack != null && stack.getType() != Material.AIR) {
                    if (!extractedMaterials.contains(stack.getType())) {
                        extractedMaterials.add(stack.getType());
                    }
                }
            }
        }

        // Mettre à jour le filtre
        hopper.getFilter().clearMaterials();
        for (Material mat : extractedMaterials) {
            hopper.getFilter().addMaterial(mat);
        }
        saveHopperData(hopper);

        // Restituer les objets physiques au joueur et vider l'inventaire GUI
        restituteAndClearFilterSlots(inv, filterSlots, player);
    }

    private void restituteAndClearFilterSlots(Inventory inv, List<Integer> filterSlots, Player player) {
        for (int slot : filterSlots) {
            if (slot >= 0 && slot < inv.getSize()) {
                ItemStack stack = inv.getItem(slot);
                if (stack != null && stack.getType() != Material.AIR) {
                    inv.setItem(slot, null); // Vider le slot de l'inventaire du GUI
                    Map<Integer, ItemStack> remaining = player.getInventory().addItem(stack);
                    if (!remaining.isEmpty()) {
                        for (ItemStack drop : remaining.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
            }
        }
    }

    private void saveHopperData(CustomHopper hopper) {
        Block block = hopper.getLocation().getBlock();
        if (block.getState() instanceof TileState tileState) {
            PDCUtil.saveToPDC(tileState, hopper, plugin);
        }
        hopperManager.registerHopper(hopper);
    }
}
