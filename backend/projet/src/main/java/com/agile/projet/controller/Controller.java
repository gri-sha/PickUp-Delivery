package com.agile.projet.controller;

import com.agile.projet.model.PickupDeliveryModel;
import com.agile.projet.model.Tournee;
import com.agile.projet.utils.CalculPlusCoursChemins;
import com.agile.projet.utils.CalculTSP;
import com.agile.projet.utils.MatriceCout;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class Controller {
    private final PickupDeliveryModel pickupDeliveryModel = new PickupDeliveryModel();

    public Controller() throws Exception { }

    public void createPlan(String planXml) {
        pickupDeliveryModel.createPlan(planXml);
        pickupDeliveryModel.plan.printNoeuds();
        pickupDeliveryModel.plan.printTroncons();
    }

    public void computeShortestPaths() {
        if (pickupDeliveryModel.plan == null) {
            throw new IllegalStateException("Plan manquant : appelez createPlan(...) d'abord.");
        }
        if (pickupDeliveryModel.demandeDelivery == null) {
            throw new IllegalStateException("DemandeDelivery manquante : appelez createDeliveryFromXml(...) d'abord.");
        }

        CalculPlusCoursChemins calculPlusCoursChemins = new CalculPlusCoursChemins();
        // Cette méthode doit remplir : model.setVertexOrder(...); model.setMatriceCout(...);
        calculPlusCoursChemins.computeAstar(
                pickupDeliveryModel.plan,
                pickupDeliveryModel.demandeDelivery,
                pickupDeliveryModel
        );
    }

    public void createDeliveryFromXml(String deliveryPlanXml) throws Exception {
        pickupDeliveryModel.createDelivery(deliveryPlanXml);
        pickupDeliveryModel.demandeDelivery.printDeliveries();
    }

    public Tournee findBestPath() {
        var entrepot = pickupDeliveryModel.getEntrepot();
        if (entrepot == null) throw new IllegalStateException("Entrepôt manquant dans le modèle.");

        MatriceCout mc = pickupDeliveryModel.getMatriceCout();
        if (mc == null) {
            throw new IllegalStateException("matriceCout manquante : appelez computeShortestPaths() d'abord.");
        }
        double[][] costMatrix = mc.getCostMatrix();

        List<Long> vertexOrder = pickupDeliveryModel.getVertexOrder();
        if (vertexOrder == null) throw new IllegalStateException("vertexOrder manquant dans le modèle.");
        if (vertexOrder.size() != costMatrix.length)
            throw new IllegalStateException("vertexOrder.size != costMatrix.length");

        long depotId = entrepot.getAdresse();
        if (!vertexOrder.contains(depotId)) {
            throw new IllegalStateException("L'ID de l'entrepôt (" + depotId + ") n'est pas présent dans vertexOrder. " +
                    "Assure-toi que computeAstar ajoute l'entrepôt aux points d'intérêt.");
        }

        CalculTSP tsp = new CalculTSP(costMatrix, vertexOrder);
        tsp.solveFromId(depotId);

        double bestCost = tsp.getBestCost();
        List<Integer> bestPathIds = tsp.getBestPathIndices();

        List<Integer> closed = new java.util.ArrayList<>(bestPathIds);
        if (!closed.isEmpty()) closed.add(closed.get(0));

        // construire les étapes (type/label + coût par tronçon et cumul)
        java.util.List<Tournee.Etape> etapes = new java.util.ArrayList<>();
        double cumul = 0.0;

        for (int k = 0; k < closed.size(); k++) {
            int idx = closed.get(k);
            long id = vertexOrder.get(idx);

            String type = resolveType(id);
            String label = buildLabel(type, id);

            double leg = 0.0;
            if (k > 0) {
                int prevIdx = closed.get(k - 1);
                leg = costMatrix[prevIdx][idx];
                cumul += leg;
            }
            etapes.add(new Tournee.Etape(id, type, label, leg, cumul));
        }

        return new Tournee(bestCost, etapes);
    }

    private String resolveType(long id) {
        if (pickupDeliveryModel.getEntrepot() != null
                && pickupDeliveryModel.getEntrepot().getAdresse() == id) {
            return "DEPOT";
        }
        for (var d : pickupDeliveryModel.demandeDelivery.getDeliveries()) {
            if (d.getAdresseEnlevement() == id) return "PICKUP";
            if (d.getAdresseLivraison() == id) return "DELIVERY";
        }
        return "UNKNOWN";
    }

    private String buildLabel(String type, long id) {
        return switch (type) {
            case "DEPOT" -> "Dépôt";
            case "PICKUP" -> "Enlèvement " + id;
            case "DELIVERY" -> "Livraison " + id;
            default -> "Noeud " + id;
        };

    }
}
