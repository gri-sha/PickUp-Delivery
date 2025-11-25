package com.agile.projet.utils;
import com.agile.projet.model.Delivery;
import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Plan;
import com.agile.projet.model.Troncon;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

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

}
