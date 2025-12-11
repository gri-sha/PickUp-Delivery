package com.agile.projet.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Controller class preconditions and basic behaviors.
 */
public class ControllerTest {

    private Controller controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new Controller();
    }

    @Test
    @DisplayName("computeShortestPaths throws when plan is missing")
    void computeShortestPaths_noPlan_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> controller.computeShortestPaths());
        assertTrue(ex.getMessage().contains("Plan manquant"));
    }

    @Test
    @DisplayName("computeShortestPaths runs when plan and delivery are set")
    void computeShortestPaths_withPlanAndDelivery_runs() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        assertDoesNotThrow(() -> controller.computeShortestPaths());
    }

    @Test
    @DisplayName("findBestPath throws when depot missing in model")
    void findBestPath_noDepot_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> controller.findBestPath());
        assertTrue(ex.getMessage().contains("Entrepôt manquant"));
    }

    @Test
    @DisplayName("buildFullPath throws when tournee not computed")
    void buildFullPath_withoutTournee_throws() {
        // buildFullPath indirectly uses internal state; without calling findBestPath, it should NPE.
        // We expect a NullPointerException due to tournee being null.
        assertThrows(NullPointerException.class, () -> controller.buildFullPath());
    }

    @Test
    @DisplayName("createPlan loads plan without exceptions")
    void createPlan_success() {
        assertDoesNotThrow(() -> controller.createPlan("grandPlan.xml"));
    }

    @Test
    @DisplayName("createDeliveryFromXml loads deliveries without exceptions")
    void createDelivery_success() throws Exception {
        controller.createPlan("grandPlan.xml");
        assertDoesNotThrow(() -> controller.createDeliveryFromXml("demandeGrand7.xml"));
    }

    @Test
    @DisplayName("printMatriceChemins works after computeShortestPaths")
    void printMatriceChemins_afterCompute() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        controller.computeShortestPaths();
        assertDoesNotThrow(() -> controller.printMatriceChemins());
    }

    @Test
    @DisplayName("findBestPath returns a tournee with steps")
    void findBestPath_success() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        controller.computeShortestPaths();
        var tournee = controller.findBestPath();
        assertNotNull(tournee);
        assertNotNull(tournee.getEtapes());
        assertTrue(tournee.getEtapes().size() > 0);
    }

    @Test
    @DisplayName("buildFullPath returns expanded node sequence")
    void buildFullPath_success() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        controller.computeShortestPaths();
        controller.findBestPath();
        var path = controller.buildFullPath();
        assertNotNull(path);
        assertTrue(path.size() >= 1);
    }

    @Test
    @DisplayName("getTrajetAvecTouteEtapes callable for coverage")
    void getTrajetAvecTouteEtapes_call() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        controller.computeShortestPaths();
        assertDoesNotThrow(() -> controller.getTrajetAvecTouteEtapes());
    }
}
