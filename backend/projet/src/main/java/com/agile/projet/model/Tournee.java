package com.agile.projet.model;

import java.util.List;
import java.util.Objects;

/**
 * Représente une tournée fermée (départ dépôt -> ... -> retour dépôt).
 * - totalCost : coût total du circuit (incluant le retour au dépôt)
 * - etapes    : séquence ordonnée des arrêts (avec type métier et coûts par tronçon)
 */
public class Tournee {

    private final double totalCost;
    private final List<Etape> etapes;

    public Tournee(double totalCost, List<Etape> etapes) {
        this.totalCost = totalCost;
        this.etapes = Objects.requireNonNull(etapes, "etapes");
    }

    public double getTotalCost() { return totalCost; }
    public List<Etape> getEtapes() { return etapes; }

    @Override
    public String toString() {
        return "Tournee{totalCost=" + totalCost + ", etapes=" + etapes + "}";
    }

    /** Une étape (arrêt) de la tournée. */
    public static class Etape {
        private final long id;              // ID (adresse/noeud)
        private final String type;          // "DEPOT" | "PICKUP" | "DELIVERY" | "UNKNOWN"
        private final String label;         // ex: "Dépôt", "Enlèvement 4150019167"
        private final double legCost;       // coût depuis l'étape précédente
        private final double cumulativeCost;// coût cumulé jusqu'à cette étape

        public Etape(long id, String type, String label, double legCost, double cumulativeCost) {
            this.id = id;
            this.type = type;
            this.label = label;
            this.legCost = legCost;
            this.cumulativeCost = cumulativeCost;
        }

        public long getId() { return id; }
        public String getType() { return type; }
        public String getLabel() { return label; }
        public double getLegCost() { return legCost; }
        public double getCumulativeCost() { return cumulativeCost; }

        @Override
        public String toString() {
            return "Etape{id=" + id + ", type='" + type + '\'' +
                    ", label='" + label + '\'' +
                    ", legCost=" + legCost +
                    ", cumulativeCost=" + cumulativeCost + '}';
        }
    }
}
