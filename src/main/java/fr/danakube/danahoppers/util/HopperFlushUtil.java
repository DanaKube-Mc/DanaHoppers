package fr.danakube.danahoppers.util;

import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Utilitaires pour le vidage automatique (flush) de l'inventaire interne des CustomHoppers vers les conteneurs liés.
 */
public final class HopperFlushUtil {

    private HopperFlushUtil() {
        // Classe utilitaire non instanciable
    }

    /**
     * Vide l'inventaire interne du Hopper physique dans le conteneur cible lié.
     *
     * @param customHopper l'instance de CustomHopper à traiter
     */
    public static void flushInternalInventory(CustomHopper customHopper) {
        if (customHopper == null) {
            return;
        }

        Location linkedLoc = customHopper.getLinkedLocation();
        if (linkedLoc == null || !linkedLoc.isWorldLoaded()) {
            return;
        }

        Location hopperLoc = customHopper.getLocation();
        if (hopperLoc == null || !hopperLoc.isWorldLoaded()) {
            return;
        }

        if (!hopperLoc.getChunk().isLoaded() || !linkedLoc.getChunk().isLoaded()) {
            return;
        }

        // Récupérer le BlockState de l'entonnoir physique
        Block hopperBlock = hopperLoc.getBlock();
        BlockState hopperState = hopperBlock.getState(false);
        if (!(hopperState instanceof InventoryHolder)) {
            hopperState = hopperBlock.getState();
        }
        if (!(hopperState instanceof InventoryHolder hopperHolder)) {
            return;
        }

        Inventory hopperInv = hopperHolder.getInventory();
        if (hopperInv == null || hopperInv.isEmpty()) {
            return;
        }

        // Récupérer le BlockState du conteneur lié
        Block targetBlock = linkedLoc.getBlock();
        BlockState targetState = targetBlock.getState(false);
        if (!(targetState instanceof InventoryHolder)) {
            targetState = targetBlock.getState();
        }
        if (!(targetState instanceof InventoryHolder)) {
            return;
        }

        // Parcourir les slots de l'entonnoir et insérer intelligemment
        int size = hopperInv.getSize();
        long totalItemsMoved = 0;

        for (int i = 0; i < size; i++) {
            ItemStack stack = hopperInv.getItem(i);
            if (stack == null || SmartContainerUtil.isAir(stack.getType()) || stack.getAmount() <= 0) {
                continue;
            }

            int originalAmount = stack.getAmount();
            ItemStack leftover = SmartContainerUtil.insertItem(targetState, stack);
            int leftoverAmount = (leftover == null || SmartContainerUtil.isAir(leftover.getType()) || leftover.getAmount() <= 0)
                    ? 0 : leftover.getAmount();

            int moved = originalAmount - leftoverAmount;
            if (moved > 0) {
                totalItemsMoved += moved;
                if (leftoverAmount <= 0) {
                    hopperInv.setItem(i, null);
                } else {
                    leftover.setAmount(leftoverAmount);
                    hopperInv.setItem(i, leftover);
                }
            }
        }

        if (totalItemsMoved > 0) {
            customHopper.addItemsTransferred(totalItemsMoved);
        }
    }
}
