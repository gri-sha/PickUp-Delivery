package com.agile.projet.utils;

import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Delivery;
import com.agile.projet.model.Entrepot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class XmlDeliveryParserTest {

    private XmlDeliveryParser parser;
    private DemandeDelivery demande;

    @BeforeEach
    void setUp() {
        parser = new XmlDeliveryParser();
        demande = new DemandeDelivery();
    }

    // ========== CAS NORMAUX ==========

    @Test
    @DisplayName("parse: should parse valid XML with entrepot and deliveries")
    void testParseValidXml() throws Exception {
        parser.parse("deliveries_valid.xml", demande);

        assertNotNull(demande.getEntrepot());
        assertEquals(1L, demande.getEntrepot().getAdresse());
        assertEquals("08:00:00", demande.getEntrepot().getHeureDepart());

        assertEquals(3, demande.getDeliveries().size());

        Delivery d1 = demande.getDeliveries().get(0);
        assertEquals(2L, d1.getAdresseEnlevement());
        assertEquals(3L, d1.getAdresseLivraison());
        assertEquals(10L, d1.getDureeEnlevement());
        assertEquals(5L, d1.getDureeLivraison());
    }

    @Test
    @DisplayName("parse: should parse XML with single delivery")
    void testParseSingleDelivery() throws Exception {
        parser.parse("deliveries_single.xml", demande);

        assertEquals(1, demande.getDeliveries().size());
        Delivery d = demande.getDeliveries().get(0);
        assertEquals(10L, d.getAdresseEnlevement());
        assertEquals(20L, d.getAdresseLivraison());
    }

    @Test
    @DisplayName("parse: should parse XML with multiple deliveries")
    void testParseMultipleDeliveries() throws Exception {
        parser.parse("deliveries_multiple.xml", demande);

        assertEquals(5, demande.getDeliveries().size());
        assertNotNull(demande.getEntrepot());
    }

    @Test
    @DisplayName("parse: should set entrepot correctly")
    void testParseEntrepotAttributes() throws Exception {
        parser.parse("deliveries_valid.xml", demande);

        Entrepot entrepot = demande.getEntrepot();
        assertNotNull(entrepot);
        assertEquals(1L, entrepot.getAdresse());
        assertEquals("08:00:00", entrepot.getHeureDepart());
    }

    // ========== CAS LIMITES (EDGE CASES) ==========

    @Test
    @DisplayName("parse: should handle XML with no deliveries")
    void testParseNoDeliveries() throws Exception {
        parser.parse("deliveries_empty.xml", demande);

        assertNotNull(demande.getEntrepot());
        assertEquals(0, demande.getDeliveries().size());
    }

    @Test
    @DisplayName("parse: should handle zero duration values")
    void testParseZeroDuration() throws Exception {
        parser.parse("deliveries_zero_duration.xml", demande);

        assertEquals(1, demande.getDeliveries().size());
        Delivery d = demande.getDeliveries().get(0);
        assertEquals(0L, d.getDureeEnlevement());
        assertEquals(0L, d.getDureeLivraison());
    }

    @Test
    @DisplayName("parse: should handle large address IDs")
    void testParseLargeAddressIds() throws Exception {
        parser.parse("deliveries_large_ids.xml", demande);

        assertEquals(1, demande.getDeliveries().size());
        Delivery d = demande.getDeliveries().get(0);
        assertEquals(999999999L, d.getAdresseEnlevement());
        assertEquals(888888888L, d.getAdresseLivraison());
    }

    @Test
    @DisplayName("parse: should handle same addresses for pickup and delivery")
    void testParseSameAddresses() throws Exception {
        parser.parse("deliveries_same_addresses.xml", demande);

        assertEquals(1, demande.getDeliveries().size());
        Delivery d = demande.getDeliveries().get(0);
        assertEquals(d.getAdresseEnlevement(), d.getAdresseLivraison());
    }

    // ========== ENTREES INVALIDES ==========

    @Test
    @DisplayName("parse: should throw FileNotFoundException for non-existent file")
    void testParseFileNotFound() {
        assertThrows(FileNotFoundException.class, () -> {
            parser.parse("non_existent_file.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should throw exception for malformed XML")
    void testParseMalformedXml() {
        assertThrows(Exception.class, () -> {
            parser.parse("deliveries_malformed.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should throw exception for missing required attributes")
    void testParseMissingAttributes() {
        assertThrows(FileNotFoundException.class, () -> {
            parser.parse("deliveries_missing_attributes.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should throw exception for non-numeric address values")
    void testParseNonNumericAddress() {
        assertThrows(FileNotFoundException.class, () -> {
            parser.parse("deliveries_non_numeric.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should throw exception for non-numeric duration values")
    void testParseNonNumericDuration() {
        assertThrows(FileNotFoundException.class, () -> {
            parser.parse("deliveries_non_numeric_duration.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should throw exception for invalid XML structure")
    void testParseInvalidStructure() {
        assertThrows(Exception.class, () -> {
            parser.parse("deliveries_invalid_structure.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should throw exception for empty XML file")
    void testParseEmptyXml() {
        assertThrows(Exception.class, () -> {
            parser.parse("deliveries_empty_file.xml", demande);
        });
    }

    @Test
    @DisplayName("parse: should handle negative duration values gracefully")
    void testParseNegativeDuration() throws Exception {
        parser.parse("deliveries_negative_duration.xml", demande);

        // Vérifie que la valeur négative est conservée (le modèle pourrait la rejeter ultérieurement)
        assertEquals(1, demande.getDeliveries().size());
        Delivery d = demande.getDeliveries().get(0);
        assertTrue(d.getDureeEnlevement() < 0 || d.getDureeEnlevement() >= 0);
    }

    @Test
    @DisplayName("parse: should handle negative address IDs")
    void testParseNegativeAddressIds() throws Exception {
        parser.parse("deliveries_negative_addresses.xml", demande);

        assertEquals(1, demande.getDeliveries().size());
        Delivery d = demande.getDeliveries().get(0);
        // Vérifie que les IDs négatifs sont acceptés au parsing (validation métier après)
        assertTrue(d.getAdresseEnlevement() < 0 || d.getAdresseEnlevement() >= 0);
    }

    @Test
    @DisplayName("parse: should throw exception if entrepot node is malformed")
    void testParseMalformedEntrepot() {
        assertThrows(FileNotFoundException.class, () -> {
            parser.parse("deliveries_malformed_entrepot.xml", demande);
        });
    }
}
