package fr.danakube.danahoppers.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.Crafter;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitaire pour l'injection intelligente d'objets dans divers conteneurs Minecraft.
 */
public final class SmartContainerUtil {

    private static final Set<Material> SMELTABLE_MATERIALS = ConcurrentHashMap.newKeySet();
    private static volatile boolean cacheInitialized = false;

    private SmartContainerUtil() {
        // Classe utilitaire non instanciable
    }

    /**
     * Vérie si un matériau représente de l'air de manière sûre sans invoquer le Registry Paper.
     */
    public static boolean isAir(Material mat) {
        if (mat == null) return true;
        return mat == Material.AIR || mat == Material.CAVE_AIR || mat == Material.VOID_AIR;
    }

    /**
     * Insère un ItemStack dans le conteneur représenté par le BlockState spécifié.
     *
     * @param targetState   le BlockState cible
     * @param stackToInsert l'objet à insérer
     * @return le surplus non inséré, ou null (ou stack de 0) si tout a été inséré
     */
    public static ItemStack insertItem(BlockState targetState, ItemStack stackToInsert) {
        if (stackToInsert == null || isAir(stackToInsert.getType()) || stackToInsert.getAmount() <= 0) {
            return null;
        }
        if (targetState == null) {
            return stackToInsert;
        }

        Inventory inventory = null;
        if (targetState instanceof Chest chest) {
            if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest) {
                inventory = doubleChest.getInventory();
            } else {
                inventory = chest.getInventory();
            }
        } else if (targetState instanceof Container container) {
            inventory = container.getInventory();
        } else if (targetState instanceof InventoryHolder holder) {
            inventory = holder.getInventory();
        }

        if (inventory == null) {
            return stackToInsert;
        }

