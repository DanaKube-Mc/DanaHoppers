package fr.danakube.danahoppers.model;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires pour la classe HopperFilter")
class HopperFilterTest {

    private HopperFilter filter;

    @BeforeEach
    void setUp() {
        filter = new HopperFilter();
    }

    @Test
    @DisplayName("Initialisation par défaut du filtre")
    void testDefaultInitialization() {
        assertEquals(FilterMode.BLACKLIST, filter.getMode());
        assertTrue(filter.getMaterials().isEmpty());
    }

    @Test
    @DisplayName("Test de filtrage en mode WHITELIST")
    void testWhitelistMatching() {
        filter.setMode(FilterMode.WHITELIST);
        filter.addMaterial(Material.DIAMOND);
        filter.addMaterial(Material.IRON_INGOT);

        assertTrue(filter.matches(Material.DIAMOND), "Le diamant doit être accepté en WHITELIST");
        assertTrue(filter.matches(Material.IRON_INGOT), "Le lingot de fer doit être accepté en WHITELIST");
        assertFalse(filter.matches(Material.DIRT), "La terre doit être rejetée en WHITELIST");
        assertFalse(filter.matches(null), "Null doit retourner false");
        assertFalse(filter.matches(Material.AIR), "AIR doit retourner false");
    }

    @Test
    @DisplayName("Test de filtrage en mode BLACKLIST")
    void testBlacklistMatching() {
        filter.setMode(FilterMode.BLACKLIST);
        filter.addMaterial(Material.DIRT);
        filter.addMaterial(Material.STONE);

        assertFalse(filter.matches(Material.DIRT), "La terre doit être rejetée en BLACKLIST");
        assertFalse(filter.matches(Material.STONE), "La pierre doit être rejetée en BLACKLIST");
        assertTrue(filter.matches(Material.DIAMOND), "Le diamant doit être accepté en BLACKLIST");
        assertFalse(filter.matches(null), "Null doit retourner false");
        assertFalse(filter.matches(Material.AIR), "AIR doit retourner false");
    }

    @Test
    @DisplayName("Ajout et suppression de matériaux")
    void testAddAndRemoveMaterials() {
        assertTrue(filter.addMaterial(Material.EMERALD));
        assertFalse(filter.addMaterial(Material.EMERALD), "Un matériau déjà présent ne doit pas être réajouté");
        assertFalse(filter.addMaterial(null), "Null ne doit pas être ajouté");
        assertFalse(filter.addMaterial(Material.AIR), "AIR ne doit pas être ajouté");

        assertEquals(1, filter.getMaterials().size());
        assertTrue(filter.getMaterials().contains(Material.EMERALD));

        assertTrue(filter.removeMaterial(Material.EMERALD));
        assertFalse(filter.removeMaterial(Material.EMERALD), "Retirer un matériau absent retourne false");
        assertTrue(filter.getMaterials().isEmpty());
    }

    @Test
    @DisplayName("Vidage de la liste de matériaux")
    void testClearMaterials() {
        filter.addMaterial(Material.GOLD_INGOT);
        filter.addMaterial(Material.COPPER_INGOT);
        assertEquals(2, filter.getMaterials().size());

        filter.clearMaterials();
        assertTrue(filter.getMaterials().isEmpty());
    }

    @Test
    @DisplayName("Sérialisation et désérialisation des matériaux")
    void testSerializationAndDeserialization() {
        filter.addMaterial(Material.DIAMOND);
        filter.addMaterial(Material.GOLD_INGOT);

        String serialized = filter.serializeMaterials();
        assertTrue(serialized.contains("DIAMOND"));
        assertTrue(serialized.contains("GOLD_INGOT"));

        List<Material> deserialized = HopperFilter.deserializeMaterials(serialized);
        assertEquals(2, deserialized.size());
        assertTrue(deserialized.contains(Material.DIAMOND));
        assertTrue(deserialized.contains(Material.GOLD_INGOT));
    }

    @Test
    @DisplayName("Changement de mode de filtrage")
    void testSetMode() {
        filter.setMode(FilterMode.BLACKLIST);
        assertEquals(FilterMode.BLACKLIST, filter.getMode());

        assertThrows(NullPointerException.class, () -> filter.setMode(null));
    }
}
