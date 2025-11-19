
package com.agile.projet.utils;

import javax.xml.parsers.DocumentBuilder;

import javax.xml.parsers.DocumentBuilderFactory;


import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class XmlPlanParser {
    public void parsePlan(String planXML) {
        try {

            InputStream is = getClass().getClassLoader().getResourceAsStream(planXML);
            if (is == null) {
                throw new FileNotFoundException("plan.xml not found");
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);

            doc.getDocumentElement().normalize();

            // ---------------------------
            //        PARSER NOEUDS
            // ---------------------------
            NodeList noeuds = doc.getElementsByTagName("noeud");

            System.out.println("===== LISTE DES NOEUDS =====");

            for (int i = 0; i < noeuds.getLength(); i++) {
                Element n = (Element) noeuds.item(i);

                String id = n.getAttribute("id");
                String lat = n.getAttribute("latitude");
                String lon = n.getAttribute("longitude");

                System.out.println("Noeud trouvé :");
                System.out.println("  id = " + id);
                System.out.println("  latitude = " + lat);
                System.out.println("  longitude = " + lon);
                System.out.println("--------------------------------");
            }

            // ---------------------------
            //      PARSER TRONCONS
            // ---------------------------
            NodeList troncons = doc.getElementsByTagName("troncon");

            System.out.println("===== LISTE DES TRONCONS =====");

            for (int i = 0; i < troncons.getLength(); i++) {
                Element t = (Element) troncons.item(i);

                String origine = t.getAttribute("origine");
                String destination = t.getAttribute("destination");
                String longueur = t.getAttribute("longueur");
                String nomRue = t.getAttribute("nomRue");

                System.out.println("Troncon trouvé :");
                System.out.println("  origine = " + origine);
                System.out.println("  destination = " + destination);
                System.out.println("  longueur = " + longueur);
                System.out.println("  nomRue = " + nomRue);
                System.out.println("--------------------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

