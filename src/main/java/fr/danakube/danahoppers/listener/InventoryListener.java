package fr.danakube.danahoppers.listener;

import fr.danakube.danahoppers.gui.DanaHopperHolder;
import fr.danakube.danahoppers.gui.HopperFilterMenu;
import fr.danakube.danahoppers.gui.HopperMainMenu;
import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Objects;

/**
 * Écouteur centralisé des événements d'inventaire (InventoryClickEvent, InventoryCloseEvent)
 * redirigeant vers les menus appropriés via DanaHopperHolder.
 */
public class InventoryListener implements Listener {

    private final HopperMainMenu mainMenu;
    private final HopperFilterMenu filterMenu;

    public InventoryListener(HopperMainMenu mainMenu, HopperFilterMenu filterMenu) {
        this.mainMenu = Objects.requireNonNull(mainMenu, "mainMenu cannot be null");
        this.filterMenu = Objects.requireNonNull(filterMenu, "filterMenu cannot be null");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof DanaHopperHolder holder) {
            if (event.getWhoClicked() instanceof Player player) {
                CustomHopper hopper = holder.getHopper();
                if (hopper != null) {
                    if (holder.getMenuType() == DanaHopperHolder.MenuType.MAIN_MENU) {
                        mainMenu.handleClick(event, player, hopper);
                    } else if (holder.getMenuType() == DanaHopperHolder.MenuType.FILTER_MENU) {
                        filterMenu.handleClick(event, player, hopper);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof DanaHopperHolder holder) {
            if (event.getPlayer() instanceof Player player) {
                CustomHopper hopper = holder.getHopper();
                if (hopper != null && holder.getMenuType() == DanaHopperHolder.MenuType.FILTER_MENU) {
                    filterMenu.handleClose(event, player, hopper);
                }
            }
        }
    }
}
