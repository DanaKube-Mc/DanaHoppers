package fr.danakube.danahoppers.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utilitaires pour l'intégration avec le plugin AdvancedChests.
 */
public final class AdvancedChestHook {

    private AdvancedChestHook() {
        // Classe utilitaire non instanciable
    }

    /**
     * Indique si le plugin AdvancedChests est présent et activé sur le serveur.
     */
    public static boolean isAdvancedChestsActive() {
        return Bukkit.getPluginManager().getPlugin("AdvancedChests") != null &&
               Bukkit.getPluginManager().getPlugin("AdvancedChests").isEnabled();
    }

    /**
     * Tente de récupérer la liste de TOUS les inventaires Bukkit (toutes les pages)
     * gérés par AdvancedChests à une localisation donnée.
     *
     * @param location la localisation du bloc cible
     * @return la liste des Inventory Bukkit d'AdvancedChests, ou une liste vide
     */
    public static List<Inventory> getAllAdvancedChestInventories(Location location) {
        List<Inventory> inventories = new ArrayList<>();
        if (!isAdvancedChestsActive() || location == null) {
            return inventories;
        }

        try {
            Class<?> apiClass = Class.forName("us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI");
            Method getChestManagerMethod = apiClass.getMethod("getChestManager");
            Object chestManager = getChestManagerMethod.invoke(null);
            if (chestManager == null) {
                return inventories;
            }

            Method getAdvancedChestMethod = chestManager.getClass().getMethod("getAdvancedChest", Location.class);
            Object advancedChest = getAdvancedChestMethod.invoke(chestManager, location);
            if (advancedChest == null) {
                return inventories;
            }

            Method getPagesMethod = advancedChest.getClass().getMethod("getPages");
            Object pagesObj = getPagesMethod.invoke(advancedChest);

            Collection<?> pagesCollection = null;
            if (pagesObj instanceof Map<?, ?> pagesMap) {
                pagesCollection = pagesMap.values();
            } else if (pagesObj instanceof Object[] pagesArray) {
                pagesCollection = Arrays.asList(pagesArray);
            } else if (pagesObj instanceof Collection<?> pagesCol) {
                pagesCollection = pagesCol;
            }

            if (pagesCollection != null) {
                for (Object page : pagesCollection) {
                    if (page == null) continue;
                    try {
                        Method getBukkitInventoryMethod = page.getClass().getMethod("getBukkitInventory");
                        Object inventoryObj = getBukkitInventoryMethod.invoke(page);
                        if (inventoryObj instanceof Inventory inv) {
                            inventories.add(inv);
                        }
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            Bukkit.getLogger().warning("[DanaHoppers/AdvancedChestHook] Erreur lors de la récupération des inventaires AdvancedChests: " + e.getMessage());
        }

        return inventories;
    }
}
