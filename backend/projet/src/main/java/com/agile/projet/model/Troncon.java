package com.agile.projet.model;


public class Troncon {

    private Noeud origineNoeud;
    private Noeud destinationNoeud;
    private double longueur;
    private String nomRue;

    private Long origine;
    private Long destination;

    public Troncon(Noeud origine, Noeud destination, double longueur, String nomRue) {
        this.origineNoeud = origine;
        this.destinationNoeud = destination;
        this.longueur = longueur;
        this.nomRue = nomRue;
    }

    public Troncon(Long origine, Long destination, double longueur, String nomRue) {
        this.origine = origine;
        this.destination = destination;
        this.longueur = longueur;
        this.nomRue = nomRue;
    }

    public Noeud getOrigineNoeud() {
        return origineNoeud;
    }
    public Long getOrigine() {
        return origine;
    }

    public Noeud getDestinationNoeud() {
        return destinationNoeud;
    }

    public Long getDestination() {
        return destination;
    }

    public double getLongueur() {
        return longueur;
    }

    public String getNomRue() {
        return nomRue;
    }
}