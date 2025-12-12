package com.agile.projet.model;

import com.agile.projet.utils.MatriceChemins;
import com.agile.projet.utils.MatriceCout;
import com.agile.projet.utils.XmlDeliveryParser;
import com.agile.projet.utils.XmlPlanParser;

import java.util.List;
import java.util.Map;

public class PickupDeliveryModel {
    public Plan plan;
    public DemandeDelivery demandeDelivery= new DemandeDelivery();

    public void createPlan(String planXml){
        plan = new Plan();
        XmlPlanParser xmlPlanParser = new XmlPlanParser();
        xmlPlanParser.parsePlan(planXml,plan);
        plan.joinNoeudTroncons();

    }

    private Entrepot entrepot;
    private MatriceCout matriceCout;
    private MatriceChemins matriceChemins;

    public void setMatriceChemins(MatriceChemins matriceChemins) {
        this.matriceChemins = matriceChemins;
    }
    public MatriceChemins getMatriceChemins() {
        return matriceChemins;
    }
    public MatriceCout getMatriceCout() {
        return matriceCout;
    }

    public void setMatriceCout(MatriceCout matriceCout) {
        this.matriceCout = matriceCout;
    }

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
        // Reset demandeDelivery to prevent accumulation
        demandeDelivery = new DemandeDelivery();

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

Le domaine (XML, entrepôt, livraisons) manipule des IDs (Long).

vertexOrder.get(i) te dit quel ID correspond à l’index i (et inversement tu peux faire indexOf(id) pour retrouver l’index).

Démarrer au bon sommet

Tu connais l’ID de l’entrepôt (entrepot.getAdresse()).

Tu retrouves son index via vertexOrder.indexOf(depotId) (ou via une map ID→index).

Le solver TSP démarre en indices, donc il a besoin de cette conversion. */

    private List<Long> vertexOrder;

    public List<Long> getVertexOrder() { return vertexOrder; }
    public void setVertexOrder(List<Long> vertexOrder) { this.vertexOrder = vertexOrder; }
    // PickupDeliveryModel.java
    private Map<Long, Long> pickupToDelivery;   // idPickup -> idDelivery
    private Map<Long, Long> deliveryToPickup;   // idDelivery -> idPickup
    private int[] pickupOfDelivery;             // indexDelivery -> indexPickup (sinon -1)

    public Map<Long, Long> getPickupToDelivery() { return pickupToDelivery; }
    public void setPickupToDelivery(Map<Long, Long> m) { this.pickupToDelivery = m; }

    public Map<Long, Long> getDeliveryToPickup() { return deliveryToPickup; }
    public void setDeliveryToPickup(Map<Long, Long> m) { this.deliveryToPickup = m; }

    public int[] getPickupOfDelivery() { return pickupOfDelivery; }
    public void setPickupOfDelivery(int[] arr) { this.pickupOfDelivery = arr; }

    // Helpers pratiques
    public boolean isPickup(long id)   { return pickupToDelivery != null && pickupToDelivery.containsKey(id); }
    public boolean isDelivery(long id) { return deliveryToPickup != null && deliveryToPickup.containsKey(id); }
    public Long counterpart(long id) {
        if (isPickup(id))   return pickupToDelivery.get(id);
        if (isDelivery(id)) return deliveryToPickup.get(id);
        return null;
    }


}
