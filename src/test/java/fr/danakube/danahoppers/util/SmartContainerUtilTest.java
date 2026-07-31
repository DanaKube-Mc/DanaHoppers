package fr.danakube.danahoppers.util;

import org.bukkit.Material;
import org.bukkit.block.Crafter;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires pour la classe SmartContainerUtil")
class SmartContainerUtilTest {

    @BeforeEach
    void setUp() {
        SmartContainerUtil.ensureSmeltableCacheInitialized();
    }

    @Test
    @DisplayName("Test de détection des objets cuisables et combustibles")
    void testSmeltableAndFuelDetection() {
        TestItemStack iron = new TestItemStack(Material.RAW_IRON, 1);
        TestItemStack dirt = new TestItemStack(Material.DIRT, 1);
        TestItemStack coal = new TestItemStack(Material.COAL, 1);

        assertTrue(SmartContainerUtil.isItemSmeltable(iron), "RAW_IRON doit être détecté comme cuisable");
        assertFalse(SmartContainerUtil.isItemSmeltable(dirt), "DIRT ne doit pas être cuisable");
        assertFalse(SmartContainerUtil.isItemSmeltable(null), "Null doit retourner false");

        assertTrue(SmartContainerUtil.isFuel(coal), "COAL doit être détecté comme combustible");
        assertFalse(SmartContainerUtil.isFuel(dirt), "DIRT ne doit pas être un combustible");
    }

    @Test
    @DisplayName("Test de détection des fioles et potions")
    void testPotionBottleDetection() {
        assertTrue(SmartContainerUtil.isPotionOrBottle(Material.GLASS_BOTTLE));
        assertTrue(SmartContainerUtil.isPotionOrBottle(Material.POTION));
        assertTrue(SmartContainerUtil.isPotionOrBottle(Material.SPLASH_POTION));
        assertTrue(SmartContainerUtil.isPotionOrBottle(Material.LINGERING_POTION));

        assertFalse(SmartContainerUtil.isPotionOrBottle(Material.STICK));
        assertFalse(SmartContainerUtil.isPotionOrBottle(null));
    }

    @Test
    @DisplayName("Test d'injection dans un Crafter avec slots désactivés")
    void testCrafterInsertionWithDisabledSlots() {
        Inventory inv = createMockInventory(9, Inventory.class);
        boolean[] disabledSlots = new boolean[9];
        disabledSlots[0] = true;
        disabledSlots[1] = true;
        disabledSlots[2] = true;

        Crafter crafter = createMockCrafter(disabledSlots, inv);

        TestItemStack diamonds = new TestItemStack(Material.DIAMOND, 10);
        ItemStack leftover = SmartContainerUtil.insertItem(crafter, diamonds);

        assertNull(leftover, "Toutes les pierres précieuses doivent être insérées");
        assertNull(inv.getItem(0), "Slot 0 désactivé doit rester vide");
        assertNull(inv.getItem(1), "Slot 1 désactivé doit rester vide");
        assertNull(inv.getItem(2), "Slot 2 désactivé doit rester vide");
        assertNotNull(inv.getItem(3), "L'insertion doit avoir eu lieu au premier slot actif (3)");
        assertEquals(Material.DIAMOND, inv.getItem(3).getType());
        assertEquals(10, inv.getItem(3).getAmount());
    }

    @Test
    @DisplayName("Test d'injection dans un Four (FurnaceInventory)")
    void testFurnaceSmartInsertion() {
        FurnaceInventory furnace = createMockInventory(3, FurnaceInventory.class);

        // 1. Ingrédient cuisable -> Slot 0
        TestItemStack rawIron = new TestItemStack(Material.RAW_IRON, 32);
        ItemStack leftover1 = SmartContainerUtil.insertItem((Inventory) furnace, rawIron);
        assertNull(leftover1);
        assertNotNull(furnace.getItem(0));
        assertEquals(Material.RAW_IRON, furnace.getItem(0).getType());
        assertEquals(32, furnace.getItem(0).getAmount());

        // 2. Combustible -> Slot 1
        TestItemStack coal = new TestItemStack(Material.COAL, 16);
        ItemStack leftover2 = SmartContainerUtil.insertItem((Inventory) furnace, coal);
        assertNull(leftover2);
        assertNotNull(furnace.getItem(1));
        assertEquals(Material.COAL, furnace.getItem(1).getType());
        assertEquals(16, furnace.getItem(1).getAmount());

        // 3. Slot 2 (Résultat) doit rester strictement vide
        assertNull(furnace.getItem(2), "Le Slot 2 de résultat du four doit rester vide");

        // 4. Objet non valide (ni cuisable ni combustible) -> Rejeté
        TestItemStack dirt = new TestItemStack(Material.DIRT, 64);
        ItemStack leftover3 = SmartContainerUtil.insertItem((Inventory) furnace, dirt);
        assertNotNull(leftover3);
        assertEquals(64, leftover3.getAmount());
    }

