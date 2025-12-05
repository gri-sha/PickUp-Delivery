package com.agile.projet.model;

import java.util.ArrayList;
import java.util.List;

public class DemandeDelivery {
    private List<Delivery> deliveries = new ArrayList<>();

    DemandeDelivery(){}

    public void addDelivery(Delivery d) {
        if (d != null) {
            deliveries.add(d);
        }
    }

    public void printDeliveries() {
        if (deliveries.isEmpty()) {
            System.out.println("Aucune delivery dans la demande.");
            return;
        }

        System.out.println("Liste des deliveries :");
        for (Delivery d : deliveries) {
            System.out.println(d);
        }
    }

    /** Getter si tu veux accéder à la liste ailleurs */
    public List<Delivery> getDeliveries() {
        return deliveries;
    }
    public Entrepot entrepot;
    public void setEntrepot(Entrepot entrepot) {
        this.entrepot = entrepot;
    }
    public Entrepot getEntrepot() {
        return entrepot;
    }


}
