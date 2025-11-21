package com.agile.projet.model;


public class Troncon {

    private Noeud origine;
    private Noeud destination;
    private double longueur;
    private String nomRue;

    private Long origine1;
    private Long destination1;

    public Troncon(Noeud origine, Noeud destination, double longueur, String nomRue) {
        this.origine = origine;
        this.destination = destination;
        this.longueur = longueur;
        this.nomRue = nomRue;
    }

    public Troncon(Long origine, Long destination, double longueur, String nomRue) {
        this.origine1 = origine;
        this.destination1 = destination;
        this.longueur = longueur;
        this.nomRue = nomRue;
    }

    public Noeud getOrigine() {
        return origine;
    }
    public Long getOrigine1() {
        return origine1;
    }

    public Noeud getDestination() {
        return destination;
    }

    public Long getDestination1() {
        return destination1;
    }

    public double getLongueur() {
        return longueur;
    }

    public String getNomRue() {
        return nomRue;
    }
}