        return insertItemInternal(targetState, inventory, stackToInsert);
    }

    /**
     * Surcharges spécifiques pour résoudre l'ambiguïté Java quand targetContainer est un Container.
     */
    public static ItemStack insertItem(Container targetContainer, ItemStack stackToInsert) {
        return insertItem((BlockState) targetContainer, stackToInsert);
    }

    /**
     * Insère un ItemStack dans le conteneur représenté par l'InventoryHolder spécifié.
     *
     * @param targetHolder  l'InventoryHolder cible
     * @param stackToInsert l'objet à insérer
     * @return le surplus non inséré, ou null si tout a été inséré
     */
    public static ItemStack insertItem(InventoryHolder targetHolder, ItemStack stackToInsert) {
        if (stackToInsert == null || isAir(stackToInsert.getType()) || stackToInsert.getAmount() <= 0) {
            return null;
        }
        if (targetHolder == null) {
            return stackToInsert;
        }

        BlockState blockState = targetHolder instanceof BlockState bs ? bs : null;
        Inventory inventory;
        if (targetHolder instanceof DoubleChest doubleChest) {
            inventory = doubleChest.getInventory();
        } else if (targetHolder instanceof Chest chest && chest.getInventory().getHolder() instanceof DoubleChest doubleChest) {
            inventory = doubleChest.getInventory();
        } else {
            inventory = targetHolder.getInventory();
        }

        if (inventory == null) {
            return stackToInsert;
        }

        return insertItemInternal(blockState, inventory, stackToInsert);
    }

    /**
     * Insère un ItemStack dans l'inventaire spécifié.
     *
     * @param targetInventory l'inventaire cible
     * @param stackToInsert   l'objet à insérer
     * @return le surplus non inséré, ou null si tout a été inséré
     */
    public static ItemStack insertItem(Inventory targetInventory, ItemStack stackToInsert) {
        if (stackToInsert == null || isAir(stackToInsert.getType()) || stackToInsert.getAmount() <= 0) {
            return null;
        }
        if (targetInventory == null) {
            return stackToInsert;
        }

        BlockState blockState = targetInventory.getHolder() instanceof BlockState bs ? bs : null;
        return insertItemInternal(blockState, targetInventory, stackToInsert);
    }

    /**
     * Méthode interne acheminant l'insertion selon le type d'inventaire / BlockState.
     */
    private static ItemStack insertItemInternal(BlockState blockState, Inventory inventory, ItemStack stackToInsert) {
        Crafter crafter = null;
        if (blockState instanceof Crafter c) {
            crafter = c;
        } else if (inventory.getHolder() instanceof Crafter c) {
            crafter = c;
        }

        if (crafter != null) {
            return insertIntoCrafter(crafter, inventory, stackToInsert);
        }

        if (inventory instanceof FurnaceInventory furnaceInv) {
            return insertIntoFurnace(furnaceInv, stackToInsert);
        }

        if (inventory instanceof BrewerInventory brewerInv) {
            return insertIntoBrewer(brewerInv, stackToInsert);
        }

        return insertIntoGeneral(inventory, stackToInsert);
    }

    /**
     * Insertion dans un Crafter 1.21 en respectant les slots désactivés (isSlotDisabled).
     */
    private static ItemStack insertIntoCrafter(Crafter crafter, Inventory inventory, ItemStack stackToInsert) {
        ItemStack remaining = stackToInsert.clone();
        int size = Math.min(9, inventory.getSize());

        // 1. Essayer de compléter les stacks existantes dans les slots non désactivés (0 à 8)
        for (int slot = 0; slot < size; slot++) {
            if (crafter.isSlotDisabled(slot)) {
                continue;
            }
            ItemStack current = inventory.getItem(slot);
            if (current != null && !isAir(current.getType()) && current.isSimilar(remaining)) {
                int maxStack = Math.min(current.getMaxStackSize(), inventory.getMaxStackSize());
                int space = maxStack - current.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, remaining.getAmount());
                    current.setAmount(current.getAmount() + toAdd);
                    inventory.setItem(slot, current);
                    remaining.setAmount(remaining.getAmount() - toAdd);
                    if (remaining.getAmount() <= 0) {
                        return null;
                    }
                }
            }
        }

        // 2. Placer dans les slots vides non désactivés (0 à 8)
        for (int slot = 0; slot < size; slot++) {
            if (crafter.isSlotDisabled(slot)) {
                continue;
            }
            ItemStack current = inventory.getItem(slot);
            if (current == null || isAir(current.getType())) {
                int maxStack = Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize());
                int toAdd = Math.min(maxStack, remaining.getAmount());
                ItemStack newStack = remaining.clone();
                newStack.setAmount(toAdd);
                inventory.setItem(slot, newStack);
                remaining.setAmount(remaining.getAmount() - toAdd);
                if (remaining.getAmount() <= 0) {
                    return null;
                }
            }
        }

        return remaining.getAmount() > 0 ? remaining : null;
    }

    /**
     * Insertion dans un Four (FurnaceInventory) : Slot 0 = Ingrédient cuisable, Slot 1 = Combustible, Slot 2 = Résultat ignoré.
     */
    private static ItemStack insertIntoFurnace(FurnaceInventory inventory, ItemStack stackToInsert) {
        ItemStack remaining = stackToInsert.clone();
        boolean smeltable = isItemSmeltable(remaining);
        boolean fuel = isFuel(remaining);

        if (smeltable) {
            remaining = tryInsertIntoSlot(inventory, 0, remaining);
            if (remaining == null || remaining.getAmount() <= 0) {
                return null;
            }
        }

        if (fuel) {
            remaining = tryInsertIntoSlot(inventory, 1, remaining);
            if (remaining == null || remaining.getAmount() <= 0) {
                return null;
            }
        }

        return remaining.getAmount() > 0 ? remaining : null;
    }

    /**
     * Insertion dans un Alambic (BrewerInventory) :
     * Slot 4 = Fuel (Blaze Powder), Slots 0..2 = Fioles/Potions, Slot 3 = Ingrédient.
     */
    private static ItemStack insertIntoBrewer(BrewerInventory inventory, ItemStack stackToInsert) {
        ItemStack remaining = stackToInsert.clone();
        Material type = remaining.getType();

        // 1. Poudre de Blaze -> Slot 4 (Fuel)
        if (type == Material.BLAZE_POWDER) {
            remaining = tryInsertIntoSlot(inventory, 4, remaining);
            if (remaining == null || remaining.getAmount() <= 0) {
                return null;
            }
            // S'il en reste, Blaze Powder peut être un ingrédient de brassage dans le Slot 3
            remaining = tryInsertIntoSlot(inventory, 3, remaining);
            return (remaining == null || remaining.getAmount() <= 0) ? null : remaining;
        }

        // 2. Fioles & Potions -> Slots 0, 1, 2
        if (isPotionOrBottle(type)) {
            for (int slot = 0; slot <= 2; slot++) {
                ItemStack current = inventory.getItem(slot);
                if (current != null && !isAir(current.getType()) && current.isSimilar(remaining)) {
                    remaining = tryInsertIntoSlot(inventory, slot, remaining);
                    if (remaining == null || remaining.getAmount() <= 0) return null;
                }
            }
            for (int slot = 0; slot <= 2; slot++) {
                ItemStack current = inventory.getItem(slot);
                if (current == null || isAir(current.getType())) {
                    remaining = tryInsertIntoSlot(inventory, slot, remaining);
                    if (remaining == null || remaining.getAmount() <= 0) return null;
                }
            }
            return remaining.getAmount() > 0 ? remaining : null;
        }

        // 3. Ingrédients de brassage -> Slot 3
        remaining = tryInsertIntoSlot(inventory, 3, remaining);
        return (remaining == null || remaining.getAmount() <= 0) ? null : remaining;
    }

    /**
     * Insertion générale (Coffres, DoubleCoffres, Barils, ShulkerBox, Entonnoirs, Droppers, etc.).
     */
    private static ItemStack insertIntoGeneral(Inventory inventory, ItemStack stackToInsert) {
        ItemStack remaining = stackToInsert.clone();
        HashMap<Integer, ItemStack> leftoverMap = inventory.addItem(remaining);
        if (leftoverMap == null || leftoverMap.isEmpty()) {
            return null;
        }
        ItemStack leftover = leftoverMap.get(0);
        return (leftover == null || isAir(leftover.getType()) || leftover.getAmount() <= 0) ? null : leftover;
    }

    /**
     * Tente d'insérer un ItemStack dans un slot spécifique de l'inventaire.
     */
    private static ItemStack tryInsertIntoSlot(Inventory inv, int slot, ItemStack remaining) {
        if (slot < 0 || slot >= inv.getSize()) {
            return remaining;
        }
        ItemStack current = inv.getItem(slot);
        if (current == null || isAir(current.getType())) {
            int maxStack = Math.min(remaining.getMaxStackSize(), inv.getMaxStackSize());
            int toAdd = Math.min(maxStack, remaining.getAmount());
            ItemStack newStack = remaining.clone();
            newStack.setAmount(toAdd);
            inv.setItem(slot, newStack);
            int left = remaining.getAmount() - toAdd;
            if (left <= 0) return null;
            remaining.setAmount(left);
            return remaining;
        } else if (current.isSimilar(remaining)) {
            int maxStack = Math.min(current.getMaxStackSize(), inv.getMaxStackSize());
            int space = maxStack - current.getAmount();
            if (space > 0) {
                int toAdd = Math.min(space, remaining.getAmount());
                current.setAmount(current.getAmount() + toAdd);
                inv.setItem(slot, current);
                int left = remaining.getAmount() - toAdd;
                if (left <= 0) return null;
                remaining.setAmount(left);
                return remaining;
            }
        }
        return remaining;
    }

    /**
     * Détermine si un type de matériau est une fiole ou potion.
     */
    public static boolean isPotionOrBottle(Material mat) {
        if (mat == null) return false;
        return mat == Material.GLASS_BOTTLE
                || mat == Material.POTION
                || mat == Material.SPLASH_POTION
                || mat == Material.LINGERING_POTION;
    }

    /**
     * Détermine si un objet est cuisable (recette de cuisson dans un four/smoker/blast furnace).
     */
    public static boolean isItemSmeltable(ItemStack stack) {
        if (stack == null || isAir(stack.getType())) {
            return false;
        }
        ensureSmeltableCacheInitialized();
        return SMELTABLE_MATERIALS.contains(stack.getType());
    }

    /**
     * Détermine si un objet est un combustible.
     */
    public static boolean isFuel(ItemStack stack) {
        if (stack == null || isAir(stack.getType())) {
            return false;
        }
        Material m = stack.getType();
        return m == Material.COAL || m == Material.CHARCOAL || m == Material.COAL_BLOCK
                || m == Material.BLAZE_ROD || m == Material.LAVA_BUCKET || m.name().endsWith("_WOOD")
                || m.name().endsWith("_LOG") || m.name().endsWith("_PLANKS");
    }

    /**
     * Initialise le cache des matériaux cuisables (optimisation O(1)).
     */
    public static void ensureSmeltableCacheInitialized() {
        if (cacheInitialized) {
            return;
        }
        synchronized (SMELTABLE_MATERIALS) {
            if (cacheInitialized) {
                return;
            }

            // Pré-remplissage avec les matériaux cuisables vanilla standard
            populateStandardSmeltables();

            try {
                if (Bukkit.getServer() != null) {
                    Iterator<Recipe> it = Bukkit.recipeIterator();
                    while (it.hasNext()) {
                        Recipe recipe = it.next();
                        if (recipe instanceof CookingRecipe<?> cookingRecipe) {
                            RecipeChoice choice = cookingRecipe.getInputChoice();
                            if (choice instanceof RecipeChoice.MaterialChoice matChoice) {
                                SMELTABLE_MATERIALS.addAll(matChoice.getChoices());
                            } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                                for (ItemStack choiceStack : exactChoice.getChoices()) {
                                    SMELTABLE_MATERIALS.add(choiceStack.getType());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // Environnement de test unitaire ou serveur non démarré
            }
            cacheInitialized = true;
        }
    }

    /**
     * Pré-remplit les éléments cuisables de base.
     */
    private static void populateStandardSmeltables() {
        addIfPresent("RAW_IRON", "RAW_COPPER", "RAW_GOLD", "IRON_ORE", "COPPER_ORE", "GOLD_ORE",
                "DEEPSLATE_IRON_ORE", "DEEPSLATE_COPPER_ORE", "DEEPSLATE_GOLD_ORE",
                "NETHER_GOLD_ORE", "ANCIENT_DEBRIS", "COBBLESTONE", "SAND", "RED_SAND",
                "CLAY_BALL", "WET_SPONGE", "PORKCHOP", "BEEF", "CHICKEN", "COD", "SALMON",
                "RABBIT", "MUTTON", "POTATO", "KELP", "LOG", "OAK_LOG", "SPRUCE_LOG",
                "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG",
                "CHERRY_LOG", "CRIMSON_HYPHAE", "WARPED_HYPHAE");
    }

    private static void addIfPresent(String... names) {
        for (String name : names) {
            try {
                Material m = Material.matchMaterial(name);
                if (m != null) {
                    SMELTABLE_MATERIALS.add(m);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Enregistre un matériau cuisable (utilisé pour étendre manuellement ou pour les tests).
     */
    public static void registerSmeltableMaterial(Material material) {
        if (material != null) {
            SMELTABLE_MATERIALS.add(material);
        }
    }
}
