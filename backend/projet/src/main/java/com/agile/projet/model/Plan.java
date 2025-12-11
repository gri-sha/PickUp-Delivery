package com.agile.projet.model;

import java.util.*;

public class Plan {

    private Map<Long, Noeud> noeuds = new HashMap<>();
    private List<Troncon> troncons = new ArrayList<>();
    private List<Troncon> vraiTroncons = new ArrayList<>();

    public Plan() {
        this.noeuds = new HashMap<>();
        this.troncons = new ArrayList<>();
        this.vraiTroncons = new ArrayList<>();
    }

    public Plan(Map<Long, Noeud> noeuds, List<Troncon> troncons, List<Troncon> vraiTroncons) {
        this.noeuds = noeuds != null ? noeuds : new HashMap<>();
        this.troncons = troncons != null ? troncons : new ArrayList<>();
        this.vraiTroncons = vraiTroncons != null ? vraiTroncons : new ArrayList<>();
    }

    public void addNoeud(Noeud n) {
        noeuds.put(n.getId(), n);
    }
    public Noeud getNoeud(long id) {
        return noeuds.get(id);
    }

    public Collection<Noeud> getAllNoeuds() {
        return noeuds.values();
    }

    public void addTroncon(Troncon t) {
        troncons.add(t);
    }

    public List<Troncon> getTroncons() {
        return troncons;
    }
    public List<Troncon> getVraiTroncons() {
        return vraiTroncons;
    }

    public Noeud getNoeudById(Long id) {
        return noeuds.get(id);
    }

    public void joinNoeudTroncons(){
        for(Troncon t : troncons){
            Noeud noeudOrigine = this.getNoeudById(t.getOrigine());
            Noeud noeudDestination = this.getNoeudById(t.getDestination());

            System.out.println(noeudDestination);


            Troncon troncon = new Troncon(noeudOrigine, noeudDestination, t.getLongueur(), t.getNomRue());
            vraiTroncons.add(troncon);
        }

    }
    public void printTroncons() {
        for (Troncon t : vraiTroncons) {
            System.out.println(
                    "Tronçon : " +
                            "origine=" + t.getOrigineNoeud().getId() +
                            " (" + t.getOrigineNoeud().getLatitude() + ", " + t.getOrigineNoeud().getLongitude() + ")" +
                            ", destination=" + t.getDestinationNoeud().getId() +
                            " (" + t.getDestinationNoeud().getLatitude() + ", " + t.getDestinationNoeud().getLongitude() + ")" +
                            ", rue=" + t.getNomRue() +
                            ", longueur=" + t.getLongueur()
            );
        }
    }

    public void printNoeuds() {
        for (Noeud n : noeuds.values()) {
            System.out.println(
                    "Noeud " + n.getId() +
                            " | lat=" + n.getLatitude() +
                            " | lon=" + n.getLongitude()
            );
        }
    }
}
