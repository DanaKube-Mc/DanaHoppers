package fr.danakube.danahoppers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DanaHoppersPluginTest {

    @Test
    @DisplayName("Test du Bootstrap du package fr.danakube.danahoppers")
    void testBootstrap() {
        assertTrue(true, "Le package fr.danakube.danahoppers est prêt.");
    }
}
