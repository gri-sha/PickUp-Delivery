package com.agile.projet.model;

public class Noeud {

    private long id;
    private double latitude;
    private double longitude;

    public Noeud(long id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public Noeud(Noeud other) {
        this.id = other.id;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
    }

    public long getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Noeud other = (Noeud) obj;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Noeud{" +
                "id=" + id +
                '}';
    }

    // Optionnel : toString()
}
