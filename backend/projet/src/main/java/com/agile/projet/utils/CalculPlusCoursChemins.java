package com.agile.projet.utils;

import com.agile.projet.model.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CalculPlusCoursChemins {

    public CalculPlusCoursChemins() { }

    public void compute(Plan plan, DemandeDelivery demandeDelivery){
        Graph<Long, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        List<Troncon> troncons = plan.getTroncons();
        for (Troncon troncon : troncons) {
            graph.addVertex(troncon.getDestination());
            graph.addVertex(troncon.getOrigine());
            DefaultWeightedEdge e = graph.addEdge(troncon.getDestination(), troncon.getOrigine());
            graph.setEdgeWeight(e, 1);
        }
        FloydWarshallShortestPaths<Long, DefaultWeightedEdge> fw =
                new FloydWarshallShortestPaths<>(graph);

        for(Delivery delivery : demandeDelivery.getDeliveries()){
            GraphPath<Long, DefaultWeightedEdge> path  = fw.getPath(delivery.getAdresseEnlevement(), delivery.getAdresseLivraison());
            GraphPath<Long, DefaultWeightedEdge> path2 = fw.getPath(delivery.getAdresseLivraison(),  delivery.getAdresseEnlevement());

            System.out.println("   chemin = " + (path  != null ? path.getVertexList()  : "aucun"));
            System.out.println("   chemin = " + (path2 != null ? path2.getVertexList() : "aucun"));
        }
    }

    public void computeAstar(Plan plan, DemandeDelivery demandeDelivery, PickupDeliveryModel model){
        Graph<Noeud, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        MatriceChemins matriceChemins = new MatriceChemins();

        // 1) Graphe réel (poids = longueur)
        List<Troncon> troncons = plan.getVraiTroncons();
        for (Troncon troncon : troncons) {
            Noeud org = troncon.getOrigineNoeud();
            Noeud dst = troncon.getDestinationNoeud();
            graph.addVertex(org);
            graph.addVertex(dst);
            DefaultWeightedEdge e = graph.addEdge(org, dst);
            if (e != null) {
                graph.setEdgeWeight(e, troncon.getLongueur());
            }
        }

        // 2) A* avec heuristique euclidienne
        AStarAdmissibleHeuristic<Noeud> h = this::calculDistanceEuclidienne;
        AStarShortestPath<Noeud, DefaultWeightedEdge> astar = new AStarShortestPath<>(graph, h);

        // 3) Points d’intérêt : ENTREPÔT d’abord (depuis le modèle), puis enlèvements + livraisons
        List<Noeud> poi = new ArrayList<>();

        // Entrepôt depuis PickupDeliveryModel (déjà défini par XmlDeliveryParser + createDelivery)
        if (model.getEntrepot() != null) {
            Noeud depot = plan.getNoeud(model.getEntrepot().getAdresse());
            if (depot != null) poi.add(depot);
        }

        for (Delivery d : demandeDelivery.getDeliveries()) {
            Noeud enl = plan.getNoeud(d.getAdresseEnlevement());
            Noeud liv = plan.getNoeud(d.getAdresseLivraison());
            if (enl != null) poi.add(enl);
            if (liv != null) poi.add(liv);
        }

        // Déduplication en gardant l'ordre (LinkedHashSet)
        LinkedHashSet<Noeud> uniques = new LinkedHashSet<>(poi);
        List<Noeud> points = new ArrayList<>(uniques);

        // 4) Build vertexOrder (index -> ID Long) et matrice des coûts
        int n = points.size();
        double[][] costMatrix = new double[n][n];

        List<Long> vertexOrder = new ArrayList<>(n);
        for (Noeud nd : points) {
            // ⚠️ si ton identifiant s’appelle autrement, remplace nd.getId() par nd.getAdresse() ou équivalent
            vertexOrder.add(nd.getId());
        }

        for (int i = 0; i < n; i++) {
            Noeud from = points.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    costMatrix[i][j] = 0.0;
                    continue;
                }
                Noeud to = points.get(j);
                GraphPath<Noeud, DefaultWeightedEdge> path = astar.getPath(from, to);
                costMatrix[i][j] = (path == null) ? Double.POSITIVE_INFINITY : path.getWeight();
                double cost = path.getWeight();
                NodePair pair = new NodePair(from, to);

                matriceChemins.put(pair,path.getVertexList());
                System.out.println(from +  "->" + to + " = " + cost);
            }
        }
        java.util.Map<Long, Long> p2d = new java.util.HashMap<>();
        java.util.Map<Long, Long> d2p = new java.util.HashMap<>();
        for (Delivery d : demandeDelivery.getDeliveries()) {
            p2d.put(d.getAdresseEnlevement(), d.getAdresseLivraison());
            d2p.put(d.getAdresseLivraison(), d.getAdresseEnlevement());
        }
        model.setPickupToDelivery(p2d);
        model.setDeliveryToPickup(d2p);

        // ---- Construire pickupOfDelivery en INDICES (indexDelivery -> indexPickup) ----
        int[] pickupOfDelivery = new int[n];
        java.util.Arrays.fill(pickupOfDelivery, -1);

        // ID -> index
        java.util.Map<Long, Integer> idToIdx = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) idToIdx.put(vertexOrder.get(i), i);

        // Pour chaque paire (pickupID -> deliveryID), poser la contrainte en indices
        for (var e : p2d.entrySet()) {
            Long idP = e.getKey();
            Long idD = e.getValue();
            Integer iP = idToIdx.get(idP);
            Integer iD = idToIdx.get(idD);
            if (iP != null && iD != null) {
                pickupOfDelivery[iD] = iP; // la livraison à iD exige d’avoir visité iP
            }
        }
        // 5) Stockage dans le modèle (vertexOrder vit dans le modèle, MatriceCout = matrice seule)
        model.setVertexOrder(vertexOrder);
        model.setMatriceCout(new MatriceCout(costMatrix));
        model.setPickupOfDelivery(pickupOfDelivery);
        model.setMatriceChemins(matriceChemins);
    }


    private double calculDistanceEuclidienne(Noeud v1, Noeud v2){
        double dx = v1.getLatitude() - v2.getLatitude();
        double dy = v1.getLongitude() - v2.getLongitude();
        return Math.sqrt(dx*dx + dy*dy);
    }
}