    @Test
    @DisplayName("Test d'injection dans un Alambic (BrewerInventory)")
    void testBrewerSmartInsertion() {
        BrewerInventory brewer = createMockInventory(5, BrewerInventory.class);

        // 1. Blaze Powder -> Slot 4 (Fuel)
        TestItemStack blazePowder = new TestItemStack(Material.BLAZE_POWDER, 20);
        ItemStack leftover1 = SmartContainerUtil.insertItem((Inventory) brewer, blazePowder);
        assertNull(leftover1);
        assertNotNull(brewer.getItem(4));
        assertEquals(20, brewer.getItem(4).getAmount());

        // 2. Fioles -> Slot 0
        TestItemStack bottle = new TestItemStack(Material.GLASS_BOTTLE, 1);
        ItemStack leftover2 = SmartContainerUtil.insertItem((Inventory) brewer, bottle);
        assertNull(leftover2);
        assertNotNull(brewer.getItem(0));
        assertEquals(Material.GLASS_BOTTLE, brewer.getItem(0).getType());

        // 3. Ingrédient (Nether Wart) -> Slot 3
        TestItemStack netherWart = new TestItemStack(Material.NETHER_WART, 5);
        ItemStack leftover3 = SmartContainerUtil.insertItem((Inventory) brewer, netherWart);
        assertNull(leftover3);
        assertNotNull(brewer.getItem(3));
        assertEquals(Material.NETHER_WART, brewer.getItem(3).getType());
    }

    @Test
    @DisplayName("Test d'injection dans un inventaire général (Coffre standard)")
    void testGeneralInventoryInsertion() {
        Inventory chest = createMockInventory(27, Inventory.class);

        TestItemStack stones = new TestItemStack(Material.STONE, 64);
        ItemStack leftover = SmartContainerUtil.insertItem(chest, stones);

        assertNull(leftover);
        assertEquals(Material.STONE, chest.getItem(0).getType());
        assertEquals(64, chest.getItem(0).getAmount());
    }

    @Test
    @DisplayName("Test avec objets et conteneurs nulls ou vides")
    void testNullOrEmptyHandling() {
        Inventory chest = createMockInventory(9, Inventory.class);

        assertNull(SmartContainerUtil.insertItem(chest, null));
        TestItemStack stack = new TestItemStack(Material.DIRT, 10);
        assertEquals(stack, SmartContainerUtil.insertItem((Inventory) null, stack));
    }

    // --- SOUS-CLASSE ITEMSTACK PUR JAVA SANS SERVEUR BUKKIT ---

    public static class TestItemStack extends ItemStack {
        private Material material;
        private int count;

        public TestItemStack(Material material, int count) {
            super();
            this.material = material;
            this.count = count;
        }

        @Override
        public Material getType() {
            return material;
        }

        @Override
        public int getAmount() {
            return count;
        }

        @Override
        public void setAmount(int amount) {
            this.count = amount;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public boolean isSimilar(ItemStack stack) {
            if (stack == null) return false;
            return stack.getType() == this.material;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ItemStack other)) return false;
            return other.getType() == this.material && other.getAmount() == this.count;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(material, count);
        }

        @Override
        public TestItemStack clone() {
            return new TestItemStack(this.material, this.count);
        }
    }

    // --- HELPER DYNAMIQUE DE MOCK D'INVENTAIRE POUR LES TESTS ---

    @SuppressWarnings("unchecked")
    private <T extends Inventory> T createMockInventory(int size, Class<T> invInterface) {
        ItemStack[] contents = new ItemStack[size];

        return (T) Proxy.newProxyInstance(
                invInterface.getClassLoader(),
                new Class<?>[]{invInterface},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getSize")) return size;
                    if (name.equals("getMaxStackSize")) return 64;
                    if (name.equals("getItem")) {
                        int slot = (int) args[0];
                        return (slot >= 0 && slot < size) ? contents[slot] : null;
                    }
                    if (name.equals("setItem")) {
                        int slot = (int) args[0];
                        ItemStack item = (ItemStack) args[1];
                        if (slot >= 0 && slot < size) {
                            contents[slot] = item;
                        }
                        return null;
                    }
                    if (name.equals("addItem")) {
                        ItemStack[] items = (ItemStack[]) args[0];
                        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
                        for (int i = 0; i < items.length; i++) {
                            ItemStack toAdd = items[i];
                            if (toAdd == null || SmartContainerUtil.isAir(toAdd.getType())) continue;
                            ItemStack remaining = toAdd.clone();

                            // Pass 1: existing matching stack
                            for (int slot = 0; slot < size; slot++) {
                                ItemStack current = contents[slot];
                                if (current != null && current.isSimilar(remaining)) {
                                    int space = 64 - current.getAmount();
                                    if (space > 0) {
                                        int add = Math.min(space, remaining.getAmount());
                                        current.setAmount(current.getAmount() + add);
                                        remaining.setAmount(remaining.getAmount() - add);
                                        if (remaining.getAmount() <= 0) break;
                                    }
                                }
                            }
                            // Pass 2: empty slot
                            if (remaining.getAmount() > 0) {
                                for (int slot = 0; slot < size; slot++) {
                                    if (contents[slot] == null || SmartContainerUtil.isAir(contents[slot].getType())) {
                                        int add = Math.min(64, remaining.getAmount());
                                        ItemStack newStack = remaining.clone();
                                        newStack.setAmount(add);
                                        contents[slot] = newStack;
                                        remaining.setAmount(remaining.getAmount() - add);
                                        if (remaining.getAmount() <= 0) break;
                                    }
                                }
                            }
                            if (remaining.getAmount() > 0) {
                                leftovers.put(i, remaining);
                            }
                        }
                        return leftovers;
                    }
                    return null;
                }
        );
    }

    private Crafter createMockCrafter(boolean[] disabledSlots, Inventory inv) {
        return (Crafter) Proxy.newProxyInstance(
                Crafter.class.getClassLoader(),
                new Class<?>[]{Crafter.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isSlotDisabled".equals(name)) {
                        int slot = (int) args[0];
                        return slot >= 0 && slot < disabledSlots.length && disabledSlots[slot];
                    }
                    if ("getInventory".equals(name) || "getSnapshotInventory".equals(name)) {
                        return inv;
                    }
                    return null;
                }
        );
    }
}
