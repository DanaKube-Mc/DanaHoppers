package fr.danakube.danahoppers.util;

import fr.danakube.danahoppers.model.CustomHopper;
import org.bukkit.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires pour la classe HopperFlushUtil")
class HopperFlushUtilTest {

    @Test
    @DisplayName("Test de vidage avec un CustomHopper null ou sans localisation liée")
    void testFlushWithNullOrUnlinkedHopper() {
        // Ne doit pas lever d'exception
        assertDoesNotThrow(() -> HopperFlushUtil.flushInternalInventory(null));

        CustomHopper unlinkedHopper = new CustomHopper(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Location(null, 10, 64, 10),
                "BASIC",
                1
        );

        assertDoesNotThrow(() -> HopperFlushUtil.flushInternalInventory(unlinkedHopper));
        assertEquals(0L, unlinkedHopper.getItemsTransferred());
    }

    @Test
    @DisplayName("Test de vidage avec un CustomHopper lié mais monde non chargé")
    void testFlushWithUnloadedWorld() {
        Location hopperLoc = new Location(null, 0, 64, 0);
        Location targetLoc = new Location(null, 0, 65, 0);

        CustomHopper hopper = new CustomHopper(
                UUID.randomUUID(),
                UUID.randomUUID(),
                hopperLoc,
                "BASIC",
                1,
                targetLoc,
                null,
                0L,
                true,
                false
        );

        assertDoesNotThrow(() -> HopperFlushUtil.flushInternalInventory(hopper));
        assertEquals(0L, hopper.getItemsTransferred());
    }
}
