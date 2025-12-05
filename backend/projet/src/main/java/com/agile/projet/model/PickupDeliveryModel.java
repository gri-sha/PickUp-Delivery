package com.agile.projet.model;

import com.agile.projet.utils.XmlDeliveryParser;
import com.agile.projet.utils.XmlPlanParser;

public class PickupDeliveryModel {
    public Plan plan;
    public DemandeDelivery demandeDelivery= new DemandeDelivery();

    public void createPlan(String planXml){
        plan = new Plan();
        XmlPlanParser xmlPlanParser = new XmlPlanParser();
        xmlPlanParser.parsePlan(planXml,plan);
        plan.joinNoeudTroncons();

    }

    public void createDelivery(String deliveryXml) throws Exception {
        XmlDeliveryParser xmlDeliveryParser = new XmlDeliveryParser();
        xmlDeliveryParser.parse(deliveryXml,demandeDelivery);
    }



}
