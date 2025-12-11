
package com.agile.projet.utils;

import com.agile.projet.model.*;
import com.agile.projet.utils.CalculPlusCoursChemins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CalculPlusCoursChemins Tests")
class CalculPlusCoursCheminsTest {

    private CalculPlusCoursChemins calculator;
    private Plan plan;
    private DemandeDelivery demandeDelivery;
    private PickupDeliveryModel model;

    @BeforeEach
    void setUp() {
        calculator = new CalculPlusCoursChemins();
        plan = mock(Plan.class);
        demandeDelivery = mock(DemandeDelivery.class);
        model = mock(PickupDeliveryModel.class);
    }

    // ========== COMPUTE METHOD TESTS ==========

    @Test
    @DisplayName("compute: should handle empty troncons list")
    void testComputeEmptyTroncons() {
        when(plan.getTroncons()).thenReturn(new ArrayList<>());
        when(demandeDelivery.getDeliveries()).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> calculator.compute(plan, demandeDelivery));
    }

    @Test
    @DisplayName("compute: should handle empty deliveries list")
    void testComputeEmptyDeliveries() {
        List<Troncon> troncons = new ArrayList<>();
        Troncon t1 = mock(Troncon.class);
        when(t1.getDestination()).thenReturn(1L);
        when(t1.getOrigine()).thenReturn(2L);
        troncons.add(t1);

        when(plan.getTroncons()).thenReturn(troncons);
        when(demandeDelivery.getDeliveries()).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> calculator.compute(plan, demandeDelivery));
    }

    @Test
    @DisplayName("compute: should process valid graph and deliveries")
    void testComputeValidInput() {
        List<Troncon> troncons = new ArrayList<>();
        Troncon t1 = mock(Troncon.class);
        when(t1.getDestination()).thenReturn(1L);
        when(t1.getOrigine()).thenReturn(2L);
        troncons.add(t1);

        List<Delivery> deliveries = new ArrayList<>();
        Delivery d1 = mock(Delivery.class);
        when(d1.getAdresseEnlevement()).thenReturn(2L);
        when(d1.getAdresseLivraison()).thenReturn(1L);
        deliveries.add(d1);

        when(plan.getTroncons()).thenReturn(troncons);
        when(demandeDelivery.getDeliveries()).thenReturn(deliveries);

        assertDoesNotThrow(() -> calculator.compute(plan, demandeDelivery));
    }

    @Test
    @DisplayName("compute: should handle null plan")
    void testComputeNullPlan() {
        assertThrows(NullPointerException.class, () -> calculator.compute(null, demandeDelivery));
    }

    @Test
    @DisplayName("compute: should handle null demandeDelivery")
    void testComputeNullDemandeDelivery() {
        when(plan.getTroncons()).thenReturn(new ArrayList<>());
        assertThrows(NullPointerException.class, () -> calculator.compute(plan, null));
    }

    // ========== COMPUTE ASTAR METHOD TESTS ==========

    @Test
    @DisplayName("computeAstar: should handle empty troncons and deliveries")
    void testComputeAstarEmpty() {
        when(plan.getVraiTroncons()).thenReturn(new ArrayList<>());
        when(model.getEntrepot()).thenReturn(null);
        when(demandeDelivery.getDeliveries()).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> calculator.computeAstar(plan, demandeDelivery, model));
        verify(model).setVertexOrder(new ArrayList<>());
    }

    @Test
    @DisplayName("computeAstar: should process valid graph with multiple nodes")
    void testComputeAstarValidInput() {
        // Setup nodes
        Noeud noeud1 = mock(Noeud.class);
        Noeud noeud2 = mock(Noeud.class);
        Noeud noeud3 = mock(Noeud.class);

        when(noeud1.getId()).thenReturn(1L);
        when(noeud2.getId()).thenReturn(2L);
        when(noeud3.getId()).thenReturn(3L);
        when(noeud1.getLatitude()).thenReturn(0.0);
        when(noeud1.getLongitude()).thenReturn(0.0);
        when(noeud2.getLatitude()).thenReturn(1.0);
        when(noeud2.getLongitude()).thenReturn(1.0);
        when(noeud3.getLatitude()).thenReturn(2.0);
        when(noeud3.getLongitude()).thenReturn(2.0);

        // Setup troncons
        List<Troncon> troncons = new ArrayList<>();
        Troncon t1 = mock(Troncon.class);
        when(t1.getOrigineNoeud()).thenReturn(noeud1);
        when(t1.getDestinationNoeud()).thenReturn(noeud2);
        when(t1.getLongueur()).thenReturn(10.0);
        troncons.add(t1);

        when(plan.getVraiTroncons()).thenReturn(troncons);
        when(plan.getNoeud(1L)).thenReturn(noeud1);
        when(model.getEntrepot()).thenReturn(null);
        when(demandeDelivery.getDeliveries()).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> calculator.computeAstar(plan, demandeDelivery, model));
    }

    @Test
    @DisplayName("computeAstar: should handle null model")
    void testComputeAstarNullModel() {
        when(plan.getVraiTroncons()).thenReturn(new ArrayList<>());
        assertThrows(NullPointerException.class, () -> calculator.computeAstar(plan, demandeDelivery, null));
    }

    @Test
    @DisplayName("computeAstar: should handle null plan")
    void testComputeAstarNullPlan() {
        assertThrows(NullPointerException.class, () -> calculator.computeAstar(null, demandeDelivery, model));
    }

    @Test
    @DisplayName("computeAstar: should handle null demandeDelivery")
    void testComputeAstarNullDemandeDelivery() {
        when(plan.getVraiTroncons()).thenReturn(new ArrayList<>());
        when(model.getEntrepot()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> calculator.computeAstar(plan, null, model));
    }

    @Test
    @DisplayName("computeAstar: should deduplicate nodes")
    void testComputeAstarDeduplication() {
        Noeud shared = mock(Noeud.class);
        when(shared.getId()).thenReturn(1L);
        when(shared.getLatitude()).thenReturn(0.0);
        when(shared.getLongitude()).thenReturn(0.0);

        when(plan.getVraiTroncons()).thenReturn(new ArrayList<>());
        when(model.getEntrepot()).thenReturn(mock(Entrepot.class));
        when(model.getEntrepot().getAdresse()).thenReturn(1L);
        when(plan.getNoeud(1L)).thenReturn(shared);

        Delivery d1 = mock(Delivery.class);
        when(d1.getAdresseEnlevement()).thenReturn(1L);
        when(d1.getAdresseLivraison()).thenReturn(1L);

        List<Delivery> deliveries = new ArrayList<>();
        deliveries.add(d1);
        when(demandeDelivery.getDeliveries()).thenReturn(deliveries);

        assertDoesNotThrow(() -> calculator.computeAstar(plan, demandeDelivery, model));
    }

    @Test
    @DisplayName("computeAstar: should handle missing nodes in plan")
    void testComputeAstarMissingNodes() {
        when(plan.getVraiTroncons()).thenReturn(new ArrayList<>());
        when(model.getEntrepot()).thenReturn(mock(Entrepot.class));
        when(model.getEntrepot().getAdresse()).thenReturn(999L);
        when(plan.getNoeud(999L)).thenReturn(null);

        Delivery d1 = mock(Delivery.class);
        when(d1.getAdresseEnlevement()).thenReturn(1L);
        when(d1.getAdresseLivraison()).thenReturn(2L);
        when(plan.getNoeud(1L)).thenReturn(null);
        when(plan.getNoeud(2L)).thenReturn(null);

        when(demandeDelivery.getDeliveries()).thenReturn(List.of(d1));

        assertDoesNotThrow(() -> calculator.computeAstar(plan, demandeDelivery, model));
    }

    // ========== CONSTRUCTOR TEST ==========

    @Test
    @DisplayName("constructor: should create instance without errors")
    void testConstructor() {
        assertNotNull(new CalculPlusCoursChemins());
    }


    @Test
    @DisplayName("computeAstar: should calculate correct paths between nodes")
    void testComputeAstarCorrectPaths() {
        // Setup nodes with real coordinates
        Noeud depot = mock(Noeud.class);
        Noeud pickup = mock(Noeud.class);
        Noeud delivery = mock(Noeud.class);

        when(depot.getId()).thenReturn(1L);
        when(pickup.getId()).thenReturn(2L);
        when(delivery.getId()).thenReturn(3L);

        when(depot.getLatitude()).thenReturn(0.0);
        when(depot.getLongitude()).thenReturn(0.0);
        when(pickup.getLatitude()).thenReturn(1.0);
        when(pickup.getLongitude()).thenReturn(0.0);
        when(delivery.getLatitude()).thenReturn(2.0);
        when(delivery.getLongitude()).thenReturn(0.0);

        // Setup troncons: depot -> pickup -> delivery
        List<Troncon> troncons = new ArrayList<>();
        Troncon t1 = mock(Troncon.class);
        Troncon t2 = mock(Troncon.class);

        when(t1.getOrigineNoeud()).thenReturn(depot);
        when(t1.getDestinationNoeud()).thenReturn(pickup);
        when(t1.getLongueur()).thenReturn(5.0);

        when(t2.getOrigineNoeud()).thenReturn(pickup);
        when(t2.getDestinationNoeud()).thenReturn(delivery);
        when(t2.getLongueur()).thenReturn(5.0);

        troncons.add(t1);
        troncons.add(t2);

        when(plan.getVraiTroncons()).thenReturn(troncons);
        when(plan.getNoeud(1L)).thenReturn(depot);
        when(plan.getNoeud(2L)).thenReturn(pickup);
        when(plan.getNoeud(3L)).thenReturn(delivery);

        // Setup model with depot
        Entrepot entrepot = mock(Entrepot.class);
        when(entrepot.getAdresse()).thenReturn(1L);
        when(model.getEntrepot()).thenReturn(entrepot);

        // Setup delivery: pickup -> delivery
        Delivery d1 = mock(Delivery.class);
        when(d1.getAdresseEnlevement()).thenReturn(2L);
        when(d1.getAdresseLivraison()).thenReturn(3L);
        when(demandeDelivery.getDeliveries()).thenReturn(List.of(d1));

        calculator.computeAstar(plan, demandeDelivery, model);

        // Verify vertex order: depot, pickup, delivery
        verify(model).setVertexOrder(List.of(1L, 2L, 3L));

        // Verify paths matrix content
        ArgumentCaptor<MatriceChemins> pathCaptor = ArgumentCaptor.forClass(MatriceChemins.class);
        verify(model).setMatriceChemins(pathCaptor.capture());
        MatriceChemins chemins = pathCaptor.getValue();
        assertNotNull(chemins);

        // Verify specific paths
        NodePair depot_pickup = new NodePair(depot, pickup);
        NodePair pickup_delivery = new NodePair(pickup, delivery);

        List<Noeud> path1 = chemins.get(depot_pickup);
        List<Noeud> path2 = chemins.get(pickup_delivery);

        // Vérifie que depot -> pickup passe par depot et pickup
        assertNotNull(path1);
        assertEquals(2, path1.size());
        assertEquals(depot, path1.get(0));
        assertEquals(pickup, path1.get(1));

        // Vérifie que pickup -> delivery passe par pickup et delivery
        assertNotNull(path2);
        assertEquals(2, path2.size());
        assertEquals(pickup, path2.get(0));
        assertEquals(delivery, path2.get(1));
    }


    @Test
    @DisplayName("computeAstar: complex graph with 10 nodes and 5 troncons fully connected")
    void testComputeAstarTenNodesFullyConnected() {
        // Setup 10 nodes in a 2x5 grid layout
        Noeud n1 = mock(Noeud.class);
        Noeud n2 = mock(Noeud.class);
        Noeud n3 = mock(Noeud.class);
        Noeud n4 = mock(Noeud.class);
        Noeud n5 = mock(Noeud.class);
        Noeud n6 = mock(Noeud.class);
        Noeud n7 = mock(Noeud.class);
        Noeud n8 = mock(Noeud.class);
        Noeud n9 = mock(Noeud.class);
        Noeud n10 = mock(Noeud.class);

        // Set IDs
        when(n1.getId()).thenReturn(1L);
        when(n2.getId()).thenReturn(2L);
        when(n3.getId()).thenReturn(3L);
        when(n4.getId()).thenReturn(4L);
        when(n5.getId()).thenReturn(5L);
        when(n6.getId()).thenReturn(6L);
        when(n7.getId()).thenReturn(7L);
        when(n8.getId()).thenReturn(8L);
        when(n9.getId()).thenReturn(9L);
        when(n10.getId()).thenReturn(10L);

        // Set coordinates in 2x5 grid
        when(n1.getLatitude()).thenReturn(0.0);
        when(n1.getLongitude()).thenReturn(0.0);
        when(n2.getLatitude()).thenReturn(0.0);
        when(n2.getLongitude()).thenReturn(1.0);
        when(n3.getLatitude()).thenReturn(0.0);
        when(n3.getLongitude()).thenReturn(2.0);
        when(n4.getLatitude()).thenReturn(0.0);
        when(n4.getLongitude()).thenReturn(3.0);
        when(n5.getLatitude()).thenReturn(0.0);
        when(n5.getLongitude()).thenReturn(4.0);
        when(n6.getLatitude()).thenReturn(1.0);
        when(n6.getLongitude()).thenReturn(0.0);
        when(n7.getLatitude()).thenReturn(1.0);
        when(n7.getLongitude()).thenReturn(1.0);
        when(n8.getLatitude()).thenReturn(1.0);
        when(n8.getLongitude()).thenReturn(2.0);
        when(n9.getLatitude()).thenReturn(1.0);
        when(n9.getLongitude()).thenReturn(3.0);
        when(n10.getLatitude()).thenReturn(1.0);
        when(n10.getLongitude()).thenReturn(4.0);

        // Setup 5 troncons creating a connected chain: N1→N2→N3→N4→N5→N6
        List<Troncon> troncons = new ArrayList<>();

        Troncon t1 = mock(Troncon.class);
        when(t1.getOrigineNoeud()).thenReturn(n1);
        when(t1.getDestinationNoeud()).thenReturn(n2);
        when(t1.getLongueur()).thenReturn(1.0);
        troncons.add(t1);

        Troncon t2 = mock(Troncon.class);
        when(t2.getOrigineNoeud()).thenReturn(n2);
        when(t2.getDestinationNoeud()).thenReturn(n3);
        when(t2.getLongueur()).thenReturn(1.0);
        troncons.add(t2);

        Troncon t3 = mock(Troncon.class);
        when(t3.getOrigineNoeud()).thenReturn(n3);
        when(t3.getDestinationNoeud()).thenReturn(n4);
        when(t3.getLongueur()).thenReturn(1.0);
        troncons.add(t3);

        Troncon t4 = mock(Troncon.class);
        when(t4.getOrigineNoeud()).thenReturn(n4);
        when(t4.getDestinationNoeud()).thenReturn(n5);
        when(t4.getLongueur()).thenReturn(1.0);
        troncons.add(t4);

        Troncon t5 = mock(Troncon.class);
        when(t5.getOrigineNoeud()).thenReturn(n5);
        when(t5.getDestinationNoeud()).thenReturn(n6);
        when(t5.getLongueur()).thenReturn(1.0);
        troncons.add(t5);

        when(plan.getVraiTroncons()).thenReturn(troncons);
        when(plan.getNoeud(1L)).thenReturn(n1);
        when(plan.getNoeud(2L)).thenReturn(n2);
        when(plan.getNoeud(3L)).thenReturn(n3);
        when(plan.getNoeud(4L)).thenReturn(n4);
        when(plan.getNoeud(5L)).thenReturn(n5);
        when(plan.getNoeud(6L)).thenReturn(n6);
        when(plan.getNoeud(7L)).thenReturn(n7);
        when(plan.getNoeud(8L)).thenReturn(n8);
        when(plan.getNoeud(9L)).thenReturn(n9);
        when(plan.getNoeud(10L)).thenReturn(n10);

        // Setup model with depot at N1
        Entrepot entrepot = mock(Entrepot.class);
        when(entrepot.getAdresse()).thenReturn(1L);
        when(model.getEntrepot()).thenReturn(entrepot);

// Setup deliveries: N3→N5, N4→N6
        Delivery d1 = mock(Delivery.class);
        when(d1.getAdresseEnlevement()).thenReturn(3L);
        when(d1.getAdresseLivraison()).thenReturn(5L);

        Delivery d2 = mock(Delivery.class);
        when(d2.getAdresseEnlevement()).thenReturn(4L);
        when(d2.getAdresseLivraison()).thenReturn(6L);

        when(demandeDelivery.getDeliveries()).thenReturn(List.of(d1, d2));

        calculator.computeAstar(plan, demandeDelivery, model);

// Verify vertex order: depot, pickup1, delivery1, pickup2, delivery2
        verify(model).setVertexOrder(List.of(1L, 3L, 5L, 4L, 6L));

        // Verify paths matrix is set
        ArgumentCaptor<MatriceChemins> pathCaptor = ArgumentCaptor.forClass(MatriceChemins.class);
        verify(model).setMatriceChemins(pathCaptor.capture());
        MatriceChemins chemins = pathCaptor.getValue();
        assertNotNull(chemins);

        // Verify path N1→N3 (should be N1→N2→N3)
        NodePair n1_n3 = new NodePair(n1, n3);
        List<Noeud> path1 = chemins.get(n1_n3);
        assertNotNull(path1);
        assertEquals(3, path1.size());
        assertEquals(n1, path1.get(0));
        assertEquals(n2, path1.get(1));
        assertEquals(n3, path1.get(2));

        // Verify path N3→N5 (should be N3→N4→N5)
        NodePair n3_n5 = new NodePair(n3, n5);
        List<Noeud> path2 = chemins.get(n3_n5);
        assertNotNull(path2);
        assertEquals(3, path2.size());
    }





}
