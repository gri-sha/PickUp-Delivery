
















package com.agile.projet.model;

public class Delivery {
    private Long adresseEnlevement;
    private Long adresseLivraison;

    private Long dureeEnlevement;
    private Long dureeLivraison;

    public Delivery() {

    }
    public Delivery(Long adresseEnlevement, Long adresseLivraison, Long dureeEnlevement, Long dureeLivraison) {
        this.adresseEnlevement = adresseEnlevement;
        this.adresseLivraison = adresseLivraison;
        this.dureeEnlevement = dureeEnlevement;
        this.dureeLivraison = dureeLivraison;
    }

    public Long getAdresseEnlevement() {
        return adresseEnlevement;
    }
    public void setAdresseEnlevement(Long adresseEnlevement) {this.adresseEnlevement = adresseEnlevement;}


    public Long getAdresseLivraison() {
        return adresseLivraison;
    }
    public void setAdresseLivraison(Long adresseLivraison) {this.adresseLivraison = adresseLivraison;}

}
