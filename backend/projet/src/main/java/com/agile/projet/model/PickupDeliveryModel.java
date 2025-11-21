package com.agile.projet.model;

import com.agile.projet.utils.XmlPlanParser;

public class PickupDeliveryModel {
    public Plan plan;

    public void createPlan(String planXml){
        plan = new Plan();
        XmlPlanParser xmlPlanParser = new XmlPlanParser();
        xmlPlanParser.parsePlan("petitPlan.xml",plan);
        plan.joinNoeudTroncons();
        plan.printTroncons();
    }



}
