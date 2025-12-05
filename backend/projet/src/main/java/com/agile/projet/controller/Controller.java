package com.agile.projet.controller;

import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Entrepot;
import com.agile.projet.model.PickupDeliveryModel;
import com.agile.projet.utils.CalculPlusCoursChemins;
import com.agile.projet.model.Plan;
import com.agile.projet.utils.CalculPlusCoursChemins;
import com.agile.projet.utils.CalculTSP;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Controller {
    private PickupDeliveryModel pickupDeliveryModel = new PickupDeliveryModel();

    public Controller() throws Exception {

    }

    public void createPlan(String planXml)
    {
        pickupDeliveryModel.createPlan(planXml);
        pickupDeliveryModel.plan.printNoeuds();
        pickupDeliveryModel.plan.printTroncons();


    }


    public void computeShortestPaths(){
        CalculPlusCoursChemins calculPlusCoursChemins = new CalculPlusCoursChemins();
        //calculPlusCoursChemins.compute(pickupDeliveryModel.plan, pickupDeliveryModel.demandeDelivery);
        calculPlusCoursChemins.computeAstar(pickupDeliveryModel.plan, pickupDeliveryModel.demandeDelivery);
    }
    public void createDeliveryFromXml(String deliveryPlanXml) throws Exception {
        pickupDeliveryModel.createDelivery(deliveryPlanXml);
        pickupDeliveryModel.demandeDelivery.printDeliveries();
    }


    public BestPathResult findBestPath(double[][] costMatrix) {
        if (costMatrix == null) throw new IllegalArgumentException("costMatrix null");

        var entrepot = pickupDeliveryModel.getEntrepot();
        if (entrepot == null) throw new IllegalStateException("Entrepôt manquant dans le modèle");

        List<Long> vertexOrder = pickupDeliveryModel.getVertexOrder();
        if (vertexOrder == null) throw new IllegalStateException("vertexOrder manquant dans le modèle");
        if (vertexOrder.size() != costMatrix.length)
            throw new IllegalStateException("vertexOrder.size != costMatrix.length");

        // Le solver construit sa map ID->index depuis vertexOrder
        CalculTSP tsp = new CalculTSP(costMatrix, vertexOrder);

        long depotId = entrepot.getAdresse();
        tsp.solveFromId(depotId);

        double bestCost = tsp.getBestCost();
        List<Long> bestPathIds = tsp.getBestPathIds();

        return new BestPathResult(bestCost, bestPathIds);
    }

    public static class BestPathResult {
        private final double cost;
        private final List<Long> pathIds;

        public BestPathResult(double cost, List<Long> pathIds) {
            this.cost = cost;
            this.pathIds = pathIds;
        }
        public double getCost() { return cost; }
        public List<Long> getPathIds() { return pathIds; }
        @Override public String toString() {
            return "BestPathResult{cost=" + cost + ", pathIds=" + pathIds + "}";
        }
    }
}
