package com.agile.projet.model;

import com.agile.projet.utils.XmlDeliveryParser;
import com.agile.projet.utils.XmlPlanParser;

import java.util.List;

public class PickupDeliveryModel {
    public Plan plan;
    public DemandeDelivery demandeDelivery= new DemandeDelivery();

    public void createPlan(String planXml){
        plan = new Plan();
        XmlPlanParser xmlPlanParser = new XmlPlanParser();
        xmlPlanParser.parsePlan(planXml,plan);
        plan.joinNoeudTroncons();
        plan.printTroncons();
    }
    private Entrepot entrepot;

    public Entrepot getEntrepot() {
        return entrepot;
    }

    public void setEntrepot(Entrepot entrepot) {
        this.entrepot = entrepot;
    }
    public Plan getPlan()
    {
        return plan;
    }
    public DemandeDelivery getDemandeDelivery()
    {
        return demandeDelivery;
    }
    public void createDelivery(String deliveryXml) throws Exception {
        XmlDeliveryParser xmlDeliveryParser = new XmlDeliveryParser();
        xmlDeliveryParser.parse(deliveryXml,demandeDelivery);
        Entrepot entrepot1= demandeDelivery.getEntrepot();
        this.setEntrepot(entrepot1);
    }
    // PickupDeliveryModel.java
    /*TvertexOrder, c’est la liste des IDs des sommets dans le même ordre que les lignes/colonnes de ta matrice de coûts.

À quoi ça sert concrètement ?

Traduire indices ⇄ IDs

La matrice cost[i][j] ne connaît que des indices 0..n-1.

Ton domaine (XML, entrepôt, livraisons) manipule des IDs (Long).

vertexOrder.get(i) te dit quel ID correspond à l’index i (et inversement tu peux faire indexOf(id) pour retrouver l’index).

Démarrer au bon sommet

Tu connais l’ID de l’entrepôt (entrepot.getAdresse()).

Tu retrouves son index via vertexOrder.indexOf(depotId) (ou via une map ID→index).

Le solver TSP démarre en indices, donc il a besoin de cette conversion. */

    private List<Long> vertexOrder;

    public List<Long> getVertexOrder() { return vertexOrder; }
    public void setVertexOrder(List<Long> vertexOrder) { this.vertexOrder = vertexOrder; }


}
