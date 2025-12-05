package com.agile.projet.controller;

import com.agile.projet.model.PickupDeliveryModel;
import com.agile.projet.utils.CalculPlusCoursChemins;
import org.springframework.stereotype.Component;

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

    public void createDeliveryFromXml(String deliveryPlanXml) throws Exception {
        pickupDeliveryModel.createDelivery(deliveryPlanXml);
        pickupDeliveryModel.demandeDelivery.printDeliveries();
    }

    public void computeShortestPaths(){
        CalculPlusCoursChemins calculPlusCoursChemins = new CalculPlusCoursChemins();
        //calculPlusCoursChemins.compute(pickupDeliveryModel.plan, pickupDeliveryModel.demandeDelivery);
        calculPlusCoursChemins.computeAstar(pickupDeliveryModel.plan, pickupDeliveryModel.demandeDelivery);
    }
}
