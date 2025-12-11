package com.agile.projet.utils;

import com.agile.projet.model.Noeud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatriceChemins {
    private final Map<NodePair, List<Noeud>> shortestPaths;
    public MatriceChemins() {
        this.shortestPaths = new HashMap<NodePair,List<Noeud>>();
    }
    public void put(NodePair nodePair, List<Noeud> noeuds){
        shortestPaths.put(nodePair, noeuds);
    }
    public Map<NodePair, List<Noeud>> getCheminMatrix() { return shortestPaths; }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MatriceChemins:\n");

        for (Map.Entry<NodePair, List<Noeud>> entry : shortestPaths.entrySet()) {
            NodePair pair = entry.getKey();
            List<Noeud> path = entry.getValue();

            sb.append("  De ")
                    .append(pair.getFrom().getId())
                    .append(" à ")
                    .append(pair.getTo().getId())
                    .append(" : ");

            if (path == null || path.isEmpty()) {
                sb.append("Aucun chemin\n");
            } else {
                // Liste les IDs des nœuds du chemin
                sb.append("[");
                for (int i = 0; i < path.size(); i++) {
                    sb.append(path.get(i).getId());
                    if (i < path.size() - 1) sb.append(" -> ");
                }
                sb.append("]\n");
            }
        }

        return sb.toString();
    }

    public List<Noeud> get(NodePair depotPickup) {
        return shortestPaths.get(depotPickup);
    }
}
