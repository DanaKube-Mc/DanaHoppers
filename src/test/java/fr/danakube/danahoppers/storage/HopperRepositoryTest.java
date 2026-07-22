package fr.danakube.danahoppers.storage;

import fr.danakube.danahoppers.manager.HopperManager;
import fr.danakube.danahoppers.model.CustomHopper;
import fr.danakube.danahoppers.model.FilterMode;
import fr.danakube.danahoppers.model.HopperFilter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests d'intégration/unité pour la couche de persistance SQLite et HopperManager")
class HopperRepositoryTest {

    private DatabaseManager databaseManager;
    private HopperRepository hopperRepository;
    private HopperManager hopperManager;
    private File testDbFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        testDbFile = tempDir.resolve("test_danahoppers.db").toFile();
        databaseManager = new DatabaseManager(testDbFile);
        databaseManager.initAsync().join();

        hopperRepository = new HopperRepository(databaseManager);
        hopperManager = new HopperManager(hopperRepository);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    @DisplayName("Sauvegarde et chargement d'un hopper dans la base SQLite")
    void testSaveAndLoadHopper() {
        UUID hopperUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        Location loc = new Location(null, 10, 64, -20);
        Location linkedLoc = new Location(null, 10, 70, -20);

        HopperFilter filter = new HopperFilter(FilterMode.BLACKLIST, List.of(Material.TNT, Material.LAVA_BUCKET));

        CustomHopper hopper = new CustomHopper(
                hopperUuid,
                ownerUuid,
                loc,
                "auto_craft",
                2,
                linkedLoc,
                filter,
                150L,
                true,
                true
        );

        hopperRepository.saveHopper(hopper).join();

        List<CustomHopper> loadedHoppers = hopperRepository.loadAllHoppers().join();
        assertEquals(1, loadedHoppers.size());

        CustomHopper loaded = loadedHoppers.get(0);
        assertEquals(hopperUuid, loaded.getHopperUuid());
        assertEquals(ownerUuid, loaded.getOwnerUuid());
        assertEquals(10, loaded.getLocation().getBlockX());
        assertEquals(64, loaded.getLocation().getBlockY());
        assertEquals(-20, loaded.getLocation().getBlockZ());
        assertEquals("auto_craft", loaded.getTypeId());
        assertEquals(2, loaded.getTier());
        assertEquals(FilterMode.BLACKLIST, loaded.getFilter().getMode());
        assertTrue(loaded.getFilter().getMaterials().contains(Material.TNT));
        assertEquals(150L, loaded.getItemsTransferred());
        assertTrue(loaded.isHologramEnabled());
        assertTrue(loaded.isTeleportEnabled());
    }

    @Test
    @DisplayName("Suppression d'un hopper dans la base SQLite")
    void testDeleteHopper() {
        UUID hopperUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        Location loc = new Location(null, 0, 100, 0);

        CustomHopper hopper = new CustomHopper(hopperUuid, ownerUuid, loc, "default", 1);
        hopperRepository.saveHopper(hopper).join();

        assertEquals(1, hopperRepository.loadAllHoppers().join().size());

        hopperRepository.deleteHopper(hopperUuid).join();
        assertEquals(0, hopperRepository.loadAllHoppers().join().size());
    }

    @Test
    @DisplayName("Test complet du HopperManager (cache mémoire + persistance)")
    void testHopperManagerCacheAndPersistence() {
        UUID hopperUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        Location loc = new Location(null, 5, 5, 5);

        CustomHopper hopper = new CustomHopper(hopperUuid, ownerUuid, loc, "speed", 3);

        // Register in manager
        hopperManager.registerHopper(hopper).join();

        // Verification cache
        assertTrue(hopperManager.isCustomHopper(loc));
        assertEquals(hopper, hopperManager.getHopper(loc));
        assertEquals(1, hopperManager.getAllHoppers().size());

        // Clear cache and reload from DB
        hopperManager.clearCache();
        assertFalse(hopperManager.isCustomHopper(loc));

        hopperManager.loadAllAsync().join();
        assertTrue(hopperManager.isCustomHopper(loc));
        assertEquals(hopperUuid, hopperManager.getHopper(loc).getHopperUuid());

        // Unregister
        hopperManager.unregisterHopper(loc).join();
        assertFalse(hopperManager.isCustomHopper(loc));
        assertEquals(0, hopperRepository.loadAllHoppers().join().size());
    }
}
