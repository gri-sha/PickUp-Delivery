package com.agile.projet.utils;
import com.agile.projet.model.*;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.shortestpath.*;

import java.util.ArrayList;
import java.util.List;

public class CalculPlusCoursChemins {
    public CalculPlusCoursChemins() {


    }

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
            GraphPath<Long, DefaultWeightedEdge> path = fw.getPath(delivery.getAdresseEnlevement(), delivery.getAdresseLivraison());
            GraphPath<Long, DefaultWeightedEdge> path2 = fw.getPath(delivery.getAdresseLivraison(), delivery.getAdresseEnlevement());

            System.out.println("   chemin = " + (path != null ? path.getVertexList() : "aucun"));
            System.out.println("   chemin = " + (path2 != null ? path2.getVertexList() : "aucun"));
        }


    }

    public void computeAstar(Plan plan, DemandeDelivery demandeDelivery){
        Graph<Noeud, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        List<Troncon> troncons = plan.getVraiTroncons();
        for (Troncon troncon : troncons) {


            graph.addVertex(troncon.getDestinationNoeud());
            graph.addVertex(troncon.getOrigineNoeud());
            DefaultWeightedEdge e = graph.addEdge(troncon.getOrigineNoeud(), troncon.getDestinationNoeud());
            graph.setEdgeWeight(e, troncon.getLongueur());
        }
        //AStarAdmissibleHeuristic<Noeud> h = (v1, v2) -> calculDistanceEuclidienne(v1, v2);
        AStarAdmissibleHeuristic<Noeud> h = this::calculDistanceEuclidienne;

        AStarShortestPath<Noeud, DefaultWeightedEdge> astar =
                new AStarShortestPath<>(graph, h);

        List<Noeud> noeudsDelivery = new ArrayList<>();
        for(Delivery delivery : demandeDelivery.getDeliveries()){
            noeudsDelivery.add(plan.getNoeud(delivery.getAdresseEnlevement()));
            noeudsDelivery.add(plan.getNoeud(delivery.getAdresseLivraison()));
        }

        for(Noeud noeud : noeudsDelivery){
            for(Noeud noeud2 : noeudsDelivery){
                GraphPath<Noeud, DefaultWeightedEdge> path = astar.getPath(noeud, noeud2);
                double cost = path.getWeight();
                System.out.println(noeud +  "->" + noeud2 + " = " + cost);
                //System.out.println(astar.getPath(noeud, noeud2).toString());
            }
        }


    }

    private double calculDistanceEuclidienne(Noeud v1, Noeud v2){
        double dx = v1.getLatitude() - v2.getLatitude();
        double dy = v1.getLongitude() - v2.getLongitude();

        return Math.sqrt(dx*dx + dy*dy);

    }

}
