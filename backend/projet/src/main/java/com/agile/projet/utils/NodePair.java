package com.agile.projet.utils;

import com.agile.projet.model.Noeud;

public class NodePair {
    private final Noeud from;
    private final Noeud to;

    public NodePair(Noeud from, Noeud to) {
        this.from = from;
        this.to = to;
    }

    public Noeud getFrom() { return from; }
    public Noeud getTo() { return to; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        NodePair other = (NodePair) obj;
        return from.getId() == other.from.getId() &&
                to.getId() == other.to.getId();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(from.getId()) * 31 + Long.hashCode(to.getId());
    }
}
