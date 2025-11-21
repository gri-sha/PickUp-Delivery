package com.agile.projet.model;

import java.util.*;

public class Plan {

    private Map<Long, Noeud> noeuds = new HashMap<>();
    private List<Troncon> troncons = new ArrayList<>();

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

    public void joinNoeudTroncons(){
        for(Troncon t : troncons){
            Noeud noeudOrigine;
            Noeud noeudDestination;
            int found = 0;
            for(Noeud n :noeuds.values()){
                if(t.getDestination1() == n.getId()){
                    noeudDestination = n;
                    found += 1;
                }
                if(t.getOrigine1() == n.getId()){
                    noeudOrigine = n;
                    found += 1;
                }
                if(found == 2){found = 0;break;}
            }
            Troncon troncon = new Troncon(noeudOrigine, noeudDestination,)
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
