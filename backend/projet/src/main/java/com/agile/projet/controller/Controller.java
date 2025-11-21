package com.agile.projet.controller;

import com.agile.projet.model.PickupDeliveryModel;
import org.springframework.stereotype.Component;

@Component
public class Controller {
    private PickupDeliveryModel pickupDeliveryModel;

    public Controller()
    {
    }

    public void createPlan(String planXml)
    {
        pickupDeliveryModel = new PickupDeliveryModel();
        pickupDeliveryModel.createPlan(planXml);
        pickupDeliveryModel.plan.printNoeuds();
    }
}
