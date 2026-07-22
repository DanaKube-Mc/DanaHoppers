package fr.danakube.danahoppers.gui;

import fr.danakube.danahoppers.config.ConfigManager;
import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryBuilderTest {

    @TempDir
    File tempFolder;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager(tempFolder);
        configManager.loadAll();
    }

    @Test
    @DisplayName("DanaHopperHolder - Instanciation et getters")
    void testHolderCreation() {
        UUID hopperUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        Location loc = new Location(null, 10, 64, 10);
        CustomHopper hopper = new CustomHopper(hopperUuid, ownerUuid, loc, "collecteur_elargi", 1);

        DanaHopperHolder holder = new DanaHopperHolder(hopper, DanaHopperHolder.MenuType.MAIN_MENU);

        assertEquals(hopper, holder.getHopper());
        assertEquals(DanaHopperHolder.MenuType.MAIN_MENU, holder.getMenuType());
        assertNull(holder.getInventory());
    }

    @Test
    @DisplayName("InventoryBuilder - Récupération des filter slots")
    void testFilterSlotsRetrieval() {
        List<Integer> filterSlots = InventoryBuilder.getFilterSlots(configManager.getFilterMenuConfig());
        assertNotNull(filterSlots);
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16), filterSlots);
    }
}
