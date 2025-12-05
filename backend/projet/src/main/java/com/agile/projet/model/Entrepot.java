package com.agile.projet.model;

public class Entrepot {
    private Long adresse;
    private String heureDepart; // tu peux remplacer par LocalTime si besoin

    public Entrepot(Long adresse, String heureDepart) {
        this.adresse = adresse;
        this.heureDepart = heureDepart;
    }

    public Long getAdresse() {
        return adresse;
    }

    public void setAdresse(Long adresse) {
        this.adresse = adresse;
    }

    public String getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(String heureDepart) {
        this.heureDepart = heureDepart;
    }

    @Override
    public String toString() {
        return "Entrepot{adresse=" + adresse + ", heureDepart='" + heureDepart + "'}";
    }
}
