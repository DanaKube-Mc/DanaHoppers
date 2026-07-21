package fr.danakube.danahoppers.config;

import fr.danakube.danahoppers.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @TempDir
    File tempFolder;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager(tempFolder);
        configManager.loadAll();
    }

    @Test
    @DisplayName("ColorUtil - Test de désérialisation MiniMessage avec et sans placeholders")
    void testColorUtilParsing() {
        Component parsedSimple = ColorUtil.parse("<red>Test Message</red>");
        assertNotNull(parsedSimple);
        String plainTextSimple = PlainTextComponentSerializer.plainText().serialize(parsedSimple);
        assertEquals("Test Message", plainTextSimple);

        Component parsedPlaceholder = ColorUtil.parse("<yellow>Bonjour <player> !</yellow>", Map.of("player", "Steve"));
        assertNotNull(parsedPlaceholder);
        String plainTextPlaceholder = PlainTextComponentSerializer.plainText().serialize(parsedPlaceholder);
        assertEquals("Bonjour Steve !", plainTextPlaceholder);

        List<Component> listParsed = ColorUtil.parseList(List.of("<green>Ligne 1</green>", "<blue>Ligne 2</blue>"));
        assertEquals(2, listParsed.size());
        assertEquals("Ligne 1", PlainTextComponentSerializer.plainText().serialize(listParsed.get(0)));
        assertEquals("Ligne 2", PlainTextComponentSerializer.plainText().serialize(listParsed.get(1)));

        Component parsedWithPrefix = ColorUtil.parseWithPrefix("[Prefix] ", "<green>Message</green>");
        assertNotNull(parsedWithPrefix);
        assertEquals("[Prefix] Message", PlainTextComponentSerializer.plainText().serialize(parsedWithPrefix));
    }

    @Test
    @DisplayName("ConfigManager - Chargement et validation de config.yml")
    void testMainConfigLoading() {
        assertNotNull(configManager.getConfig());
        assertEquals("fr_FR", configManager.getConfig().getString("language"));
        assertEquals("SQLITE", configManager.getConfig().getString("database.type"));
        assertEquals("danahoppers.db", configManager.getConfig().getString("database.sqlite.filename"));
        assertEquals(10, configManager.getConfig().getInt("limits.max-hoppers-per-player"));
        assertEquals(3, configManager.getConfig().getInt("limits.max-hoppers-per-chunk"));
        assertTrue(configManager.getConfig().getBoolean("options.enable-particles"));
    }

    @Test
    @DisplayName("ConfigManager - Chargement et validation de lang/fr_FR.yml")
    void testLangConfigLoading() {
        assertNotNull(configManager.getLangConfig());
        assertTrue(configManager.getPrefix().contains("[DanaHoppers]"));

        Component msgReload = configManager.getMessage("reload_success");
        assertNotNull(msgReload);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(msgReload).contains("rechargés avec succès"));

        Component msgGiven = configManager.getMessage("hopper_given", Map.of("type", "Élargi", "tier", "1"));
        assertNotNull(msgGiven);
        String textGiven = PlainTextComponentSerializer.plainText().serialize(msgGiven);
        assertTrue(textGiven.contains("Élargi"));
        assertTrue(textGiven.contains("Niveau 1"));
    }

    @Test
    @DisplayName("ConfigManager - Chargement des GUIs main_menu.yml et filter_menu.yml")
    void testGuiConfigsLoading() {
        assertNotNull(configManager.getMainMenuConfig());
        assertEquals(27, configManager.getMainMenuConfig().getInt("size"));
        assertNotNull(configManager.getMainMenuConfig().getString("title"));

        assertNotNull(configManager.getFilterMenuConfig());
        assertEquals(27, configManager.getFilterMenuConfig().getInt("size"));
        List<Integer> slots = configManager.getFilterMenuConfig().getIntegerList("filter_slots");
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16), slots);
    }

    @Test
    @DisplayName("ConfigManager - Chargement des types et tiers de Hoppers")
    void testHopperTypesLoading() {
        Map<String, HopperTypeConfig> types = configManager.getHopperTypes();
        assertEquals(3, types.size());
        assertTrue(types.containsKey("collecteur_elargi"));
        assertTrue(types.containsKey("collecteur_zone"));
        assertTrue(types.containsKey("sautelien"));

        // Collecteur Élargi
        HopperTypeConfig elargi = configManager.getHopperType("collecteur_elargi");
        assertNotNull(elargi);
        assertEquals("collecteur_elargi", elargi.id());
        assertEquals(3, elargi.tiers().size());

        HopperTierConfig elargiTier1 = elargi.getTier(1);
        assertNotNull(elargiTier1);
        assertEquals("CUBE", elargiTier1.suctionType());
        assertEquals(3, elargiTier1.radiusX());
        assertEquals(3, elargiTier1.radiusY());
        assertEquals(3, elargiTier1.radiusZ());
        assertEquals(5.0, elargiTier1.intervalSeconds());
        assertEquals(1000.0, elargiTier1.upgradeCostMoney());
        assertEquals(2, elargiTier1.nextTier());

        HopperTierConfig elargiTier3 = elargi.getTier(3);
        assertNotNull(elargiTier3);
        assertEquals(7, elargiTier3.radiusX());
        assertEquals(0.0, elargiTier3.upgradeCostMoney());

        // Collecteur Zone
        HopperTypeConfig zone = configManager.getHopperType("collecteur_zone");
        assertNotNull(zone);
        HopperTierConfig zoneTier1 = zone.getTier(1);
        assertNotNull(zoneTier1);
        assertEquals("CHUNK", zoneTier1.suctionType());

        // SauteLien
        HopperTypeConfig link = configManager.getHopperType("sautelien");
        assertNotNull(link);
        HopperTierConfig linkTier1 = link.getTier(1);
        assertNotNull(linkTier1);
        assertEquals("LINK", linkTier1.suctionType());
        assertEquals(30, linkTier1.maxLinkingDistance());

        HopperTierConfig linkTier3 = link.getTier(3);
        assertNotNull(linkTier3);
        assertEquals(120, linkTier3.maxLinkingDistance());
    }
}
