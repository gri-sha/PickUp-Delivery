
package com.agile.projet.utils;

import javax.xml.parsers.DocumentBuilder;

import javax.xml.parsers.DocumentBuilderFactory;


import com.agile.projet.model.Noeud;
import com.agile.projet.model.Plan;
import com.agile.projet.model.Troncon;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class XmlPlanParser {
    public void parsePlan(String planXML, Plan plan) {
        try {
            /// open xml document
            InputStream is = getClass().getClassLoader().getResourceAsStream(planXML);
            if (is == null) {
                throw new FileNotFoundException("plan.xml not found");
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            doc.getDocumentElement().normalize();
            /// open xml document

            // ---------------------------
            //        PARSER NOEUDS
            // ---------------------------
            NodeList noeuds = doc.getElementsByTagName("noeud");

            for (int i = 0; i < noeuds.getLength(); i++) {
                Element n = (Element) noeuds.item(i);


                String id = n.getAttribute("id");
                String lat = n.getAttribute("latitude");
                String lon = n.getAttribute("longitude");

                Noeud noeud = new Noeud(Long.parseLong(id),Double.parseDouble(lat),Double.parseDouble(lon));
                plan.addNoeud(noeud);

            }

            // ---------------------------
            //      PARSER TRONCONS
            // ---------------------------
            NodeList troncons = doc.getElementsByTagName("troncon");

            for (int i = 0; i < troncons.getLength(); i++) {
                Element t = (Element) troncons.item(i);

                String origine = t.getAttribute("origine");
                String destination = t.getAttribute("destination");
                String longueur = t.getAttribute("longueur");
                String nomRue = t.getAttribute("nomRue");
                Troncon troncon = new Troncon(Long.parseLong(origine), Long.parseLong(destination), Double.parseDouble(longueur), nomRue);
                plan.addTroncon(troncon);



            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
}

