package fr.danakube.danahoppers.gui;

import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

/**
 * InventoryHolder personnalisé permettant de lier un inventaire Bukkit
 * à une instance de CustomHopper et de connaître le type de menu ouvert.
 */
public class DanaHopperHolder implements InventoryHolder {

    public enum MenuType {
        MAIN_MENU,
        FILTER_MENU
    }

    private final CustomHopper hopper;
    private final MenuType menuType;
    private Inventory inventory;

    public DanaHopperHolder(CustomHopper hopper, MenuType menuType) {
        this.hopper = Objects.requireNonNull(hopper, "hopper cannot be null");
        this.menuType = Objects.requireNonNull(menuType, "menuType cannot be null");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public CustomHopper getHopper() {
        return hopper;
    }

    public MenuType getMenuType() {
        return menuType;
    }
}
